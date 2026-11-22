package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.core.model.CardAppearance
import com.example.core.model.LyricsStorageMode
import com.example.core.model.MiniPlayerStyle
import com.example.core.model.NavDestinationKey
import com.example.core.model.NowPlayingStyle
import com.example.core.model.RatingSystemMode
import com.example.core.model.ThemeBaseMode
import com.example.core.model.ThemeConfig
import com.example.core.model.UiDensity
import com.example.core.util.AudioFormatSupport
import com.example.core.util.TextNormalizer
import com.example.data.db.EqualizerPresetEntity
import com.example.data.preferences.AppSettingsSnapshot
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.theme.ArtworkPaletteColors
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.CuratedThemePresets
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.ThemeSwatchColors
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
private fun FullWidthSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.overline,
        color = MaterialTheme.colors.primary,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EqualizerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val eqState by viewModel.eqState.collectAsState()
    val customPresets by viewModel.customEqPresets.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    val eqCtrl = viewModel.container.audioEffectsController

    var showSavePresetDialog by remember { mutableStateOf(false) }

    fun persistEqToSettings() {
        val cur = eqCtrl.state.value
        scope.launch {
            viewModel.container.settingsRepository.updateSettings {
                it.copy(
                    equalizerEnabled = cur.enabled,
                    equalizerSelectedPreset = cur.selectedPresetName,
                    equalizerBandsCsv = eqCtrl.bandsCsv(),
                    bassBoostStrength = cur.bassBoostStrength,
                    virtualizerStrength = cur.virtualizerStrength,
                    loudnessGainMb = cur.loudnessGainMb
                )
            }
        }
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Equalizer Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            viewModel.container.database.customizationAndCacheDao().upsertEqPreset(
                                EqualizerPresetEntity(
                                    name = presetName.trim(),
                                    bandLevelsCsv = eqCtrl.bandsCsv(),
                                    bassBoostStrength = eqState.bassBoostStrength,
                                    virtualizerStrength = eqState.virtualizerStrength,
                                    loudnessGainMb = eqState.loudnessGainMb,
                                    isBuiltIn = false
                                )
                            )
                        }
                        viewModel.showMessage("Saved EQ preset \"${presetName.trim()}\".")
                        showSavePresetDialog = false
                    },
                    enabled = presetName.isNotBlank()
                ) { SingleLineButtonLabel("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { SingleLineButtonLabel("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("equalizer_screen")
    ) {
        TopAppBar(
            title = { Text("Equalizer & Audio DSP", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = { showSavePresetDialog = true }, enabled = eqState.bands.isNotEmpty()) {
                    Icon(Icons.Default.Save, contentDescription = "Save EQ Preset")
                }
                Switch(
                    checked = eqState.enabled,
                    onCheckedChange = {
                        eqCtrl.setEnabled(it)
                        persistEqToSettings()
                    },
                    modifier = Modifier.padding(end = CadenceSpacing.sm).testTag("eq_master_switch")
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Full-Width Hardware Audio Session Strip
            Surface(color = ext.elevatedSurface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = MaterialTheme.colors.primary)
                        Spacer(Modifier.width(CadenceSpacing.sm))
                        Text(eqState.deviceDriverName, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(CadenceSpacing.xs))
                    Text(
                        text = if (eqState.audioSessionId > 0) {
                            "Session #${eqState.audioSessionId} • EQ: ${if (eqState.eqSupported) "Active" else "N/A"} • Bass: ${if (eqState.bassBoostSupported) "OK" else "N/A"} • Virt: ${if (eqState.virtualizerSupported) "OK" else "N/A"}"
                        } else {
                            "Start playing a track to bind the live Android AudioEffect session."
                        },
                        style = MaterialTheme.typography.caption.tabular(),
                        color = ext.subtleText
                    )
                }
            }
            Divider(color = ext.dividerColor)

            Column(modifier = Modifier.padding(vertical = CadenceSpacing.md)) {
                FullWidthSectionHeader("Built-In Presets", Modifier.padding(horizontal = hPad))
                Spacer(Modifier.height(CadenceSpacing.xs))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = hPad),
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                ) {
                    items(eqCtrl.builtInPresets, key = { it.name }) { preset ->
                        FilterChip(
                            selected = eqState.selectedPresetName == preset.name,
                            onClick = {
                                eqCtrl.applyBuiltInPreset(preset)
                                persistEqToSettings()
                            }
                        ) { SingleLineButtonLabel(preset.name, style = MaterialTheme.typography.caption) }
                    }
                }

                if (customPresets.isNotEmpty()) {
                    Spacer(Modifier.height(CadenceSpacing.md))
                    FullWidthSectionHeader("Saved Custom Presets", Modifier.padding(horizontal = hPad))
                    Spacer(Modifier.height(CadenceSpacing.xs))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = hPad),
                        horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                    ) {
                        items(customPresets, key = { "cp_${it.id}" }) { preset ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = eqState.selectedPresetName == preset.name,
                                    onClick = {
                                        val levels = preset.bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
                                        eqCtrl.applyCustomLevels(
                                            preset.name,
                                            levels,
                                            preset.bassBoostStrength,
                                            preset.virtualizerStrength,
                                            preset.loudnessGainMb
                                        )
                                        persistEqToSettings()
                                    }
                                ) { SingleLineButtonLabel(preset.name, style = MaterialTheme.typography.caption) }
                                IconButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            viewModel.container.database.customizationAndCacheDao().deleteCustomEqPreset(preset.id)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Preset", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            Divider(color = ext.dividerColor)

            // Full-Width Frequency Bands Section
            Column(modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md)) {
                FullWidthSectionHeader("Frequency Bands • ${eqState.selectedPresetName}")
                Spacer(Modifier.height(CadenceSpacing.sm))
                if (eqState.bands.isEmpty()) {
                    Text(
                        text = "No equalizer bands attached yet. Play any local track and toggle EQ on to initialize hardware bands.",
                        style = MaterialTheme.typography.body2,
                        color = ext.subtleText
                    )
                } else {
                    eqState.bands.forEach { band ->
                        val freqText = if (band.centerFreqHz >= 1000) {
                            "${String.format("%.1f", band.centerFreqHz / 1000f)}k"
                        } else {
                            "${band.centerFreqHz}Hz"
                        }
                        val dbText = String.format("%+.1fdB", band.levelMb / 100f)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(freqText, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.caption.tabular(), maxLines = 1, modifier = Modifier.width(52.dp))
                            Slider(
                                value = band.levelMb.toFloat(),
                                onValueChange = { eqCtrl.setBandLevel(band.index, it.toInt()) },
                                onValueChangeFinished = { persistEqToSettings() },
                                valueRange = eqState.minBandLevelMb.toFloat()..eqState.maxBandLevelMb.toFloat(),
                                enabled = eqState.enabled && eqState.eqSupported,
                                modifier = Modifier.weight(1f)
                            )
                            Text(dbText, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.caption.tabular(), maxLines = 1, modifier = Modifier.width(56.dp))
                        }
                    }
                }
            }
            Divider(color = ext.dividerColor)

            // Full-Width Sound Enhancements Section
            Column(
                modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
            ) {
                FullWidthSectionHeader("Sound Enhancements")

                Text("Bass Boost: ${eqState.bassBoostStrength / 10}%", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = eqState.bassBoostStrength.toFloat(),
                    onValueChange = { eqCtrl.setBassBoost(it.toInt()) },
                    onValueChangeFinished = { persistEqToSettings() },
                    valueRange = 0f..1000f,
                    enabled = eqState.enabled && eqState.bassBoostSupported
                )

                Text("Virtualizer: ${eqState.virtualizerStrength / 10}%", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = eqState.virtualizerStrength.toFloat(),
                    onValueChange = { eqCtrl.setVirtualizer(it.toInt()) },
                    onValueChangeFinished = { persistEqToSettings() },
                    valueRange = 0f..1000f,
                    enabled = eqState.enabled && eqState.virtualizerSupported
                )

                Text("Loudness Enhancer: +${String.format("%.1f", eqState.loudnessGainMb / 100f)} dB", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = eqState.loudnessGainMb.toFloat(),
                    onValueChange = { eqCtrl.setLoudnessGain(it.toInt()) },
                    onValueChangeFinished = { persistEqToSettings() },
                    valueRange = 0f..1500f,
                    enabled = eqState.enabled && eqState.loudnessSupported
                )

                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                SettingSwitchRow(
                    title = "ReplayGain Volume Normalization",
                    subtitle = "Uses track/album ReplayGain tags when present",
                    checked = settings.replayGainEnabled
                ) { enabled ->
                    scope.launch {
                        viewModel.container.settingsRepository.updateSettings { it.copy(replayGainEnabled = enabled) }
                        viewModel.container.playerConnection.notifyServiceSettingsChanged()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeGeneratorScreen(
    viewModel: MainViewModel,
    artworkPalette: ArtworkPaletteColors?,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val customThemes by viewModel.customThemes.collectAsState()
    val cfg = settings.themeConfig
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    val json = remember { Json { ignoreUnknownKeys = true; encodeDefaults = true } }

    var editingTarget by remember { mutableIntStateOf(0) } // 0=Primary, 1=Secondary, 2=Background, 3=Surface
    var showSaveDialog by remember { mutableStateOf(false) }

    val activeHex = when (editingTarget) {
        0 -> cfg.primaryHex
        1 -> cfg.secondaryHex
        2 -> cfg.customBackgroundHex ?: MaterialTheme.colors.background.toArgb().toLong() and 0xFFFFFFFFL
        else -> cfg.customSurfaceHex ?: MaterialTheme.colors.surface.toArgb().toLong() and 0xFFFFFFFFL
    }

    val hsv = remember(activeHex, editingTarget) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(activeHex.toInt(), arr)
        arr
    }
    var hue by remember(activeHex, editingTarget) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(activeHex, editingTarget) { mutableFloatStateOf(hsv[1]) }
    var valBright by remember(activeHex, editingTarget) { mutableFloatStateOf(hsv[2]) }

    fun applyPickedHex(argbLong: Long) {
        viewModel.updateThemeConfig { c ->
            when (editingTarget) {
                0 -> c.copy(presetId = "custom", primaryHex = argbLong)
                1 -> c.copy(presetId = "custom", secondaryHex = argbLong)
                2 -> c.copy(presetId = "custom", customBackgroundHex = argbLong)
                else -> c.copy(presetId = "custom", customSurfaceHex = argbLong)
            }
        }
    }

    if (showSaveDialog) {
        var themeName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom Theme") },
            text = {
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("Theme Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomTheme(themeName, cfg)
                        showSaveDialog = false
                    },
                    enabled = themeName.isNotBlank()
                ) { SingleLineButtonLabel("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { SingleLineButtonLabel("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("theme_generator_screen")
    ) {
        TopAppBar(
            title = { Text("Theme Studio", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Default.Save, contentDescription = "Save Theme Preset")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Full-Width Live Preview Strip
            Surface(color = ext.elevatedSurface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(CadenceRadius.sm))
                            .background(MaterialTheme.colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colors.onPrimary)
                    }
                    Spacer(Modifier.width(CadenceSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Studio Theme", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${cfg.density.label} • Corners ${cfg.artworkCornerRadiusDp}dp • ${cfg.cardAppearance.label}",
                            style = MaterialTheme.typography.caption.tabular(),
                            color = ext.subtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(color = MaterialTheme.colors.secondary, shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colors.onSecondary, modifier = Modifier.padding(CadenceSpacing.sm))
                    }
                }
            }
            Divider(color = ext.dividerColor)

            // Base Mode & Curated Presets
            Column(modifier = Modifier.padding(vertical = CadenceSpacing.md)) {
                FullWidthSectionHeader("Base Appearance Mode", Modifier.padding(horizontal = hPad))
                Spacer(Modifier.height(CadenceSpacing.xs))
                FlowRow(
                    modifier = Modifier.padding(horizontal = hPad),
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                ) {
                    ThemeBaseMode.entries.forEach { mode ->
                        FilterChip(
                            selected = cfg.baseMode == mode,
                            onClick = {
                                viewModel.updateThemeConfig {
                                    it.copy(baseMode = mode, customBackgroundHex = null, customSurfaceHex = null)
                                }
                            }
                        ) {
                            SingleLineButtonLabel(
                                when (mode) {
                                    ThemeBaseMode.SYSTEM -> "System"
                                    ThemeBaseMode.LIGHT -> "Light"
                                    ThemeBaseMode.DARK -> "Dark"
                                    ThemeBaseMode.AMOLED_BLACK -> "AMOLED"
                                },
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }

                Spacer(Modifier.height(CadenceSpacing.md))
                FullWidthSectionHeader("Curated Studio Presets", Modifier.padding(horizontal = hPad))
                Spacer(Modifier.height(CadenceSpacing.xs))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = hPad),
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                ) {
                    items(CuratedThemePresets, key = { it.id }) { preset ->
                        val selected = cfg.presetId == preset.id
                        Surface(
                            shape = RoundedCornerShape(CadenceRadius.sm),
                            color = ext.elevatedSurface,
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colors.primary else ext.subtleBorder),
                            modifier = Modifier.width(142.dp).clickable {
                                viewModel.updateThemeConfig {
                                    it.copy(
                                        presetId = preset.id,
                                        baseMode = preset.baseMode,
                                        primaryHex = preset.primaryHex,
                                        secondaryHex = preset.secondaryHex,
                                        customBackgroundHex = preset.backgroundHex,
                                        customSurfaceHex = preset.surfaceHex
                                    )
                                }
                            }
                        ) {
                            Column(modifier = Modifier.padding(CadenceSpacing.md)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color(preset.primaryHex)))
                                    Box(Modifier.size(18.dp).clip(CircleShape).background(Color(preset.secondaryHex)))
                                }
                                Spacer(Modifier.height(CadenceSpacing.xs))
                                Text(preset.name, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(preset.subtitle, style = MaterialTheme.typography.caption, color = ext.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Divider(color = ext.dividerColor)

            // Artwork-Adaptive Mode (Full-Width Section)
            Column(
                modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
            ) {
                FullWidthSectionHeader("Artwork-Adaptive Mode")
                SettingSwitchRow(
                    title = "Extract Palette from Album Art",
                    subtitle = "Adapts Now Playing and Lyrics reader accents",
                    checked = cfg.useArtworkColors
                ) { checked -> viewModel.updateThemeConfig { it.copy(useArtworkColors = checked) } }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Apply Artwork Colors App-Wide", style = MaterialTheme.typography.body2)
                        Text("Overrides accent across all tabs while playing", style = MaterialTheme.typography.caption, color = ext.subtleText)
                    }
                    Switch(
                        checked = cfg.artworkThemeAppWide,
                        enabled = cfg.useArtworkColors,
                        onCheckedChange = { checked -> viewModel.updateThemeConfig { it.copy(artworkThemeAppWide = checked) } }
                    )
                }
                if (artworkPalette != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Text("Active swatches:", style = MaterialTheme.typography.caption, color = ext.subtleText)
                        Box(Modifier.size(18.dp).clip(CircleShape).background(artworkPalette.vibrant))
                        Box(Modifier.size(18.dp).clip(CircleShape).background(artworkPalette.dominant))
                        Box(Modifier.size(18.dp).clip(CircleShape).background(artworkPalette.muted))
                    }
                }
            }
            Divider(color = ext.dividerColor)

            // Custom Color Generator (Full-Width Section)
            Column(
                modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FullWidthSectionHeader("Custom Color Generator", Modifier.weight(1f))
                    if (cfg.customBackgroundHex != null || cfg.customSurfaceHex != null) {
                        TextButton(
                            onClick = { viewModel.updateThemeConfig { it.copy(customBackgroundHex = null, customSurfaceHex = null) } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { SingleLineButtonLabel("RESET BG") }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                    listOf("Primary", "Secondary", "Background", "Surface").forEachIndexed { idx, label ->
                        FilterChip(selected = editingTarget == idx, onClick = { editingTarget = idx }) {
                            SingleLineButtonLabel(label, style = MaterialTheme.typography.caption)
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                    items(ThemeSwatchColors) { swatch ->
                        val swatchHex = swatch.toArgb().toLong() and 0xFFFFFFFFL
                        val isSelected = swatchHex == activeHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colors.onSurface, CircleShape)
                                .clickable { applyPickedHex(swatchHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Text("Hue (${hue.toInt()}°)", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        val cInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, valBright))
                        applyPickedHex(cInt.toLong() and 0xFFFFFFFFL)
                    },
                    valueRange = 0f..360f
                )
                Text("Saturation (${(sat * 100).toInt()}%)", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = sat,
                    onValueChange = {
                        sat = it
                        val cInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, valBright))
                        applyPickedHex(cInt.toLong() and 0xFFFFFFFFL)
                    },
                    valueRange = 0f..1f
                )
                Text("Brightness (${(valBright * 100).toInt()}%)", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = valBright,
                    onValueChange = {
                        valBright = it
                        val cInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, valBright))
                        applyPickedHex(cInt.toLong() and 0xFFFFFFFFL)
                    },
                    valueRange = 0.04f..1f
                )
            }
            Divider(color = ext.dividerColor)

            // Layout, Density & Styles (Full-Width Section)
            Column(
                modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
            ) {
                FullWidthSectionHeader("Layout, Density & Styles")

                Text("Artwork Corner Radius: ${cfg.artworkCornerRadiusDp} dp", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = cfg.artworkCornerRadiusDp.toFloat(),
                    onValueChange = { v -> viewModel.updateThemeConfig { it.copy(artworkCornerRadiusDp = v.toInt()) } },
                    valueRange = 0f..24f
                )

                Text("Font Scale: ${String.format("%.2fx", cfg.fontScale)}", style = MaterialTheme.typography.caption.tabular())
                Slider(
                    value = cfg.fontScale,
                    onValueChange = { v -> viewModel.updateThemeConfig { it.copy(fontScale = v) } },
                    valueRange = 0.85f..1.25f
                )

                Text("Container Style", style = MaterialTheme.typography.caption, color = ext.subtleText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                    CardAppearance.entries.forEach { ca ->
                        FilterChip(
                            selected = cfg.cardAppearance == ca,
                            onClick = { viewModel.updateThemeConfig { it.copy(cardAppearance = ca) } }
                        ) { SingleLineButtonLabel(ca.label, style = MaterialTheme.typography.caption) }
                    }
                }

                Text("List Density", style = MaterialTheme.typography.caption, color = ext.subtleText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                    UiDensity.entries.forEach { d ->
                        FilterChip(
                            selected = cfg.density == d,
                            onClick = { viewModel.updateThemeConfig { it.copy(density = d) } }
                        ) { SingleLineButtonLabel(d.label, style = MaterialTheme.typography.caption) }
                    }
                }

                Text("Now Playing Layout", style = MaterialTheme.typography.caption, color = ext.subtleText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                    NowPlayingStyle.entries.forEach { s ->
                        FilterChip(
                            selected = cfg.nowPlayingStyle == s,
                            onClick = { viewModel.updateThemeConfig { it.copy(nowPlayingStyle = s) } }
                        ) { SingleLineButtonLabel(s.label, style = MaterialTheme.typography.caption) }
                    }
                }

                Text("Mini Player Style", style = MaterialTheme.typography.caption, color = ext.subtleText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                    MiniPlayerStyle.entries.forEach { m ->
                        FilterChip(
                            selected = cfg.miniPlayerStyle == m,
                            onClick = { viewModel.updateThemeConfig { it.copy(miniPlayerStyle = m) } }
                        ) { SingleLineButtonLabel(m.label, style = MaterialTheme.typography.caption) }
                    }
                }

                SettingSwitchRow(
                    title = "Reduced Motion Mode",
                    subtitle = "Minimizes heavy animations on older hardware",
                    checked = cfg.reducedMotion
                ) { r -> viewModel.updateThemeConfig { it.copy(reducedMotion = r) } }
            }

            if (customThemes.isNotEmpty()) {
                Divider(color = ext.dividerColor)
                FullWidthSectionHeader("Saved Custom Themes", Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.sm))
                customThemes.forEach { saved ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching { json.decodeFromString<ThemeConfig>(saved.configJson) }.getOrNull()?.let { parsed ->
                                    viewModel.updateThemeConfig { parsed }
                                    viewModel.showMessage("Applied \"${saved.name}\".")
                                }
                            }
                            .padding(horizontal = hPad, vertical = CadenceSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(saved.name, style = MaterialTheme.typography.subtitle1, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteCustomTheme(saved.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Custom Theme", tint = MaterialTheme.colors.error)
                        }
                    }
                    Divider(color = ext.dividerColor, startIndent = hPad)
                }
            }
            Spacer(Modifier.height(CadenceSpacing.xl))
        }
    }
}

private enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    APPEARANCE("Appearance & Layout", "Theme Studio, scoring mode, nav tabs & mini player icons", Icons.Default.Palette),
    PLAYBACK("Playback & Notifications", "Audio engine, seek step & notification player icons", Icons.Default.PlayArrow),
    AUDIO_EQ("Equalizer & Audio DSP", "Hardware Equalizer, Bass Boost & ReplayGain", Icons.Default.Equalizer),
    LIBRARY("Library & Scanner", "SAF folders, exclusions, formats & min duration", Icons.Default.FolderOpen),
    LYRICS_PROVIDERS("Lyrics & Online Providers", "LRCLIB, iTunes US, provider priority & API keys", Icons.Default.Key),
    BACKUP_STORAGE("Backup, Restore & Export", "Full ZIP backup, restore & library CSV export", Icons.Default.Backup),
    PRIVACY_DIAGNOSTICS("Stats, Cache & Diagnostics", "Listening history, audio decoder & error logs", Icons.Default.TableChart),
    ABOUT("About & Reset", "App logo, version, architecture & factory reset", Icons.Default.Info)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenThemeGenerator: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val scanFolders by viewModel.scanFolders.collectAsState()
    val stats by viewModel.libraryStats.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val playbackErrors by viewModel.playbackErrors.collectAsState()
    val scope = rememberCoroutineScope()
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    val codecSupport = remember { AudioFormatSupport.probeDeviceAudioCodecs() }
    var showResetConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = activeCategory != null) {
        activeCategory = null
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }.onFailure {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            scope.launch {
                viewModel.container.libraryScanner.addSafFolder(uri, isExcluded = false)
                viewModel.triggerLibraryScan()
            }
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.container.backupManager.exportZipBackup(uri)
                    .onSuccess { viewModel.showMessage(it) }
                    .onFailure { viewModel.showMessage("Backup failed: ${it.localizedMessage}") }
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.container.backupManager.importZipBackup(uri)
                    .onSuccess { viewModel.showMessage(it) }
                    .onFailure { viewModel.showMessage("Restore failed: ${it.localizedMessage}") }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.container.backupManager.exportLibraryCsv(uri)
                    .onSuccess { count -> viewModel.showMessage("Exported $count tracks to CSV.") }
                    .onFailure { viewModel.showMessage("CSV export failed: ${it.localizedMessage}") }
            }
        }
    }

    fun update(block: (AppSettingsSnapshot) -> AppSettingsSnapshot) {
        scope.launch {
            viewModel.container.settingsRepository.updateSettings(block)
            viewModel.container.playerConnection.notifyServiceSettingsChanged()
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Settings?") },
            text = { Text("Restores theme, playback, scanner, and provider preferences to defaults.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { viewModel.container.settingsRepository.resetAllSettings() }
                    showResetConfirm = false
                    viewModel.showMessage("All settings reset to defaults.")
                }) { SingleLineButtonLabel("RESET") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { SingleLineButtonLabel("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_screen")
    ) {
        TopAppBar(
            title = { Text(activeCategory?.title ?: "Settings", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = {
                    if (activeCategory != null) activeCategory = null else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (activeCategory == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = CadenceSpacing.lg)
            ) {
                items(SettingsCategory.entries) { cat ->
                    val statusSummary = when (cat) {
                        SettingsCategory.APPEARANCE -> "${settings.themeConfig.baseMode.name.lowercase().replaceFirstChar { it.uppercase() }} • ${settings.ratingSystemMode.label}"
                        SettingsCategory.PLAYBACK -> "Seek ${settings.seekIntervalSec}s • ${if (settings.replayGainEnabled) "ReplayGain ON" else "ReplayGain OFF"}"
                        SettingsCategory.AUDIO_EQ -> if (settings.equalizerEnabled) "Active (${settings.equalizerSelectedPreset})" else "Bypassed"
                        SettingsCategory.LIBRARY -> "${stats?.totalTracks ?: 0} tracks • ${scanFolders.size} custom folder(s)"
                        SettingsCategory.LYRICS_PROVIDERS -> if (settings.networkLookupsEnabled) "Online lookups allowed • ${settings.lyricsStorageMode.label}" else "Strictly Offline"
                        SettingsCategory.BACKUP_STORAGE -> "ZIP archive & CSV metadata export"
                        SettingsCategory.PRIVACY_DIAGNOSTICS -> "${playbackErrors.size} playback diagnostic(s)"
                        SettingsCategory.ABOUT -> "Cadence v${BuildConfig.VERSION_NAME}"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (cat == SettingsCategory.AUDIO_EQ) {
                                    onOpenEqualizer()
                                } else {
                                    activeCategory = cat
                                }
                            }
                            .padding(horizontal = hPad, vertical = CadenceSpacing.md)
                            .testTag("settings_cat_${cat.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            cat.icon,
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(CadenceSpacing.lg))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.title, style = MaterialTheme.typography.subtitle1)
                            Text(cat.subtitle, style = MaterialTheme.typography.body2, color = ext.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(statusSummary, style = MaterialTheme.typography.caption.tabular(), color = MaterialTheme.colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Divider(color = ext.dividerColor, startIndent = 56.dp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = CadenceSpacing.xxl)
            ) {
                when (activeCategory) {
                    SettingsCategory.APPEARANCE -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                FullWidthSectionHeader("Track Scoring System")
                                Text(
                                    "Choose either Favorite Hearts or 5-Star Ratings across lists, Now Playing, mini player, and Home shelves.",
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText
                                )
                                RatingSystemMode.entries.forEach { mode ->
                                    val selected = settings.ratingSystemMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(CadenceRadius.sm),
                                        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else ext.elevatedSurface,
                                        border = BorderStroke(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colors.primary else ext.subtleBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { update { it.copy(ratingSystemMode = mode) } }
                                            .testTag("setting_rating_mode_${mode.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = CadenceSpacing.md, vertical = CadenceSpacing.sm),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (mode == RatingSystemMode.HEARTS) Icons.Default.Favorite else Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (selected) MaterialTheme.colors.primary else ext.subtleText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(CadenceSpacing.md))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = mode.label,
                                                    style = MaterialTheme.typography.subtitle2,
                                                    color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                                )
                                                Text(
                                                    text = mode.description,
                                                    style = MaterialTheme.typography.caption,
                                                    color = ext.subtleText
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colors.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                FullWidthSectionHeader("Theme Studio")
                                Text(
                                    "Customize colors, AMOLED Black, Artwork-Adaptive themes, font scaling, geometry, and Now Playing layout.",
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText
                                )
                                Button(
                                    onClick = onOpenThemeGenerator,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("open_theme_studio_button")
                                ) {
                                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(CadenceSpacing.sm))
                                    SingleLineButtonLabel("OPEN THEME STUDIO")
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                FullWidthSectionHeader("Mini Player Icons")
                                Text(
                                    "Choose which quick controls and indicators appear on the persistent mini player bar.",
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText
                                )
                                SettingSwitchRow("Show Album Artwork", "Display square cover thumbnail", settings.miniPlayerShowArtwork) { v ->
                                    update { it.copy(miniPlayerShowArtwork = v) }
                                }
                                SettingSwitchRow("Favorite / Star Control", "Quick-score current track from mini bar", settings.miniPlayerShowFavorite) { v ->
                                    update { it.copy(miniPlayerShowFavorite = v) }
                                }
                                SettingSwitchRow("Previous Track Button", "Show skip-previous icon", settings.miniPlayerShowPrevious) { v ->
                                    update { it.copy(miniPlayerShowPrevious = v) }
                                }
                                SettingSwitchRow("Play / Pause Button", "Primary playback toggle icon", settings.miniPlayerShowPlayPause) { v ->
                                    update { it.copy(miniPlayerShowPlayPause = v) }
                                }
                                SettingSwitchRow("Next Track Button", "Show skip-next icon", settings.miniPlayerShowNext) { v ->
                                    update { it.copy(miniPlayerShowNext = v) }
                                }
                                SettingSwitchRow("Queue Shortcut Button", "Open active playback queue directly", settings.miniPlayerShowQueue) { v ->
                                    update { it.copy(miniPlayerShowQueue = v) }
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                FullWidthSectionHeader("Navigation Tabs & Home")
                                SettingSwitchRow(
                                    title = "Auto-Generated Smart Mixes",
                                    subtitle = "Generate mixes on Home from top artists, genres, and user tags",
                                    checked = settings.homeShowGeneratedMixes
                                ) { v -> update { it.copy(homeShowGeneratedMixes = v) } }
                                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                                Text(
                                    "Reorder or toggle Library tabs. Phones show up to the first 5 visible tabs in the bottom bar; tablets show all visible tabs in the rail.",
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText
                                )
                                val configurableTabs = settings.navOrder.filter { it != NavDestinationKey.SETTINGS }
                                val visibleCount = configurableTabs.count { it !in settings.hiddenNavItems }
                                configurableTabs.forEachIndexed { idx, navKey ->
                                    val isVisible = navKey !in settings.hiddenNavItems
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(navKey.label, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val updated = configurableTabs.toMutableList()
                                                    val moved = updated.removeAt(idx)
                                                    updated.add(idx - 1, moved)
                                                    update { it.copy(navOrder = updated + NavDestinationKey.SETTINGS) }
                                                }
                                            },
                                            enabled = idx > 0
                                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp)) }
                                        IconButton(
                                            onClick = {
                                                if (idx < configurableTabs.lastIndex) {
                                                    val updated = configurableTabs.toMutableList()
                                                    val moved = updated.removeAt(idx)
                                                    updated.add(idx + 1, moved)
                                                    update { it.copy(navOrder = updated + NavDestinationKey.SETTINGS) }
                                                }
                                            },
                                            enabled = idx < configurableTabs.lastIndex
                                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp)) }
                                        Switch(
                                            checked = isVisible,
                                            enabled = !isVisible || visibleCount > 2,
                                            onCheckedChange = { show ->
                                                val nextHidden = if (show) settings.hiddenNavItems - navKey else settings.hiddenNavItems + navKey
                                                update { it.copy(hiddenNavItems = nextHidden + NavDestinationKey.SETTINGS) }
                                            }
                                        )
                                    }
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                FullWidthSectionHeader("Home Shelves Order & Visibility")
                                settings.homeSectionOrder.forEachIndexed { idx, sec ->
                                    val isVisible = sec !in settings.hiddenHomeSections
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(sec.label, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val list = settings.homeSectionOrder.toMutableList()
                                                    val moved = list.removeAt(idx)
                                                    list.add(idx - 1, moved)
                                                    update { it.copy(homeSectionOrder = list) }
                                                }
                                            },
                                            enabled = idx > 0
                                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp)) }
                                        IconButton(
                                            onClick = {
                                                if (idx < settings.homeSectionOrder.lastIndex) {
                                                    val list = settings.homeSectionOrder.toMutableList()
                                                    val moved = list.removeAt(idx)
                                                    list.add(idx + 1, moved)
                                                    update { it.copy(homeSectionOrder = list) }
                                                }
                                            },
                                            enabled = idx < settings.homeSectionOrder.lastIndex
                                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp)) }
                                        Switch(
                                            checked = isVisible,
                                            onCheckedChange = { show ->
                                                val nextHidden = if (show) settings.hiddenHomeSections - sec else settings.hiddenHomeSections + sec
                                                update { it.copy(hiddenHomeSections = nextHidden) }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.PLAYBACK -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                FullWidthSectionHeader("Audio & Playback Engine")
                                SettingSwitchRow("Fade on Pause / Resume", "Smooth volume ramp during transitions", settings.fadeOnPauseResume) { v -> update { it.copy(fadeOnPauseResume = v) } }
                                SettingSwitchRow("Gapless Playback", "Continuous playback without track gaps", settings.gaplessPlayback) { v -> update { it.copy(gaplessPlayback = v) } }
                                SettingSwitchRow("Skip Silence", "Trim extended silence inside audio streams", settings.skipSilence) { v -> update { it.copy(skipSilence = v) } }
                                SettingSwitchRow("ReplayGain Normalization", "Balance loudness across tracks using tags", settings.replayGainEnabled) { v -> update { it.copy(replayGainEnabled = v) } }
                                SettingSwitchRow("Resume Track Position", "Remember playback timestamp on restart", settings.resumeTrackPosition) { v -> update { it.copy(resumeTrackPosition = v) } }
                            }
                            Divider(color = ext.dividerColor)
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                FullWidthSectionHeader("Notification & Lockscreen Icons")
                                Text(
                                    "Customize media notification action buttons on Android shade and lockscreen.",
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText
                                )
                                SettingSwitchRow("Previous Track", "Allow skipping back from notification", settings.notificationShowPrevious) { v ->
                                    update { it.copy(notificationShowPrevious = v) }
                                }
                                SettingSwitchRow("Next Track", "Allow skipping forward from notification", settings.notificationShowNext) { v ->
                                    update { it.copy(notificationShowNext = v) }
                                }
                                SettingSwitchRow("Favorite / Heart Toggle", "Toggle track favorite directly from lockscreen", settings.notificationShowFavorite) { v ->
                                    update { it.copy(notificationShowFavorite = v) }
                                }
                                SettingSwitchRow("Seek Back (${settings.seekIntervalSec}s)", "Custom rewind command button", settings.notificationShowSeekBack) { v ->
                                    update { it.copy(notificationShowSeekBack = v) }
                                }
                                SettingSwitchRow("Seek Forward (${settings.seekIntervalSec}s)", "Custom fast-forward command button", settings.notificationShowSeekForward) { v ->
                                    update { it.copy(notificationShowSeekForward = v) }
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                FullWidthSectionHeader("Queue & Hardware Behavior")
                                SettingSwitchRow("Auto-Remove Played from Queue", "Clean queue as tracks finish", settings.autoRemovePlayedFromQueue) { v -> update { it.copy(autoRemovePlayedFromQueue = v) } }
                                SettingSwitchRow("Prevent Queue Duplicates", "Skip items already present in queue", settings.preventQueueDuplicates) { v -> update { it.copy(preventQueueDuplicates = v) } }
                                SettingSwitchRow("Pause on Headset Disconnect", "Automatically pause when unplugging audio", settings.pauseOnHeadsetDisconnect) { v -> update { it.copy(pauseOnHeadsetDisconnect = v) } }
                                SettingSwitchRow("Duck Audio Focus", "Lower volume during navigation/notifications", settings.duckOnAudioFocusLoss) { v -> update { it.copy(duckOnAudioFocusLoss = v) } }

                                Spacer(Modifier.height(CadenceSpacing.xs))
                                Text("Quick Seek Interval: ${settings.seekIntervalSec}s", style = MaterialTheme.typography.caption.tabular())
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                                    listOf(5, 10, 15, 30).forEach { sec ->
                                        FilterChip(
                                            selected = settings.seekIntervalSec == sec,
                                            onClick = { update { it.copy(seekIntervalSec = sec) } }
                                        ) { SingleLineButtonLabel("${sec}s", style = MaterialTheme.typography.caption) }
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.LIBRARY -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                FullWidthSectionHeader("Library Scanner & Folders")
                                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                                    Button(
                                        onClick = { viewModel.triggerLibraryScan(false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        SingleLineButtonLabel("SCAN NOW")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.triggerLibraryScan(true) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        SingleLineButtonLabel("RE-READ TAGS")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { safFolderLauncher.launch(null) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    SingleLineButtonLabel("ADD STORAGE FOLDER (SAF)")
                                }

                                if (scanFolders.isNotEmpty()) {
                                    Spacer(Modifier.height(CadenceSpacing.xs))
                                    FullWidthSectionHeader("Configured Folders")
                                    scanFolders.forEach { sf ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sf.displayName, style = MaterialTheme.typography.body2)
                                                Text(
                                                    text = "${if (sf.isExcluded) "EXCLUDED" else "INCLUDED"} • ${sf.uriOrPath}",
                                                    style = MaterialTheme.typography.caption,
                                                    color = if (sf.isExcluded) MaterialTheme.colors.error else ext.subtleText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    viewModel.container.database.customizationAndCacheDao().deleteScanFolder(sf.id)
                                                }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove Folder Rule")
                                            }
                                        }
                                    }
                                }

                                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                                SettingSwitchRow("Auto-Scan on Launch", "Check for new/updated audio files on startup", settings.autoScanOnLaunch) { v -> update { it.copy(autoScanOnLaunch = v) } }
                                SettingSwitchRow("Include Hidden Folders", "Index dot-prefixed directories", settings.includeHiddenFolders) { v -> update { it.copy(includeHiddenFolders = v) } }
                                SettingSwitchRow("Respect .nomedia Files", "Skip folders containing .nomedia", settings.respectNomedia) { v -> update { it.copy(respectNomedia = v) } }

                                Text("Minimum Track Duration: ${settings.minTrackDurationSec} sec", style = MaterialTheme.typography.caption.tabular())
                                Slider(
                                    value = settings.minTrackDurationSec.toFloat(),
                                    onValueChange = { v -> update { it.copy(minTrackDurationSec = v.toInt()) } },
                                    valueRange = 0f..60f
                                )

                                Text("Indexed Audio Formats", style = MaterialTheme.typography.caption, color = ext.subtleText)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                                    listOf("MP3", "FLAC", "M4A", "AAC", "OGG", "OPUS", "WAV", "ALAC", "AIFF", "WMA", "APE").forEach { fmt ->
                                        val enabled = fmt in settings.enabledFormats
                                        FilterChip(
                                            selected = enabled,
                                            onClick = {
                                                val next = if (enabled && settings.enabledFormats.size > 1) settings.enabledFormats - fmt else settings.enabledFormats + fmt
                                                update { it.copy(enabledFormats = next) }
                                            }
                                        ) { SingleLineButtonLabel(fmt, style = MaterialTheme.typography.caption) }
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.LYRICS_PROVIDERS -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                FullWidthSectionHeader("Lyrics & Online Lookups")
                                SettingSwitchRow("Enable Optional Online Lookups", "App stays 100% offline unless you allow metadata/lyrics fetches", settings.networkLookupsEnabled) { v -> update { it.copy(networkLookupsEnabled = v) } }
                                SettingSwitchRow("Auto-Lookup Lyrics", "Fetch lyrics automatically when opening Lyrics Studio", settings.lyricsAutoLookup) { v -> update { it.copy(lyricsAutoLookup = v) } }
                                SettingSwitchRow("Auto-Save Fetched Lyrics", "Cache retrieved lyrics locally for offline reading", settings.lyricsAutoSave) { v -> update { it.copy(lyricsAutoSave = v) } }
                                SettingSwitchRow("Auto-Scroll Synced Lyrics", "Follow playback position automatically", settings.lyricsAutoScroll) { v -> update { it.copy(lyricsAutoScroll = v) } }

                                Text("Default Lyrics Save Mode", style = MaterialTheme.typography.caption, color = ext.subtleText)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                                    LyricsStorageMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = settings.lyricsStorageMode == mode,
                                            onClick = { update { it.copy(lyricsStorageMode = mode) } }
                                        ) { SingleLineButtonLabel(mode.label, style = MaterialTheme.typography.caption) }
                                    }
                                }

                                Text("Auto-Match Confidence Threshold: ${(settings.autoMatchConfidenceThreshold * 100).toInt()}%", style = MaterialTheme.typography.caption.tabular())
                                Slider(
                                    value = settings.autoMatchConfidenceThreshold,
                                    onValueChange = { v -> update { it.copy(autoMatchConfidenceThreshold = v) } },
                                    valueRange = 0.50f..0.95f
                                )
                            }
                            Divider(color = ext.dividerColor)
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                FullWidthSectionHeader("Metadata Provider Priority")
                                settings.metadataProviderPriority.forEachIndexed { idx, pid ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${idx + 1}. ${pid.displayName}", style = MaterialTheme.typography.body2)
                                            Text(
                                                text = if (pid.requiresKey) "Optional user key" else "No API key needed",
                                                style = MaterialTheme.typography.caption,
                                                color = if (pid.requiresKey) ext.subtleText else MaterialTheme.colors.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val list = settings.metadataProviderPriority.toMutableList()
                                                    val m = list.removeAt(idx); list.add(idx - 1, m)
                                                    update { it.copy(metadataProviderPriority = list) }
                                                }
                                            },
                                            enabled = idx > 0
                                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp)) }
                                        IconButton(
                                            onClick = {
                                                if (idx < settings.metadataProviderPriority.lastIndex) {
                                                    val list = settings.metadataProviderPriority.toMutableList()
                                                    val m = list.removeAt(idx); list.add(idx + 1, m)
                                                    update { it.copy(metadataProviderPriority = list) }
                                                }
                                            },
                                            enabled = idx < settings.metadataProviderPriority.lastIndex
                                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp)) }
                                    }
                                }

                                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(CadenceSpacing.xs))
                                    FullWidthSectionHeader("Optional Provider API Keys")
                                }
                                Text(
                                    "US iTunes Search, LRCLIB, MusicBrainz, Cover Art Archive, and Deezer work with zero API keys. Enter keys below only if you want optional Discogs, AcoustID, or Musixmatch lookups.",
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText
                                )
                                var discogsTok by remember(settings.discogsTokenOverride) { mutableStateOf(settings.discogsTokenOverride) }
                                var acoustKey by remember(settings.acoustIdKeyOverride) { mutableStateOf(settings.acoustIdKeyOverride) }
                                var mxmKey by remember(settings.musixmatchKeyOverride) { mutableStateOf(settings.musixmatchKeyOverride) }

                                OutlinedTextField(
                                    value = discogsTok,
                                    onValueChange = { discogsTok = it },
                                    label = { Text("Discogs Personal Token (Optional)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = acoustKey,
                                    onValueChange = { acoustKey = it },
                                    label = { Text("AcoustID API Key (Optional)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = mxmKey,
                                    onValueChange = { mxmKey = it },
                                    label = { Text("Musixmatch API Key (Optional)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            discogsTok = ""
                                            acoustKey = ""
                                            mxmKey = ""
                                            update {
                                                it.copy(
                                                    appleMusicTokenOverride = "",
                                                    discogsTokenOverride = "",
                                                    acoustIdKeyOverride = "",
                                                    musixmatchKeyOverride = ""
                                                )
                                            }
                                            viewModel.showMessage("Cleared optional API keys.")
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { SingleLineButtonLabel("CLEAR KEYS") }
                                    Button(
                                        onClick = {
                                            update {
                                                it.copy(
                                                    appleMusicTokenOverride = "",
                                                    discogsTokenOverride = discogsTok.trim(),
                                                    acoustIdKeyOverride = acoustKey.trim(),
                                                    musixmatchKeyOverride = mxmKey.trim()
                                                )
                                            }
                                            viewModel.showMessage("Saved optional API keys locally.")
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { SingleLineButtonLabel("SAVE KEYS") }
                                }
                            }
                        }
                    }

                    SettingsCategory.BACKUP_STORAGE -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                FullWidthSectionHeader("Backup, Restore & Export")
                                Text(
                                    "Export playlists, smart rules, custom themes, EQ presets, bookmarks, and preferences into a portable ZIP archive.",
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText
                                )
                                Button(
                                    onClick = { backupExportLauncher.launch("cadence_backup.zip") },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    SingleLineButtonLabel("EXPORT BACKUP (.ZIP)")
                                }
                                OutlinedButton(
                                    onClick = { backupImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    SingleLineButtonLabel("RESTORE BACKUP (.ZIP)")
                                }
                                OutlinedButton(
                                    onClick = { csvExportLauncher.launch("cadence_library.csv") },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    SingleLineButtonLabel("EXPORT LIBRARY (.CSV)")
                                }
                            }
                        }
                    }

                    SettingsCategory.PRIVACY_DIAGNOSTICS -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                FullWidthSectionHeader("Listening Statistics & Cache")
                                stats?.let { st ->
                                    Text(
                                        "Tracks: ${st.totalTracks} • Listen Time: ${TextNormalizer.formatDuration(st.totalListenMs)} • Lyrics: ${st.tracksWithLyrics} • Artwork: ${st.tracksWithArtwork}",
                                        style = MaterialTheme.typography.caption.tabular(),
                                        color = ext.subtleText
                                    )
                                }
                                if (dailyStats.isNotEmpty()) {
                                    Text("Recent Daily Listening", style = MaterialTheme.typography.subtitle2)
                                    dailyStats.take(7).forEach { day ->
                                        Row {
                                            Text(day.dayBucket, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.caption.tabular(), modifier = Modifier.width(98.dp))
                                            Text("${TextNormalizer.formatDuration(day.totalMs)} (${day.playEvents} plays)", style = MaterialTheme.typography.caption.tabular())
                                        }
                                    }
                                }
                                SettingSwitchRow("Collect Local Listening Stats", "Track play counts, skips, and daily listening durations locally", settings.listeningStatsEnabled) { v -> update { it.copy(listeningStatsEnabled = v) } }
                                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetStatistics() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        SingleLineButtonLabel("RESET STATS")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                viewModel.container.database.customizationAndCacheDao().clearMetadataCache()
                                                viewModel.clearSearchHistory()
                                            }
                                            viewModel.showMessage("Cleared metadata cache & search history.")
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { SingleLineButtonLabel("CLEAR CACHE") }
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                FullWidthSectionHeader("Device Audio Decoder Inspector")
                                codecSupport.forEach { info ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(info.format, style = MaterialTheme.typography.subtitle2, modifier = Modifier.width(86.dp))
                                        Text(
                                            text = if (info.isSupportedByDevice) "Hardware/OS Decoder OK" else "Media3 / Limited",
                                            style = MaterialTheme.typography.caption,
                                            color = if (info.isSupportedByDevice) MaterialTheme.colors.primary else ext.subtleText,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (playbackErrors.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = CadenceSpacing.xs), color = ext.dividerColor)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Recent Playback Diagnostics (${playbackErrors.size})", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.error, modifier = Modifier.weight(1f))
                                        TextButton(
                                            onClick = { viewModel.container.playbackDiagnostics.clearAll() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) { SingleLineButtonLabel("CLEAR") }
                                    }
                                    playbackErrors.take(5).forEach { err ->
                                        Text("• ${err.message}", style = MaterialTheme.typography.caption)
                                    }
                                }
                            }
                        }
                    }

                    SettingsCategory.ABOUT -> {
                        item {
                            Surface(color = ext.elevatedSurface, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.lg),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.cadence_logo_mark_1795348934126),
                                        contentDescription = "Cadence Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                    Spacer(Modifier.width(CadenceSpacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Cadence Local Music Player", style = MaterialTheme.typography.h6)
                                        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.caption.tabular(), color = MaterialTheme.colors.primary)
                                    }
                                }
                            }
                            Divider(color = ext.dividerColor)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.md)
                            ) {
                                Text(
                                    "Offline-first Material Design 2 audio player powered by Android Media3/ExoPlayer, Room, TagLib/JAudioTagger, and real-time LRC synchronization.",
                                    style = MaterialTheme.typography.body2,
                                    color = ext.subtleText
                                )
                                OutlinedButton(
                                    onClick = { showResetConfirm = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SingleLineButtonLabel("RESET SETTINGS TO DEFAULTS", color = MaterialTheme.colors.error)
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val ext = LocalCadenceTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = CadenceSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.body2)
            Text(subtitle, style = MaterialTheme.typography.caption, color = ext.subtleText)
        }
        Spacer(Modifier.width(CadenceSpacing.sm))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
