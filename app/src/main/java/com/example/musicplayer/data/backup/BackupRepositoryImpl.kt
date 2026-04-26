package com.example.musicplayer.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.gson.Gson
import com.example.musicplayer.data.local.MusicDatabase
import com.example.musicplayer.data.local.entity.FavoriteEntity
import com.example.musicplayer.data.local.entity.PlayHistoryEntity
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongEntity
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.data.preferences.ThemeMode
import com.example.musicplayer.domain.model.BackupData
import com.example.musicplayer.domain.model.DriveBackupFile
import com.example.musicplayer.domain.model.PlayHistoryBackup
import com.example.musicplayer.domain.model.PlaylistBackup
import com.example.musicplayer.domain.model.PreferencesBackup
import com.example.musicplayer.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val db: MusicDatabase,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : BackupRepository {

    private val TAG = "BackupRepo"
    private val gson = Gson()
    private val driveScope = "https://www.googleapis.com/auth/drive.appdata"
    private val driveApiBase = "https://www.googleapis.com/drive/v3"
    private val driveUploadBase = "https://www.googleapis.com/upload/drive/v3"

    private fun buildSignInOptions() =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(driveScope))
            .build()

    override fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) { Log.d(TAG, "isSignedIn: no account"); return false }
        val hasPerm = GoogleSignIn.hasPermissions(account, Scope(driveScope))
        Log.d(TAG, "isSignedIn: account=${account.email} hasDrivePerm=$hasPerm")
        return hasPerm
    }

    override fun getSignedInEmail(): String? =
        GoogleSignIn.getLastSignedInAccount(context)?.email

    override fun getSignInIntent(): Intent =
        GoogleSignIn.getClient(context, buildSignInOptions()).signInIntent

    override suspend fun handleSignInResult(data: Intent?): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "handleSignInResult: success email=${account?.email}")
            account != null
        } catch (e: ApiException) {
            Log.e(TAG, "handleSignInResult: ApiException statusCode=${e.statusCode} msg=${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "handleSignInResult: unexpected error", e)
            false
        }
    }

    override suspend fun signOut() {
        val client = GoogleSignIn.getClient(context, buildSignInOptions())
        suspendCancellableCoroutine<Unit> { cont ->
            client.signOut().addOnCompleteListener { cont.resume(Unit) }
        }
    }

    private suspend fun getAuthToken(): String = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("Not signed in to Google")
        Log.d(TAG, "getAuthToken: fetching token for ${account.email}")
        val token = GoogleAuthUtil.getToken(context, account.account!!, "oauth2:$driveScope")
        Log.d(TAG, "getAuthToken: got token (length=${token.length})")
        token
    }

    private suspend fun buildBackupData(): BackupData {
        val playlists = db.playlistDao().getAllOnce().map { entity ->
            val songRefs = db.playlistDao().getSongsForPlaylistOnce(entity.id)
            PlaylistBackup(
                name = entity.name,
                createdAt = entity.createdAt,
                modifiedAt = entity.modifiedAt,
                songIds = songRefs.map { it.songId }
            )
        }
        val favorites = db.favoriteDao().getAllOnce().map { it.songId }
        val history = db.playHistoryDao().getAllOnce().map { h ->
            PlayHistoryBackup(h.songId, h.playCount, h.lastPlayedAt)
        }
        val prefs = PreferencesBackup(
            themeMode = appPreferences.themeMode.first().name,
            pauseOnDetach = appPreferences.pauseOnDetach.first(),
            colorThemeId = appPreferences.colorThemeId.first(),
            skinId = appPreferences.skinId.first(),
            minDurationSec = appPreferences.minDurationSec.first(),
            minSizeKb = appPreferences.minSizeKb.first()
        )
        return BackupData(playlists = playlists, favorites = favorites, playHistory = history, preferences = prefs)
    }

    override suspend fun uploadBackup(): Result<DriveBackupFile> = runCatching {
        Log.d(TAG, "uploadBackup: starting")
        val backup = buildBackupData()
        val json = gson.toJson(backup)
        val token = getAuthToken()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "musicplayer_backup_$timestamp.json"
        val boundary = UUID.randomUUID().toString().replace("-", "")

        val metadataJson = """{"name":"$fileName","parents":["appDataFolder"]}"""
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(json)
            append("\r\n--$boundary--")
        }

        val responseJson = withContext(Dispatchers.IO) {
            val url = URL("$driveUploadBase/files?uploadType=multipart&fields=id,name,createdTime,size")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            Log.d(TAG, "uploadBackup: HTTP $code")
            if (code != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "unknown"
                Log.e(TAG, "uploadBackup: error body=$err")
                throw IOException("Upload failed ($code): $err")
            }
            conn.inputStream.bufferedReader().readText()
        }

        val obj = JSONObject(responseJson)
        DriveBackupFile(
            id = obj.getString("id"),
            name = obj.getString("name"),
            createdTime = System.currentTimeMillis(),
            sizeBytes = json.length.toLong()
        )
    }

    override suspend fun listBackups(): Result<List<DriveBackupFile>> = runCatching {
        val token = getAuthToken()
        val responseJson = withContext(Dispatchers.IO) {
            val url = URL(
                "$driveApiBase/files" +
                    "?spaces=appDataFolder" +
                    "&fields=files(id,name,createdTime,size)" +
                    "&orderBy=createdTime+desc"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "unknown"
                throw IOException("List failed (${conn.responseCode}): $err")
            }
            conn.inputStream.bufferedReader().readText()
        }

        val filesArray = JSONObject(responseJson).getJSONArray("files")
        (0 until filesArray.length()).map { i ->
            val file = filesArray.getJSONObject(i)
            DriveBackupFile(
                id = file.getString("id"),
                name = file.getString("name"),
                createdTime = parseIso8601(file.optString("createdTime")),
                sizeBytes = file.optLong("size", 0L)
            )
        }
    }

    override suspend fun downloadAndRestore(fileId: String): Result<Unit> = runCatching {
        val token = getAuthToken()
        val json = withContext(Dispatchers.IO) {
            val url = URL("$driveApiBase/files/$fileId?alt=media")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "unknown"
                throw IOException("Download failed (${conn.responseCode}): $err")
            }
            conn.inputStream.bufferedReader().readText()
        }
        val backup = gson.fromJson(json, BackupData::class.java)
        applyBackup(backup)
    }

    override suspend fun deleteBackup(fileId: String): Result<Unit> = runCatching {
        val token = getAuthToken()
        withContext(Dispatchers.IO) {
            val url = URL("$driveApiBase/files/$fileId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "unknown"
                throw IOException("Delete failed (${conn.responseCode}): $err")
            }
        }
    }

    private suspend fun applyBackup(backup: BackupData) {
        // Clear existing data — delete songs first to avoid FK issues on older SQLite
        db.playlistDao().deleteAllSongs()
        db.playlistDao().deleteAll()
        db.favoriteDao().deleteAll()
        db.playHistoryDao().deleteAll()

        backup.playlists.forEach { pb ->
            val newId = db.playlistDao().insert(
                PlaylistEntity(name = pb.name, createdAt = pb.createdAt, modifiedAt = pb.modifiedAt)
            )
            pb.songIds.forEachIndexed { idx, songId ->
                db.playlistDao().insertSong(
                    PlaylistSongEntity(playlistId = newId, songId = songId, position = idx)
                )
            }
        }

        backup.favorites.forEach { songId ->
            db.favoriteDao().insert(FavoriteEntity(songId = songId))
        }

        backup.playHistory.forEach { h ->
            db.playHistoryDao().upsert(
                PlayHistoryEntity(songId = h.songId, playCount = h.playCount, lastPlayedAt = h.lastPlayedAt)
            )
        }

        val prefs = backup.preferences ?: PreferencesBackup()
        runCatching { appPreferences.setThemeMode(ThemeMode.valueOf(prefs.themeMode)) }
        appPreferences.setPauseOnDetach(prefs.pauseOnDetach)
        appPreferences.setColorThemeId(prefs.colorThemeId)
        appPreferences.setSkinId(prefs.skinId)
        appPreferences.setMinDurationSec(prefs.minDurationSec)
        appPreferences.setMinSizeKb(prefs.minSizeKb)
    }

    private fun parseIso8601(value: String): Long {
        if (value.isBlank()) return 0L
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                .parse(value)?.time ?: 0L
        }.getOrDefault(0L)
    }
}
