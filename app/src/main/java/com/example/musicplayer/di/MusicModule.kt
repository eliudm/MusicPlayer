package com.example.musicplayer.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import com.example.musicplayer.data.local.MusicDatabase
import com.example.musicplayer.data.mediastore.MediaStoreSource
import com.example.musicplayer.data.backup.BackupRepositoryImpl
import com.example.musicplayer.data.lyrics.LyricsRepositoryImpl
import com.example.musicplayer.data.repository.MusicRepositoryImpl
import com.example.musicplayer.domain.repository.BackupRepository
import com.example.musicplayer.domain.repository.LyricsRepository
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.data.preferences.AppPreferences
import com.example.musicplayer.service.EqualizerManager
import com.example.musicplayer.service.PlayerController
import com.example.musicplayer.widget.WidgetUpdater
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext ctx: Context): MusicDatabase =
        Room.databaseBuilder(ctx, MusicDatabase::class.java, "music_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideMediaStoreSource(@ApplicationContext ctx: Context): MediaStoreSource =
        MediaStoreSource(ctx)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideMusicRepository(
        db: MusicDatabase,
        ms: MediaStoreSource,
        playerController: PlayerController,
        @ApplicationContext ctx: Context,
        @ApplicationScope appScope: CoroutineScope,
        equalizerManager: EqualizerManager,
        appPreferences: AppPreferences,
        widgetUpdater: WidgetUpdater
    ): MusicRepository = MusicRepositoryImpl(db, ms, playerController, ctx, appScope, equalizerManager, appPreferences)
        .also { widgetUpdater.init(it) }

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext ctx: Context): ExoPlayer =
        ExoPlayer.Builder(ctx).build()

    @Provides
    @Singleton
    fun provideBackupRepository(impl: BackupRepositoryImpl): BackupRepository = impl

    @Provides
    @Singleton
    fun provideLyricsRepository(impl: LyricsRepositoryImpl): LyricsRepository = impl
}
