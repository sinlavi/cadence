package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.model.LyricsBackgroundMode
import com.example.core.model.LyricsCandidate
import com.example.core.model.LyricsStorageMode
import com.example.core.util.TextNormalizer
import com.example.data.db.TrackEntity
import com.example.data.lyrics.LrcParser
import com.example.data.lyrics.LyricsLookupResult
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.TrackArtwork
import com.example.ui.theme.ArtworkPaletteColors
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private enum class LyricCardStyle(val label: String) {
    DARK("Obsidian"),
    LIGHT("Editorial"),
    ARTWORK("Artwork")
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun LyricsScreen(
    trackId: Long,
    startInEditor: Boolean,
    viewModel: MainViewModel,
    artworkPalette: ArtworkPaletteColors?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val syncState by viewModel.lyricsSyncState.collectAsState()
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current

    var track by remember(trackId) { mutableStateOf<TrackEntity?>(null) }
    var editorMode by remember { mutableIntStateOf(if (startInEditor) 2 else 0) } // 0=Viewer, 1=Plain Editor, 2=Timeline Sync Editor
    var plainDraft by remember(trackId) { mutableStateOf("") }
    var plainDraftSyncedTrackId by remember(trackId) { mutableStateOf<Long?>(null) }
    var selectedLineIdx by remember { mutableIntStateOf(0) }
    var storageMode by remember(settings.lyricsStorageMode) { mutableStateOf(settings.lyricsStorageMode) }
    var autoScroll by remember(settings.lyricsAutoScroll) { mutableStateOf(settings.lyricsAutoScroll) }
    var fontSizeSp by remember(settings.lyricsFontSizeSp) { mutableFloatStateOf(settings.lyricsFontSizeSp) }
    var showDisplayControls by remember { mutableStateOf(false) }
    var shareSelectionMode by remember { mutableStateOf(false) }
    var selectedShareIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showCardShareDialog by remember { mutableStateOf(false) }
    var cardStyle by remember { mutableStateOf(if (artworkPalette != null) LyricCardStyle.ARTWORK else LyricCardStyle.DARK) }

    var bgMode by remember(settings.themeConfig.useArtworkColors, artworkPalette) {
        mutableStateOf(
            if (settings.themeConfig.useArtworkColors && artworkPalette != null) {
                LyricsBackgroundMode.ARTWORK
            } else {
                LyricsBackgroundMode.THEME
            }
        )
    }

    var isSearching by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<LyricsCandidate>>(emptyList()) }
    var searchWarnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCandidateDialog by remember { mutableStateOf(false) }
    var customSearchQuery by remember { mutableStateOf("") }
    var overflowMenuOpen by remember { mutableStateOf(false) }

    val engine = viewModel.container.lyricsSyncEngine

    LaunchedEffect(trackId) {
        track = viewModel.container.database.trackDao().getTrackById(trackId)
        val loaded = engine.loadForTrackId(trackId)
        plainDraft = loaded.plainText
        plainDraftSyncedTrackId = trackId
    }

    LaunchedEffect(syncState.trackId, syncState.isLoading) {
        if (syncState.trackId == trackId && !syncState.isLoading && plainDraftSyncedTrackId != trackId) {
            plainDraft = syncState.plainText
            plainDraftSyncedTrackId = trackId
        }
    }

    val lines = if (syncState.trackId == trackId) syncState.lines else emptyList()
    val isCurrentPlayingTrack = playback.currentTrack?.id == trackId
    val activeLineIdx = if (syncState.trackId == trackId) syncState.activeLineIndex else -1

    BackHandler(enabled = shareSelectionMode || (!startInEditor && editorMode != 0)) {
        when {
            shareSelectionMode -> {
                shareSelectionMode = false
                selectedShareIndices = emptySet()
            }
            !startInEditor && editorMode != 0 -> {
                editorMode = 0
            }
        }
    }

    val importLrcLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val parsed = viewModel.container.lyricsRepository.importLrcFromUri(uri)
                if (parsed != null && parsed.lines.isNotEmpty()) {
                    val plain = parsed.lines.joinToString("\n") { it.text }
                    engine.updateLines(parsed.lines, plain)
                    plainDraft = plain
                    viewModel.showMessage("Imported ${parsed.lines.size} lines.")
                } else {
                    viewModel.showMessage("Could not parse LRC file.")
                }
            }
        }
    }

    val exportLrcLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val t = track ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                val serialized = LrcParser.serialize(lines, t.title, t.artist, t.album)
                val ok = viewModel.container.lyricsRepository.exportLrcToUri(uri, serialized)
                viewModel.showMessage(if (ok) "Exported .lrc file." else "Export failed.")
            }
        }
    }

    fun runProviderSearch(overrideQuery: String? = null) {
        val t = track ?: return
        isSearching = true
        scope.launch {
            val res = viewModel.container.lyricsRepository.searchProviders(t, settings, overrideQuery)
            isSearching = false
            when (res) {
                is LyricsLookupResult.Success -> {
                    candidates = res.candidates
                    searchWarnings = res.warnings
                    showCandidateDialog = true
                }
                is LyricsLookupResult.OfflineOrDisabled -> viewModel.showMessage(res.message)
                is LyricsLookupResult.Error -> viewModel.showMessage(res.message)
            }
        }
    }

    fun saveCurrentLyrics(returnToViewer: Boolean = false) {
        val t = track ?: return
        scope.launch {
            val linesToSave = if (editorMode == 1) {
                LrcParser.fromPlainLines(plainDraft)
            } else {
                lines
            }
            val plain = if (editorMode == 1) plainDraft else linesToSave.joinToString("\n") { it.text }
            val ok = engine.saveForTrack(
                track = t,
                linesToSave = linesToSave,
                plainText = plain,
                storageMode = storageMode,
                userEdited = true,
                source = "user_editor"
            )
            plainDraft = plain
            viewModel.showMessage(if (ok) "Lyrics saved (${storageMode.label})." else "Failed to save lyrics.")
            if (ok && returnToViewer && !startInEditor) {
                editorMode = 0
            }
        }
    }

    fun openEditorFromViewer() {
        shareSelectionMode = false
        selectedShareIndices = emptySet()
        showDisplayControls = false
        if (lines.isNotEmpty()) {
            if (plainDraft.isBlank()) {
                plainDraft = lines.joinToString("\n") { it.text }
            }
            editorMode = if (syncState.isSynced) 2 else 1
        } else {
            editorMode = 1
        }
    }

    fun toggleShareLine(idx: Int) {
        if (idx in selectedShareIndices) {
            val next = selectedShareIndices - idx
            selectedShareIndices = next
            if (next.isEmpty()) shareSelectionMode = false
        } else if (selectedShareIndices.size >= 6) {
            viewModel.showMessage("Select up to 6 lines for a lyric share card.")
        } else {
            shareSelectionMode = true
            selectedShareIndices = selectedShareIndices + idx
        }
    }

    fun resolveSelectedLyricsText(): String {
        val sorted = selectedShareIndices.sorted().mapNotNull { lines.getOrNull(it)?.text?.trim() }.filter { it.isNotEmpty() }
        return if (sorted.isNotEmpty()) {
            sorted.joinToString("\n")
        } else {
            lines.joinToString("\n") { it.text }.ifBlank { plainDraft }
        }
    }

    fun shareLyricsAsText() {
        val t = track ?: return
        val quote = resolveSelectedLyricsText()
        if (quote.isBlank()) {
            viewModel.showMessage("No lyrics available to share.")
            return
        }
        val payload = buildString {
            append(quote)
            append("\n\n— ")
            append(t.title)
            if (t.artist.isNotBlank()) {
                append(" • ")
                append(t.artist)
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${t.title} — ${t.artist}")
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        context.startActivity(Intent.createChooser(intent, "Share Lyrics"))
    }

    if (showCardShareDialog) {
        val t = track
        val quoteLines = remember(selectedShareIndices, lines, plainDraft) {
            val picked = selectedShareIndices.sorted().mapNotNull { lines.getOrNull(it)?.text?.trim() }.filter { it.isNotEmpty() }
            if (picked.isNotEmpty()) picked else lines.map { it.text.trim() }.filter { it.isNotEmpty() }.take(4)
        }
        val previewBg = when (cardStyle) {
            LyricCardStyle.DARK -> Color(0xFF0E131D)
            LyricCardStyle.LIGHT -> Color(0xFFF6F7FB)
            LyricCardStyle.ARTWORK -> artworkPalette?.dominant ?: MaterialTheme.colors.primary
        }
        val previewBgBottom = when (cardStyle) {
            LyricCardStyle.DARK -> Color(0xFF172033)
            LyricCardStyle.LIGHT -> Color(0xFFE7ECF5)
            LyricCardStyle.ARTWORK -> artworkPalette?.muted ?: Color(0xFF151923)
        }
        val previewInk = when (cardStyle) {
            LyricCardStyle.DARK -> Color(0xFFF5F7FB)
            LyricCardStyle.LIGHT -> Color(0xFF111827)
            LyricCardStyle.ARTWORK -> contrastOnColor(previewBg)
        }
        val previewAccent = when (cardStyle) {
            LyricCardStyle.DARK -> MaterialTheme.colors.primary
            LyricCardStyle.LIGHT -> MaterialTheme.colors.primary
            LyricCardStyle.ARTWORK -> artworkPalette?.vibrant ?: previewInk
        }
        var isGenerating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isGenerating) showCardShareDialog = false },
            title = { Text("Share Lyric Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs), modifier = Modifier.fillMaxWidth()) {
                        LyricCardStyle.entries.forEach { st ->
                            val enabled = st != LyricCardStyle.ARTWORK || artworkPalette != null
                            FilterChip(
                                selected = cardStyle == st,
                                enabled = enabled,
                                onClick = { cardStyle = st }
                            ) {
                                SingleLineButtonLabel(st.label, style = MaterialTheme.typography.caption)
                            }
                        }
                    }

                    // Live Card Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(previewBg, previewBgBottom)), shape = RoundedCornerShape(CadenceRadius.md))
                            .padding(CadenceSpacing.lg)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(CadenceSpacing.md)) {
                            Text(
                                text = "“",
                                style = MaterialTheme.typography.h4,
                                fontWeight = FontWeight.Bold,
                                color = previewAccent
                            )
                            Text(
                                text = quoteLines.joinToString("\n").ifBlank { t?.title ?: "" },
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.SemiBold,
                                color = previewInk,
                                lineHeight = 22.sp
                            )
                            Divider(color = previewInk.copy(alpha = 0.16f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (t != null) {
                                    TrackArtwork(
                                        artworkUri = t.artworkUri,
                                        contentUri = t.contentUri,
                                        contentDescription = t.title,
                                        cornerRadius = 6.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.width(CadenceSpacing.sm))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = t?.title ?: "Unknown Track",
                                        style = MaterialTheme.typography.subtitle2,
                                        fontWeight = FontWeight.SemiBold,
                                        color = previewInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = t?.artist ?: "Unknown Artist",
                                        style = MaterialTheme.typography.caption,
                                        color = previewInk.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isGenerating && t != null && quoteLines.isNotEmpty(),
                    onClick = {
                        val tr = t ?: return@Button
                        isGenerating = true
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                renderAndSaveLyricCardPng(
                                    context = context,
                                    track = tr,
                                    lines = quoteLines,
                                    topColor = previewBg.toArgb(),
                                    bottomColor = previewBgBottom.toArgb(),
                                    inkColor = previewInk.toArgb(),
                                    accentColor = previewAccent.toArgb()
                                )
                            }
                            isGenerating = false
                            if (uri != null) {
                                showCardShareDialog = false
                                shareSelectionMode = false
                                selectedShareIndices = emptySet()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, "${tr.title} — ${tr.artist}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Lyric Card"))
                            } else {
                                viewModel.showMessage("Could not generate lyric card image.")
                            }
                        }
                    }
                ) {
                    SingleLineButtonLabel(if (isGenerating) "RENDERING..." else "SHARE IMAGE")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                    TextButton(onClick = {
                        showCardShareDialog = false
                        shareLyricsAsText()
                    }) { SingleLineButtonLabel("AS TEXT") }
                    TextButton(onClick = { showCardShareDialog = false }) { SingleLineButtonLabel("CLOSE") }
                }
            }
        )
    }

    if (showCandidateDialog) {
        AlertDialog(
            onDismissRequest = { showCandidateDialog = false },
            title = { Text("Lyrics Search Results") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customSearchQuery,
                            onValueChange = { customSearchQuery = it },
                            label = { Text("Refine search query") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(CadenceSpacing.xs))
                        Button(
                            onClick = { runProviderSearch(customSearchQuery) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) { SingleLineButtonLabel("Go") }
                    }
                    searchWarnings.forEach { w ->
                        Text(w, style = MaterialTheme.typography.caption, color = ext.subtleText, modifier = Modifier.padding(top = CadenceSpacing.xs))
                    }
                    Spacer(Modifier.height(CadenceSpacing.sm))
                    if (candidates.isEmpty()) {
                        Text("No matching lyrics found across enabled providers.", style = MaterialTheme.typography.body2)
                    } else {
                        candidates.forEach { cand ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val t = track ?: return@clickable
                                        showCandidateDialog = false
                                        scope.launch {
                                            val parsed = engine.applyCandidate(t, cand, storageMode, settings)
                                            plainDraft = cand.plainLyrics ?: parsed.lines.joinToString("\n") { it.text }
                                            viewModel.showMessage("Loaded lyrics from ${cand.provider.displayName}.")
                                        }
                                    }
                                    .padding(vertical = CadenceSpacing.sm)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        cand.trackName,
                                        style = MaterialTheme.typography.subtitle2,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(CadenceSpacing.xs))
                                    Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.14f), shape = MaterialTheme.shapes.small) {
                                        Text(
                                            text = if (!cand.syncedLyrics.isNullOrBlank()) "SYNCED • ${(cand.confidenceScore * 100).toInt()}%" else "PLAIN • ${(cand.confidenceScore * 100).toInt()}%",
                                            style = MaterialTheme.typography.overline.tabular(),
                                            color = MaterialTheme.colors.primary,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("${cand.artistName} • ${cand.provider.displayName}", style = MaterialTheme.typography.caption, color = ext.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Divider(color = ext.dividerColor)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCandidateDialog = false }) { SingleLineButtonLabel("CLOSE") } }
        )
    }

    val listState = rememberLazyListState()
    val syncListState = rememberLazyListState()

    LaunchedEffect(listState.isScrollInProgress, editorMode) {
        if (listState.isScrollInProgress && editorMode == 0 && autoScroll) {
            engine.pauseAutoScroll()
        }
    }

    LaunchedEffect(activeLineIdx, autoScroll, syncState.manualScrollPaused, editorMode) {
        if (editorMode == 0 && autoScroll && !syncState.manualScrollPaused && activeLineIdx >= 0) {
            listState.animateScrollToItem((activeLineIdx - 2).coerceAtLeast(0))
        }
    }

    LaunchedEffect(selectedLineIdx, editorMode) {
        if (editorMode == 2 && selectedLineIdx >= 0 && selectedLineIdx < lines.size) {
            syncListState.animateScrollToItem((selectedLineIdx - 2).coerceAtLeast(0))
        }
    }

    val readerColors = remember(bgMode, artworkPalette, MaterialTheme.colors.background, MaterialTheme.colors.onBackground, MaterialTheme.colors.primary) {
        when (bgMode) {
            LyricsBackgroundMode.DARK -> ReaderPalette(
                bgColor = Color(0xFF0E1117),
                gradient = null,
                textColor = Color(0xFFF5F7FB),
                subtleColor = Color(0xFF9AA3B5),
                accentColor = Color(0xFFF59E0B)
            )
            LyricsBackgroundMode.LIGHT -> ReaderPalette(
                bgColor = Color(0xFFF8F9FC),
                gradient = null,
                textColor = Color(0xFF141821),
                subtleColor = Color(0xFF616878),
                accentColor = Color(0xFF2563EB)
            )
            LyricsBackgroundMode.ARTWORK -> if (artworkPalette != null) {
                val baseDark = artworkPalette.muted.copy(alpha = 0.94f)
                val top = artworkPalette.dominant.copy(alpha = 0.88f)
                val ink = contrastOnColor(artworkPalette.dominant)
                ReaderPalette(
                    bgColor = baseDark,
                    gradient = Brush.verticalGradient(listOf(top, baseDark)),
                    textColor = ink,
                    subtleColor = ink.copy(alpha = 0.68f),
                    accentColor = artworkPalette.vibrant
                )
            } else {
                null
            }
            LyricsBackgroundMode.THEME -> null
        }
    }

    val containerBgModifier = if (editorMode == 0 && readerColors != null) {
        if (readerColors.gradient != null) Modifier.background(readerColors.gradient) else Modifier.background(readerColors.bgColor)
    } else {
        Modifier.background(MaterialTheme.colors.background)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(containerBgModifier)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lyrics_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                elevation = 0.dp,
                title = {
                    Column {
                        Text(
                            text = if (editorMode == 0) (track?.title ?: "Lyrics") else "Edit Lyrics",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track?.artist ?: "",
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            shareSelectionMode -> {
                                shareSelectionMode = false
                                selectedShareIndices = emptySet()
                            }
                            !startInEditor && editorMode != 0 -> editorMode = 0
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp).padding(4.dp), strokeWidth = 2.dp)
                    }
                    if (editorMode == 0) {
                        IconButton(onClick = { showDisplayControls = !showDisplayControls }) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Display Controls",
                                tint = if (showDisplayControls) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                            )
                        }
                        if (lines.isNotEmpty() || plainDraft.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (lines.isNotEmpty()) {
                                        if (!shareSelectionMode) {
                                            shareSelectionMode = true
                                            if (selectedShareIndices.isEmpty() && activeLineIdx in lines.indices) {
                                                selectedShareIndices = setOf(activeLineIdx)
                                            }
                                        } else if (selectedShareIndices.isNotEmpty()) {
                                            showCardShareDialog = true
                                        } else {
                                            shareSelectionMode = false
                                        }
                                    } else {
                                        shareLyricsAsText()
                                    }
                                },
                                modifier = Modifier.testTag("lyrics_share_btn")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share Lyrics",
                                    tint = if (shareSelectionMode) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                )
                            }
                        }
                        IconButton(
                            onClick = { openEditorFromViewer() },
                            modifier = Modifier.testTag("lyrics_edit_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit & Sync Lyrics")
                        }
                    } else {
                        IconButton(
                            onClick = { saveCurrentLyrics(returnToViewer = true) },
                            modifier = Modifier.testTag("lyrics_save_done_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save & View", tint = MaterialTheme.colors.primary)
                        }
                    }
                    Box {
                        IconButton(onClick = { overflowMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More lyrics actions")
                        }
                        DropdownMenu(expanded = overflowMenuOpen, onDismissRequest = { overflowMenuOpen = false }) {
                            DropdownMenuItem(onClick = {
                                overflowMenuOpen = false
                                runProviderSearch()
                            }) { Text("Fetch Online Lyrics (LRCLIB)") }
                            DropdownMenuItem(onClick = {
                                overflowMenuOpen = false
                                importLrcLauncher.launch(arrayOf("text/*", "application/octet-stream"))
                            }) { Text("Import .LRC File") }
                            DropdownMenuItem(onClick = {
                                overflowMenuOpen = false
                                val base = TextNormalizer.sanitizeFilename("${track?.artist ?: "track"} - ${track?.title ?: "lyrics"}")
                                exportLrcLauncher.launch("$base.lrc")
                            }) { Text("Export .LRC File") }
                            DropdownMenuItem(onClick = {
                                overflowMenuOpen = false
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = lines.joinToString("\n") { it.text }.ifBlank { plainDraft }
                                cm.setPrimaryClip(ClipData.newPlainText("Lyrics", text))
                                viewModel.showMessage("Lyrics copied to clipboard.")
                            }) { Text("Copy All Lyrics") }
                            if (editorMode == 0 && (lines.isNotEmpty() || plainDraft.isNotBlank())) {
                                DropdownMenuItem(onClick = {
                                    overflowMenuOpen = false
                                    shareLyricsAsText()
                                }) { Text("Share Full Lyrics as Text") }
                            }
                        }
                    }
                }
            )

            // Editor Sub-Tabs ONLY when in Editor mode (Viewer stays distraction-free!)
            if (editorMode != 0) {
                TabRow(selectedTabIndex = if (editorMode == 1) 0 else 1) {
                    Tab(
                        selected = editorMode == 1,
                        onClick = {
                            if (plainDraft.isBlank() && lines.isNotEmpty()) {
                                plainDraft = lines.joinToString("\n") { it.text }
                            }
                            editorMode = 1
                        },
                        text = { SingleLineButtonLabel("Plain Text") }
                    )
                    Tab(
                        selected = editorMode == 2,
                        onClick = {
                            if (lines.isEmpty() && plainDraft.isNotBlank()) {
                                engine.updateLines(LrcParser.fromPlainLines(plainDraft), plainDraft)
                            }
                            editorMode = 2
                        },
                        text = { SingleLineButtonLabel("Timeline Sync") }
                    )
                }
            }

            when (editorMode) {
                // 0: DISTRACTION-FREE SYNCHRONIZED VIEWER
                0 -> {
                    val primaryTextColor = readerColors?.textColor ?: MaterialTheme.colors.onBackground
                    val subtleTextColor = readerColors?.subtleColor ?: ext.subtleText
                    val highlightColor = readerColors?.accentColor ?: MaterialTheme.colors.primary

                    AnimatedVisibility(visible = showDisplayControls) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ext.elevatedSurface.copy(alpha = 0.85f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Size ${fontSizeSp.toInt()}sp", style = MaterialTheme.typography.caption.tabular(), color = subtleTextColor)
                                Slider(
                                    value = fontSizeSp,
                                    onValueChange = { fontSizeSp = it },
                                    valueRange = 14f..28f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = CadenceSpacing.sm)
                                )
                                TextButton(onClick = {
                                    autoScroll = !autoScroll
                                    if (autoScroll) engine.resumeAutoScroll() else engine.pauseAutoScroll()
                                }) {
                                    SingleLineButtonLabel(
                                        text = if (autoScroll && !syncState.manualScrollPaused) "Auto" else "Manual",
                                        color = highlightColor
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                items(LyricsBackgroundMode.entries) { mode ->
                                    val enabled = mode != LyricsBackgroundMode.ARTWORK || artworkPalette != null
                                    FilterChip(
                                        selected = bgMode == mode,
                                        enabled = enabled,
                                        onClick = { bgMode = mode }
                                    ) {
                                        SingleLineButtonLabel(mode.label, style = MaterialTheme.typography.caption)
                                    }
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }
                    }

                    if (shareSelectionMode) {
                        Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Tap up to 6 lines to share • ${selectedShareIndices.size}/6 selected",
                                style = MaterialTheme.typography.caption.tabular(),
                                color = MaterialTheme.colors.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = CadenceSpacing.md)
                            )
                        }
                    }

                    if (syncState.isLoading) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (lines.isEmpty()) {
                        EmptyStateView(
                            title = "No Lyrics Loaded",
                            subtitle = "Fetch synchronized LRC lyrics from LRCLIB, import a .lrc file, or tap Edit to type & sync manually.",
                            actionLabel = "Search LRCLIB",
                            onAction = { runProviderSearch() },
                            icon = Icons.Default.MusicNote
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = CadenceSpacing.lg,
                                end = CadenceSpacing.lg,
                                top = CadenceSpacing.lg,
                                bottom = if (shareSelectionMode) 96.dp else CadenceSpacing.xxl
                            ),
                            verticalArrangement = Arrangement.spacedBy(CadenceSpacing.md)
                        ) {
                            itemsIndexed(lines, key = { idx, l -> "${l.id}_$idx" }) { idx, line ->
                                val isActive = idx == activeLineIdx
                                val isPassed = activeLineIdx >= 0 && idx < activeLineIdx
                                val isSharePicked = idx in selectedShareIndices
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = when {
                                        isSharePicked -> MaterialTheme.colors.primary.copy(alpha = 0.24f)
                                        isActive -> highlightColor.copy(alpha = 0.16f)
                                        else -> Color.Transparent
                                    },
                                    border = if (isSharePicked) BorderStroke(1.dp, MaterialTheme.colors.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (shareSelectionMode) {
                                                    toggleShareLine(idx)
                                                } else if (line.timeMs != null) {
                                                    engine.seekToLine(line, track)
                                                }
                                            },
                                            onLongClick = { toggleShareLine(idx) }
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = CadenceSpacing.md, vertical = CadenceSpacing.sm),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (line.timeMs != null && isActive && !shareSelectionMode) {
                                            Text(
                                                text = LrcParser.formatTimestamp(line.timeMs),
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.overline.tabular(),
                                                color = highlightColor
                                            )
                                            Spacer(Modifier.height(2.dp))
                                        }

                                        if (isActive && line.words.isNotEmpty() && syncState.activeWordIndex >= 0) {
                                            val annotated = buildAnnotatedString {
                                                line.words.forEachIndexed { wIdx, w ->
                                                    val reached = wIdx <= syncState.activeWordIndex
                                                    withStyle(
                                                        SpanStyle(
                                                            color = if (reached) highlightColor else primaryTextColor.copy(alpha = 0.55f),
                                                            fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    ) {
                                                        append(w.text)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = annotated,
                                                fontSize = (fontSizeSp + 2f).sp,
                                                lineHeight = (fontSizeSp * settings.lyricsLineSpacingMultiplier).sp,
                                                textAlign = TextAlign.Center
                                            )
                                        } else {
                                            Text(
                                                text = line.text.ifBlank { "♪" },
                                                fontSize = if (isActive) (fontSizeSp + 2f).sp else fontSizeSp.sp,
                                                lineHeight = (fontSizeSp * settings.lyricsLineSpacingMultiplier).sp,
                                                fontWeight = if (isActive || isSharePicked) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSharePicked -> primaryTextColor
                                                    isActive -> highlightColor
                                                    isPassed -> primaryTextColor.copy(alpha = 0.45f)
                                                    else -> primaryTextColor.copy(alpha = 0.82f)
                                                },
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        if (isActive && syncState.isSynced && !shareSelectionMode) {
                                            Spacer(Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = syncState.lineProgress,
                                                color = highlightColor,
                                                backgroundColor = highlightColor.copy(alpha = 0.18f),
                                                modifier = Modifier.fillMaxWidth(0.55f).height(2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1: PLAIN LYRICS EDITOR
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isCompact) CadenceSpacing.md else CadenceSpacing.lg)
                    ) {
                        StorageModeRow(storageMode) { storageMode = it }
                        Spacer(Modifier.height(CadenceSpacing.sm))
                        OutlinedTextField(
                            value = plainDraft,
                            onValueChange = { plainDraft = it },
                            label = { Text("Paste or write lyrics (one line per verse)") },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        Spacer(Modifier.height(CadenceSpacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                            OutlinedButton(
                                onClick = {
                                    val converted = LrcParser.fromPlainLines(plainDraft)
                                    engine.updateLines(converted, plainDraft)
                                    selectedLineIdx = 0
                                    editorMode = 2
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                SingleLineButtonLabel("SYNC TIMELINE", style = MaterialTheme.typography.caption)
                            }
                            Button(
                                onClick = { saveCurrentLyrics(returnToViewer = !startInEditor) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                SingleLineButtonLabel("SAVE LYRICS", style = MaterialTheme.typography.caption)
                            }
                        }
                    }
                }

                // 2: VISUAL LRC TIMELINE SYNC EDITOR
                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(color = ext.elevatedSurface, elevation = 2.dp) {
                            Column(modifier = Modifier.fillMaxWidth().padding(CadenceSpacing.md)) {
                                StorageModeRow(storageMode) { storageMode = it }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = CadenceSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = TextNormalizer.formatDuration(progress.positionMs),
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.h6.tabular(),
                                        color = MaterialTheme.colors.primary,
                                        maxLines = 1
                                    )
                                    Row {
                                        IconButton(onClick = { viewModel.container.playerConnection.seekBy(-5000L) }) {
                                            Icon(Icons.Default.Replay5, contentDescription = "Back 5s")
                                        }
                                        IconButton(onClick = {
                                            val t = track
                                            if (!isCurrentPlayingTrack && t != null) {
                                                viewModel.container.playerConnection.playTracks(listOf(t), 0)
                                            } else {
                                                viewModel.container.playerConnection.togglePlayPause()
                                            }
                                        }) {
                                            Icon(
                                                if (playback.isPlaying && isCurrentPlayingTrack) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play or Pause",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        }
                                        IconButton(onClick = { viewModel.container.playerConnection.seekBy(5000L) }) {
                                            Icon(Icons.Default.Forward5, contentDescription = "Forward 5s")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                                        OutlinedButton(
                                            onClick = { engine.shiftOffset(-100L) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            SingleLineButtonLabel("-0.1s", style = MaterialTheme.typography.caption.tabular())
                                        }
                                        OutlinedButton(
                                            onClick = { engine.shiftOffset(100L) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            SingleLineButtonLabel("+0.1s", style = MaterialTheme.typography.caption.tabular())
                                        }
                                    }
                                }
                                Spacer(Modifier.height(CadenceSpacing.xs))
                                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                                    Button(
                                        onClick = {
                                            if (lines.isNotEmpty()) {
                                                val idx = selectedLineIdx.coerceIn(0, lines.lastIndex)
                                                selectedLineIdx = engine.stampLine(idx, progress.positionMs)
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f).testTag("stamp_time_button")
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        SingleLineButtonLabel("STAMP #${selectedLineIdx + 1} & NEXT", style = MaterialTheme.typography.caption)
                                    }
                                    OutlinedButton(
                                        onClick = { selectedLineIdx = engine.addEmptyLine() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Line")
                                    }
                                    Button(
                                        onClick = { saveCurrentLyrics(returnToViewer = false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = "Save LRC")
                                    }
                                }
                            }
                        }

                        if (lines.isEmpty()) {
                            EmptyStateView(
                                title = "Timeline Is Empty",
                                subtitle = "Add lines manually or switch to Plain Text to paste lyrics, then tap STAMP while the song plays.",
                                actionLabel = "Add First Line",
                                onAction = { selectedLineIdx = engine.addEmptyLine() }
                            )
                        } else {
                            LazyColumn(
                                state = syncListState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(CadenceSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                itemsIndexed(lines, key = { idx, l -> "ed_${l.id}_$idx" }) { idx, line ->
                                    val isSelected = idx == selectedLineIdx
                                    val isLive = idx == activeLineIdx
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = when {
                                            isSelected -> MaterialTheme.colors.primary.copy(alpha = 0.14f)
                                            isLive -> MaterialTheme.colors.secondary.copy(alpha = 0.10f)
                                            else -> MaterialTheme.colors.surface
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colors.primary else ext.dividerColor
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedLineIdx = idx }
                                    ) {
                                        Column(modifier = Modifier.padding(CadenceSpacing.sm)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = line.timeMs?.let { LrcParser.formatTimestamp(it) } ?: "[--:--.--]",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    style = MaterialTheme.typography.caption.tabular(),
                                                    color = if (line.timeMs != null) MaterialTheme.colors.primary else ext.subtleText,
                                                    modifier = Modifier.clickable { engine.seekToLine(line, track) }
                                                )
                                                Spacer(Modifier.width(CadenceSpacing.sm))
                                                OutlinedTextField(
                                                    value = line.text,
                                                    onValueChange = { updatedText ->
                                                        val m = lines.toMutableList()
                                                        m[idx] = line.copy(text = updatedText)
                                                        engine.updateLines(m)
                                                    },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = CadenceSpacing.xs),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        val m = lines.toMutableList()
                                                        m[idx] = line.copy(timeMs = progress.positionMs)
                                                        engine.updateLines(m)
                                                        selectedLineIdx = idx
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { SingleLineButtonLabel("Stamp", style = MaterialTheme.typography.caption) }
                                                TextButton(
                                                    onClick = {
                                                        val m = lines.toMutableList()
                                                        m[idx] = line.copy(timeMs = ((line.timeMs ?: progress.positionMs) - 100L).coerceAtLeast(0L))
                                                        engine.updateLines(m)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { SingleLineButtonLabel("-100ms", style = MaterialTheme.typography.caption.tabular()) }
                                                TextButton(
                                                    onClick = {
                                                        val m = lines.toMutableList()
                                                        m[idx] = line.copy(timeMs = (line.timeMs ?: progress.positionMs) + 100L)
                                                        engine.updateLines(m)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { SingleLineButtonLabel("+100ms", style = MaterialTheme.typography.caption.tabular()) }
                                                TextButton(
                                                    onClick = {
                                                        val m = lines.toMutableList()
                                                        m[idx] = line.copy(timeMs = null)
                                                        engine.updateLines(m)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { SingleLineButtonLabel("Clear", style = MaterialTheme.typography.caption) }
                                                IconButton(onClick = {
                                                    val m = lines.toMutableList()
                                                    m.removeAt(idx)
                                                    engine.updateLines(m)
                                                    if (selectedLineIdx >= m.size) selectedLineIdx = (m.size - 1).coerceAtLeast(0)
                                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete line", modifier = Modifier.size(18.dp)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Share Bar when Share Selection Mode is active in Viewer
        if (editorMode == 0 && shareSelectionMode) {
            Surface(
                color = ext.elevatedSurface,
                elevation = 8.dp,
                border = BorderStroke(1.dp, ext.subtleBorder),
                shape = RoundedCornerShape(CadenceRadius.md),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(CadenceSpacing.md)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CadenceSpacing.md, vertical = CadenceSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                ) {
                    TextButton(
                        onClick = {
                            shareSelectionMode = false
                            selectedShareIndices = emptySet()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) { SingleLineButtonLabel("Cancel", style = MaterialTheme.typography.caption) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { shareLyricsAsText() },
                        enabled = selectedShareIndices.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        SingleLineButtonLabel("Text", style = MaterialTheme.typography.caption)
                    }
                    Button(
                        onClick = { showCardShareDialog = true },
                        enabled = selectedShareIndices.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        SingleLineButtonLabel("Share Card", style = MaterialTheme.typography.caption)
                    }
                }
            }
        } else if (editorMode == 0 && syncState.isSynced && (syncState.manualScrollPaused || !autoScroll)) {
            ExtendedFloatingActionButton(
                onClick = {
                    autoScroll = true
                    engine.resumeAutoScroll()
                    if (activeLineIdx >= 0) {
                        scope.launch { listState.animateScrollToItem((activeLineIdx - 2).coerceAtLeast(0)) }
                    }
                },
                icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                text = { SingleLineButtonLabel("Resume Sync") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(CadenceSpacing.lg)
                    .testTag("resume_lyrics_sync_fab")
            )
        }
    }
}

private data class ReaderPalette(
    val bgColor: Color,
    val gradient: Brush?,
    val textColor: Color,
    val subtleColor: Color,
    val accentColor: Color
)

@Composable
private fun StorageModeRow(selected: LyricsStorageMode, onSelect: (LyricsStorageMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs), modifier = Modifier.fillMaxWidth()) {
        LyricsStorageMode.entries.forEach { mode ->
            val isSel = selected == mode
            OutlinedButton(
                onClick = { onSelect(mode) },
                border = BorderStroke(1.dp, if (isSel) MaterialTheme.colors.primary else LocalCadenceTheme.current.dividerColor),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                SingleLineButtonLabel(
                    text = when (mode) {
                        LyricsStorageMode.APP_DATABASE -> "App DB"
                        LyricsStorageMode.EXTERNAL_LRC -> ".LRC File"
                        LyricsStorageMode.EMBEDDED_TAG -> "Embed Tag"
                    },
                    style = MaterialTheme.typography.caption,
                    color = if (isSel) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun renderAndSaveLyricCardPng(
    context: Context,
    track: TrackEntity,
    lines: List<String>,
    topColor: Int,
    bottomColor: Int,
    inkColor: Int,
    accentColor: Int
): Uri? {
    return runCatching {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                topColor, bottomColor, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative quote mark
        val quoteMarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 150f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("“", 96f, 210f, quoteMarkPaint)

        // Lyric quote body
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = if (lines.size >= 5) 48f else 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyText = lines.joinToString("\n")
        val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, bodyPaint, width - 192)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(14f, 1.15f)
            .setMaxLines(12)
            .build()

        val footerTop = height - 290f
        val bodyY = 260f.coerceAtMost((footerTop - bodyLayout.height) / 2f + 120f).coerceAtLeast(220f)
        canvas.save()
        canvas.translate(96f, bodyY)
        bodyLayout.draw(canvas)
        canvas.restore()

        // Divider hairline
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (inkColor and 0x00FFFFFF) or 0x33000000
            strokeWidth = 2f
        }
        canvas.drawLine(96f, footerTop - 36f, width - 96f, footerTop - 36f, linePaint)

        // Cover art thumbnail if available
        val coverBmp = loadTrackCoverForCard(context, track)
        val artSize = 132f
        val textStartX = if (coverBmp != null) {
            val destRect = RectF(96f, footerTop, 96f + artSize, footerTop + artSize)
            val scaled = Bitmap.createScaledBitmap(coverBmp, artSize.toInt(), artSize.toInt(), true)
            val clipPath = android.graphics.Path().apply {
                addRoundRect(destRect, 20f, 20f, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(scaled, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
            if (scaled != coverBmp) scaled.recycle()
            coverBmp.recycle()
            96f + artSize + 32f
        } else {
            96f
        }

        // Title & artist metadata
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (inkColor and 0x00FFFFFF) or 0xB3000000.toInt()
            textSize = 32f
            typeface = Typeface.DEFAULT
        }
        val metaWidth = (width - 96f - textStartX).toInt().coerceAtLeast(200)
        val titleLayout = StaticLayout.Builder.obtain(track.title, 0, track.title.length, titlePaint, metaWidth)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        val artistStr = if (track.album.isNotBlank()) "${track.artist} • ${track.album}" else track.artist
        val artistLayout = StaticLayout.Builder.obtain(artistStr, 0, artistStr.length, artistPaint, metaWidth)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.translate(textStartX, footerTop + 20f)
        titleLayout.draw(canvas)
        canvas.translate(0f, titleLayout.height + 12f)
        artistLayout.draw(canvas)
        canvas.restore()

        val shareDir = File(context.cacheDir, "shared_lyrics").apply { mkdirs() }
        val outFile = File(shareDir, "lyric_card.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            fos.flush()
        }
        bitmap.recycle()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }.getOrNull()
}

private fun loadTrackCoverForCard(context: Context, track: TrackEntity): Bitmap? {
    val opts = BitmapFactory.Options().apply {
        inSampleSize = 2
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    runCatching {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, Uri.parse(track.contentUri))
        val bytes = mmr.embeddedPicture
        mmr.release()
        if (bytes != null) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        }
    }
    if (!track.artworkUri.isNullOrBlank()) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(track.artworkUri))?.use { stream ->
                return BitmapFactory.decodeStream(stream, null, opts)
            }
        }
    }
    return null
}
