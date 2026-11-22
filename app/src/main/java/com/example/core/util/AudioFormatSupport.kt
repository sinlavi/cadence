package com.example.core.util

import android.media.MediaCodecList
import android.media.MediaFormat

data class FormatSupportInfo(
    val format: String,
    val mimeType: String,
    val isSupportedByDevice: Boolean,
    val note: String
)

object AudioFormatSupport {
    private val formatMimeMap = listOf(
        Triple("MP3", MediaFormat.MIMETYPE_AUDIO_MPEG, "MPEG-1/2 Audio Layer III"),
        Triple("FLAC", MediaFormat.MIMETYPE_AUDIO_FLAC, "Free Lossless Audio Codec"),
        Triple("M4A / AAC", MediaFormat.MIMETYPE_AUDIO_AAC, "AAC-LC / HE-AAC in MP4 container"),
        Triple("OGG", MediaFormat.MIMETYPE_AUDIO_VORBIS, "Ogg Vorbis"),
        Triple("OPUS", MediaFormat.MIMETYPE_AUDIO_OPUS, "Opus Interactive Audio"),
        Triple("WAV", MediaFormat.MIMETYPE_AUDIO_RAW, "Uncompressed PCM WAV"),
        Triple("ALAC", "audio/alac", "Apple Lossless (device/ExoPlayer decoder)"),
        Triple("AIFF", "audio/x-aiff", "Audio Interchange File Format"),
        Triple("WMA", "audio/x-ms-wma", "Windows Media Audio (OEM codec dependent)"),
        Triple("APE", "audio/x-ape", "Monkey's Audio (OEM/extension dependent)")
    )

    fun probeDeviceAudioCodecs(): List<FormatSupportInfo> {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val supportedMimes = mutableSetOf<String>()
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) {
                supportedMimes.addAll(info.supportedTypes.map { it.lowercase() })
            }
        }
        return formatMimeMap.map { (fmt, mime, note) ->
            val supported = supportedMimes.contains(mime.lowercase()) ||
                (fmt == "WAV" && supportedMimes.contains("audio/raw"))
            FormatSupportInfo(
                format = fmt,
                mimeType = mime,
                isSupportedByDevice = supported,
                note = if (supported) note else "$note — No hardware/platform decoder reported on this device"
            )
        }
    }

    fun extensionToFormat(ext: String): String {
        return when (ext.lowercase().trimStart('.')) {
            "mp3" -> "MP3"
            "flac" -> "FLAC"
            "wav", "wave" -> "WAV"
            "m4a" -> "M4A"
            "aac" -> "AAC"
            "ogg", "oga" -> "OGG"
            "opus" -> "OPUS"
            "wma" -> "WMA"
            "ape" -> "APE"
            "aiff", "aif" -> "AIFF"
            "alac" -> "ALAC"
            else -> ext.uppercase().ifBlank { "AUDIO" }
        }
    }
}
