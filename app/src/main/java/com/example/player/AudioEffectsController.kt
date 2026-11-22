package com.example.player

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int
)

data class EqualizerState(
    val enabled: Boolean = false,
    val audioSessionId: Int = 0,
    val eqSupported: Boolean = false,
    val bassBoostSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
    val loudnessSupported: Boolean = false,
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val bands: List<EqBandInfo> = emptyList(),
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0, // 0..1000
    val loudnessGainMb: Int = 0, // 0..1500
    val selectedPresetName: String = "Flat",
    val deviceDriverName: String = "System Audio Pipeline"
)

data class BuiltInEqPreset(
    val name: String,
    val normalizedCurves: List<Float>, // -1f..1f interpolated across device bands
    val bassBoost: Int = 0,
    val virtualizer: Int = 0
)

class AudioEffectsController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    val builtInPresets: List<BuiltInEqPreset> = listOf(
        BuiltInEqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
        BuiltInEqPreset("Rock", listOf(0.45f, 0.25f, -0.15f, 0.3f, 0.5f), bassBoost = 150),
        BuiltInEqPreset("Pop", listOf(-0.15f, 0.25f, 0.45f, 0.25f, -0.15f)),
        BuiltInEqPreset("Jazz", listOf(0.3f, 0.1f, -0.2f, 0.2f, 0.35f)),
        BuiltInEqPreset("Classical", listOf(0.4f, 0.2f, -0.1f, 0.2f, 0.35f)),
        BuiltInEqPreset("Metal", listOf(0.5f, 0.1f, -0.2f, 0.25f, 0.55f), bassBoost = 250),
        BuiltInEqPreset("Electronic", listOf(0.55f, 0.25f, 0f, 0.25f, 0.5f), bassBoost = 320, virtualizer = 200),
        BuiltInEqPreset("Hip-Hop", listOf(0.6f, 0.35f, -0.1f, 0.15f, 0.3f), bassBoost = 400),
        BuiltInEqPreset("Vocal", listOf(-0.25f, 0.1f, 0.5f, 0.4f, 0.05f)),
        BuiltInEqPreset("Acoustic", listOf(0.3f, 0.2f, 0.15f, 0.25f, 0.25f)),
        BuiltInEqPreset("Bass Boost", listOf(0.7f, 0.45f, 0.05f, 0f, 0f), bassBoost = 550),
        BuiltInEqPreset("Treble Boost", listOf(0f, 0f, 0.1f, 0.45f, 0.7f))
    )

    fun attachToSession(
        audioSessionId: Int,
        enabled: Boolean,
        savedBandsCsv: String,
        bassStrength: Int,
        virtStrength: Int,
        loudnessMb: Int,
        presetName: String
    ) {
        if (audioSessionId <= 0) return
        if (_state.value.audioSessionId == audioSessionId && equalizer != null) {
            setEnabled(enabled)
            return
        }
        release()

        val descriptors = AudioEffect.queryEffects()
        val hasEqDescriptor = descriptors.any { it.type == AudioEffect.EFFECT_TYPE_EQUALIZER }
        val hasBassDescriptor = descriptors.any { it.type == AudioEffect.EFFECT_TYPE_BASS_BOOST }
        val hasVirtDescriptor = descriptors.any { it.type == AudioEffect.EFFECT_TYPE_VIRTUALIZER }

        var eqOk = false
        var bassOk = false
        var virtOk = false
        var loudOk = false
        var minMb = -1500
        var maxMb = 1500
        val bands = mutableListOf<EqBandInfo>()

        if (hasEqDescriptor) {
            runCatching {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = enabled
                equalizer = eq
                eqOk = true
                val range = eq.bandLevelRange
                minMb = range[0].toInt()
                maxMb = range[1].toInt()
                val numBands = eq.numberOfBands.toInt()
                val savedLevels = savedBandsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
                for (i in 0 until numBands) {
                    val level = savedLevels.getOrNull(i)?.coerceIn(minMb, maxMb) ?: eq.getBandLevel(i.toShort()).toInt()
                    if (savedLevels.getOrNull(i) != null) {
                        runCatching { eq.setBandLevel(i.toShort(), level.toShort()) }
                    }
                    bands.add(
                        EqBandInfo(
                            index = i,
                            centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                            levelMb = level
                        )
                    )
                }
            }
        }

        if (hasBassDescriptor) {
            runCatching {
                val bb = BassBoost(0, audioSessionId)
                if (bb.strengthSupported) {
                    bb.setStrength(bassStrength.coerceIn(0, 1000).toShort())
                    bb.enabled = enabled && bassStrength > 0
                    bassBoost = bb
                    bassOk = true
                } else {
                    bb.release()
                }
            }
        }

        if (hasVirtDescriptor) {
            runCatching {
                val v = Virtualizer(0, audioSessionId)
                if (v.strengthSupported) {
                    v.setStrength(virtStrength.coerceIn(0, 1000).toShort())
                    v.enabled = enabled && virtStrength > 0
                    virtualizer = v
                    virtOk = true
                } else {
                    v.release()
                }
            }
        }

        runCatching {
            val le = LoudnessEnhancer(audioSessionId)
            le.setTargetGain(loudnessMb.coerceIn(0, 1500))
            le.enabled = enabled && loudnessMb > 0
            loudnessEnhancer = le
            loudOk = true
        }

        _state.value = EqualizerState(
            enabled = enabled,
            audioSessionId = audioSessionId,
            eqSupported = eqOk,
            bassBoostSupported = bassOk,
            virtualizerSupported = virtOk,
            loudnessSupported = loudOk,
            minBandLevelMb = minMb,
            maxBandLevelMb = maxMb,
            bands = bands,
            bassBoostStrength = bassStrength,
            virtualizerStrength = virtStrength,
            loudnessGainMb = loudnessMb,
            selectedPresetName = presetName,
            deviceDriverName = descriptors.firstOrNull { it.type == AudioEffect.EFFECT_TYPE_EQUALIZER }?.name
                ?: "Android AudioEffect"
        )
    }

    fun setEnabled(enabled: Boolean) {
        val cur = _state.value
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled && cur.bassBoostStrength > 0 }
        runCatching { virtualizer?.enabled = enabled && cur.virtualizerStrength > 0 }
        runCatching { loudnessEnhancer?.enabled = enabled && cur.loudnessGainMb > 0 }
        _state.value = cur.copy(enabled = enabled)
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val cur = _state.value
        val clamped = levelMb.coerceIn(cur.minBandLevelMb, cur.maxBandLevelMb)
        runCatching { equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort()) }
        val updatedBands = cur.bands.map {
            if (it.index == bandIndex) it.copy(levelMb = clamped) else it
        }
        _state.value = cur.copy(bands = updatedBands, selectedPresetName = "Custom")
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.setStrength(clamped.toShort())
            bassBoost?.enabled = _state.value.enabled && clamped > 0
        }
        _state.value = _state.value.copy(bassBoostStrength = clamped)
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.setStrength(clamped.toShort())
            virtualizer?.enabled = _state.value.enabled && clamped > 0
        }
        _state.value = _state.value.copy(virtualizerStrength = clamped)
    }

    fun setLoudnessGain(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1500)
        runCatching {
            loudnessEnhancer?.setTargetGain(clamped)
            loudnessEnhancer?.enabled = _state.value.enabled && clamped > 0
        }
        _state.value = _state.value.copy(loudnessGainMb = clamped)
    }

    fun applyBuiltInPreset(preset: BuiltInEqPreset) {
        val cur = _state.value
        val bands = cur.bands
        if (bands.isEmpty()) return
        val updated = bands.mapIndexed { idx, band ->
            val curvePos = if (bands.size <= 1) 0f else (idx.toFloat() / (bands.size - 1)) * (preset.normalizedCurves.size - 1)
            val lowIdx = curvePos.toInt().coerceIn(0, preset.normalizedCurves.lastIndex)
            val highIdx = (lowIdx + 1).coerceIn(0, preset.normalizedCurves.lastIndex)
            val frac = curvePos - lowIdx
            val norm = preset.normalizedCurves[lowIdx] * (1f - frac) + preset.normalizedCurves[highIdx] * frac
            val targetMb = (norm * cur.maxBandLevelMb).toInt().coerceIn(cur.minBandLevelMb, cur.maxBandLevelMb)
            runCatching { equalizer?.setBandLevel(idx.toShort(), targetMb.toShort()) }
            band.copy(levelMb = targetMb)
        }
        setBassBoost(preset.bassBoost)
        setVirtualizer(preset.virtualizer)
        _state.value = _state.value.copy(bands = updated, selectedPresetName = preset.name)
    }

    fun applyCustomLevels(name: String, levels: List<Int>, bass: Int, virt: Int, loudness: Int) {
        val cur = _state.value
        val updated = cur.bands.mapIndexed { idx, band ->
            val target = (levels.getOrNull(idx) ?: 0).coerceIn(cur.minBandLevelMb, cur.maxBandLevelMb)
            runCatching { equalizer?.setBandLevel(idx.toShort(), target.toShort()) }
            band.copy(levelMb = target)
        }
        setBassBoost(bass)
        setVirtualizer(virt)
        setLoudnessGain(loudness)
        _state.value = _state.value.copy(bands = updated, selectedPresetName = name)
    }

    fun bandsCsv(): String = _state.value.bands.joinToString(",") { it.levelMb.toString() }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
