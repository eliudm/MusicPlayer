package com.example.musicplayer.domain.model

enum class AudioFormat(
    val label: String,
    val isHighFidelity: Boolean,
    val isPlayable: Boolean
) {
    FLAC("FLAC", true, true),
    WAV("WAV", true, true),
    ALAC("ALAC", true, true),
    AIFF("AIFF", true, true),
    OGG("OGG", false, true),
    MP3("MP3", false, true),
    AAC("AAC", false, true),
    M4A("M4A", false, true),
    OPUS("Opus", false, true),
    APE("APE", true, false),
    DSD("DSD", true, false),
    WMA("WMA", false, false),
    UNKNOWN("Audio", false, true);

    companion object {
        fun fromMimeType(mimeType: String): AudioFormat {
            val m = mimeType.lowercase()
            return when {
                "flac" in m -> FLAC
                "wav" in m || "wave" in m -> WAV
                "alac" in m || m == "audio/x-alac" -> ALAC
                "aiff" in m || m == "audio/aif" -> AIFF
                "ogg" in m || "vorbis" in m -> OGG
                "mpeg" in m || "mp3" in m -> MP3
                "opus" in m -> OPUS
                "mp4" in m || "m4a" in m || "aac" in m -> AAC
                "ape" in m || "monkeys" in m -> APE
                "dsf" in m || "dff" in m || "dsd" in m -> DSD
                "wma" in m || "ms-wma" in m -> WMA
                else -> UNKNOWN
            }
        }

        fun fromPath(path: String): AudioFormat {
            val ext = path.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "flac" -> FLAC
                "wav", "wave" -> WAV
                "alac", "m4a" -> ALAC
                "aif", "aiff" -> AIFF
                "ogg" -> OGG
                "mp3" -> MP3
                "opus" -> OPUS
                "aac", "mp4" -> AAC
                "ape" -> APE
                "dsf", "dff" -> DSD
                "wma" -> WMA
                else -> UNKNOWN
            }
        }
    }
}
