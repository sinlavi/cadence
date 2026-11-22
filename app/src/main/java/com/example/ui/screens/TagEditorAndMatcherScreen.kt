package com.example.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.model.EditableTrackTags
import com.example.core.model.MetadataCandidate
import com.example.core.util.TextNormalizer
import com.example.data.db.TrackEntity
import com.example.data.metadata.MetadataLookupResult
import com.example.data.tagger.BatchFieldSelection
import com.example.data.tagger.TagWriteOutcome
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.TrackArtwork
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditorScreen(
    trackIds: List<Long>,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val history by viewModel.tagEditHistory.collectAsState()
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    var tracks by remember(trackIds) { mutableStateOf<List<TrackEntity>>(emptyList()) }
    val isBatch = tracks.size > 1
    var initialTags by remember(trackIds) { mutableStateOf(EditableTrackTags()) }
    var tags by remember(trackIds) { mutableStateOf(EditableTrackTags()) }
    var batchSel by remember(trackIds) { mutableStateOf(BatchFieldSelection()) }
    var undoStack by remember(trackIds) { mutableStateOf<List<EditableTrackTags>>(emptyList()) }

    var artworkBytes by remember(trackIds) { mutableStateOf<ByteArray?>(null) }
    var artworkMime by remember(trackIds) { mutableStateOf("image/jpeg") }
    var artworkInfo by remember(trackIds) { mutableStateOf("Embedded or album cover") }
    var artworkModified by remember(trackIds) { mutableStateOf(false) }
    var removeArtwork by remember(trackIds) { mutableStateOf(false) }

    var showCoreSection by remember { mutableStateOf(true) }
    var showReleaseSection by remember { mutableStateOf(true) }
    var showCreditsSection by remember { mutableStateOf(false) }
    var showLyricsSection by remember { mutableStateOf(false) }

    var isLookingUp by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var lookupCandidates by remember { mutableStateOf<List<MetadataCandidate>>(emptyList()) }
    var lookupNotices by remember { mutableStateOf<List<String>>(emptyList()) }
    var showMatcherDialog by remember { mutableStateOf(false) }
    var pendingPreviewCandidate by remember { mutableStateOf<MetadataCandidate?>(null) }
    var applyMatchedArtwork by remember { mutableStateOf(true) }
    var matcherQuery by remember { mutableStateOf("") }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val hasUnsavedChanges = tracks.isNotEmpty() && (tags != initialTags || artworkModified || removeArtwork)

    fun attemptExit() {
        if (hasUnsavedChanges && !isSaving) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = hasUnsavedChanges && !isSaving) {
        showDiscardDialog = true
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isSaving = false
        if (result.resultCode == Activity.RESULT_OK) {
            val action = pendingPermissionAction
            pendingPermissionAction = null
            action?.invoke()
        } else {
            pendingPermissionAction = null
            viewModel.showMessage("Write permission declined by system.")
        }
    }

    // Zero-permission Android Photo Picker for album artwork
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    artworkBytes = bytes
                    artworkMime = mime
                    removeArtwork = false
                    artworkModified = true
                    artworkInfo = "Selected local image • ${opts.outWidth}×${opts.outHeight}px (${TextNormalizer.formatFileSize(bytes.size.toLong())})"
                }
            }
        }
    }

    val exportArtworkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null && artworkBytes != null) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(artworkBytes!!) }
                viewModel.showMessage("Exported full-resolution artwork.")
            }
        }
    }

    LaunchedEffect(trackIds) {
        val loaded = viewModel.container.database.trackDao().getTracksByIds(trackIds)
        tracks = loaded
        if (loaded.size == 1) {
            val read = viewModel.container.tagEditorEngine.readTagsFromTrack(loaded.first())
            if (read != null) {
                val t = loaded.first()
                val loadedTags = read.tags.copy(customTags = read.tags.customTags.ifBlank { t.customTags })
                initialTags = loadedTags
                tags = loadedTags
                val front = read.pictures.firstOrNull()
                if (front != null) {
                    artworkBytes = front.data
                    artworkMime = front.mimeType
                    artworkInfo = "${read.artworkWidth}×${read.artworkHeight}px • ${front.mimeType} • ${TextNormalizer.formatFileSize(read.artworkByteSize.toLong())}"
                } else {
                    artworkInfo = "No embedded artwork"
                }
            } else {
                val t = loaded.first()
                val fallback = EditableTrackTags(
                    title = t.title, artist = t.artist, album = t.album,
                    albumArtist = t.albumArtist, genre = t.genre,
                    year = t.year.takeIf { it > 0 }?.toString().orEmpty(),
                    customTags = t.customTags
                )
                initialTags = fallback
                tags = fallback
            }
        } else if (loaded.isNotEmpty()) {
            val first = loaded.first()
            val batchInitial = EditableTrackTags(
                artist = if (loaded.all { it.artist == first.artist }) first.artist else "",
                album = if (loaded.all { it.album == first.album }) first.album else "",
                albumArtist = if (loaded.all { it.albumArtist == first.albumArtist }) first.albumArtist else "",
                genre = if (loaded.all { it.genre == first.genre }) first.genre else "",
                year = if (loaded.all { it.year == first.year && it.year > 0 }) first.year.toString() else "",
                customTags = if (loaded.all { it.customTags == first.customTags }) first.customTags else ""
            )
            initialTags = batchInitial
            tags = batchInitial
            artworkInfo = "Batch mode (${loaded.size} tracks)"
        }
    }

    fun updateTags(next: EditableTrackTags) {
        undoStack = (undoStack + tags).takeLast(15)
        tags = next
    }

    fun runOnlineLookup(overrideQuery: String? = null) {
        val target = tracks.firstOrNull() ?: return
        if (overrideQuery == null && matcherQuery.isBlank()) {
            matcherQuery = listOf(target.artist, target.title)
                .filter { it.isNotBlank() && it != "Unknown Artist" }
                .joinToString(" ")
                .ifBlank { target.fileName }
        }
        isLookingUp = true
        scope.launch {
            val res = viewModel.container.metadataRepository.searchMetadata(target, settings, overrideQuery)
            isLookingUp = false
            when (res) {
                is MetadataLookupResult.Success -> {
                    lookupCandidates = res.candidates
                    lookupNotices = res.providerNotices
                    showMatcherDialog = true
                }
                is MetadataLookupResult.OfflineOrDisabled -> viewModel.showMessage(res.message)
            }
        }
    }

    fun executeSave() {
        if (tracks.isEmpty() || isSaving) return
        isSaving = true
        val artPayload = if (!removeArtwork && artworkModified) artworkBytes else null
        scope.launch {
            try {
                if (!isBatch) {
                    val t = tracks.first()
                    when (val outcome = viewModel.container.tagEditorEngine.writeSingleTrackTags(
                        track = t,
                        newTags = tags,
                        newArtworkBytes = artPayload,
                        newArtworkMime = artworkMime,
                        removeArtwork = removeArtwork
                    )) {
                        is TagWriteOutcome.Success -> {
                            viewModel.container.playerConnection.refreshCurrentTrackFromDb(syncMediaMetadata = true)
                            viewModel.showMessage(outcome.message)
                            onBack()
                        }
                        is TagWriteOutcome.NeedsUserPermission -> {
                            pendingPermissionAction = { executeSave() }
                            writePermissionLauncher.launch(IntentSenderRequest.Builder(outcome.pendingIntent.intentSender).build())
                        }
                        is TagWriteOutcome.Failure -> viewModel.showMessage(outcome.reason)
                    }
                } else {
                    val outcomes = viewModel.container.tagEditorEngine.writeBatchTrackTags(
                        tracks = tracks,
                        templateTags = tags,
                        selection = batchSel,
                        newArtworkBytes = artPayload,
                        newArtworkMime = artworkMime,
                        removeArtwork = removeArtwork
                    )
                    val okCount = outcomes.count { it is TagWriteOutcome.Success }
                    val fail = outcomes.filterIsInstance<TagWriteOutcome.Failure>()
                    val perm = outcomes.filterIsInstance<TagWriteOutcome.NeedsUserPermission>().firstOrNull()
                    if (perm != null) {
                        pendingPermissionAction = { executeSave() }
                        writePermissionLauncher.launch(IntentSenderRequest.Builder(perm.pendingIntent.intentSender).build())
                    } else {
                        if (okCount > 0) {
                            viewModel.container.playerConnection.refreshCurrentTrackFromDb(syncMediaMetadata = true)
                        }
                        viewModel.showMessage("Updated $okCount/${tracks.size} tracks.${if (fail.isNotEmpty()) " ${fail.first().reason}" else ""}")
                        if (okCount > 0) onBack()
                    }
                }
            } finally {
                isSaving = false
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have unsaved tag or artwork edits. Leave without writing changes to file?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) { SingleLineButtonLabel("DISCARD", color = MaterialTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { SingleLineButtonLabel("KEEP EDITING") }
            }
        )
    }

    if (pendingPreviewCandidate != null) {
        val cand = pendingPreviewCandidate!!
        AlertDialog(
            onDismissRequest = { pendingPreviewCandidate = null },
            title = { Text("Apply Metadata Match?") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                ) {
                    Text(
                        "${cand.provider.displayName} • ${(cand.confidenceScore * 100).toInt()}% confidence",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary
                    )
                    Divider(color = ext.dividerColor)
                    if (!isBatch) TagDiffRow("Title", tags.title, cand.title)
                    TagDiffRow("Artist", tags.artist, cand.artist)
                    TagDiffRow("Album", tags.album, cand.album.orEmpty())
                    TagDiffRow("Album Artist", tags.albumArtist, cand.albumArtist.orEmpty())
                    TagDiffRow("Genre", tags.genre, cand.genre.orEmpty())
                    TagDiffRow("Year", tags.year, cand.year?.toString().orEmpty())
                    if (!isBatch) TagDiffRow("Track #", tags.trackNumber, cand.trackNumber?.toString().orEmpty())
                    TagDiffRow("Label", tags.recordLabel, cand.recordLabel.orEmpty())
                    if (!cand.isrc.isNullOrBlank()) TagDiffRow("ISRC", tags.isrc, cand.isrc)

                    if (cand.artworkUrl != null) {
                        Divider(color = ext.dividerColor)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = applyMatchedArtwork, onCheckedChange = { applyMatchedArtwork = it })
                            Text("Download & replace album artwork", style = MaterialTheme.typography.body2)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val chosen = cand
                    pendingPreviewCandidate = null
                    showMatcherDialog = false
                    scope.launch {
                        updateTags(
                            tags.copy(
                                title = if (!isBatch && chosen.title.isNotBlank()) chosen.title else tags.title,
                                artist = chosen.artist.ifBlank { tags.artist },
                                album = chosen.album ?: tags.album,
                                albumArtist = chosen.albumArtist ?: tags.albumArtist,
                                genre = chosen.genre ?: tags.genre,
                                year = chosen.year?.toString() ?: tags.year,
                                releaseDate = chosen.releaseDate ?: tags.releaseDate,
                                trackNumber = if (!isBatch) chosen.trackNumber?.toString() ?: tags.trackNumber else tags.trackNumber,
                                totalTracks = chosen.totalTracks?.toString() ?: tags.totalTracks,
                                discNumber = if (!isBatch) chosen.discNumber?.toString() ?: tags.discNumber else tags.discNumber,
                                recordLabel = chosen.recordLabel ?: tags.recordLabel,
                                isrc = chosen.isrc ?: tags.isrc
                            )
                        )
                        if (isBatch) {
                            batchSel = batchSel.copy(
                                artist = true,
                                album = !chosen.album.isNullOrBlank(),
                                albumArtist = !chosen.albumArtist.isNullOrBlank(),
                                genre = !chosen.genre.isNullOrBlank(),
                                year = chosen.year != null
                            )
                        }
                        if (applyMatchedArtwork && chosen.artworkUrl != null) {
                            val bytes = viewModel.container.metadataRepository.downloadFullResArtworkBytes(chosen.artworkUrl)
                            if (bytes != null) {
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                                artworkBytes = bytes
                                artworkMime = "image/jpeg"
                                removeArtwork = false
                                artworkModified = true
                                artworkInfo = "From ${chosen.provider.displayName} • ${opts.outWidth}×${opts.outHeight}px (Full Res)"
                            }
                        }
                        viewModel.showMessage("Applied metadata preview from ${chosen.provider.displayName}.")
                    }
                }) { SingleLineButtonLabel("APPLY MATCH") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPreviewCandidate = null }) { SingleLineButtonLabel("CANCEL") }
            }
        )
    }

    if (showMatcherDialog) {
        AlertDialog(
            onDismissRequest = { showMatcherDialog = false },
            title = { Text("Online Metadata & Artwork Match") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = matcherQuery,
                            onValueChange = { matcherQuery = it },
                            label = { Text("Search iTunes US, Deezer, MusicBrainz") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(CadenceSpacing.xs))
                        Button(
                            onClick = { runOnlineLookup(matcherQuery) },
                            enabled = !isLookingUp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) { SingleLineButtonLabel(if (isLookingUp) "..." else "Go") }
                    }
                    lookupNotices.forEach { n ->
                        Text(n, style = MaterialTheme.typography.caption, color = ext.subtleText, modifier = Modifier.padding(top = CadenceSpacing.xs))
                    }
                    Spacer(Modifier.height(CadenceSpacing.sm))
                    if (lookupCandidates.isEmpty()) {
                        Text("No matches found from enabled providers.", style = MaterialTheme.typography.body2)
                    } else {
                        lookupCandidates.forEach { cand ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        applyMatchedArtwork = cand.artworkUrl != null
                                        pendingPreviewCandidate = cand
                                    }
                                    .padding(vertical = CadenceSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cand.artworkUrl != null) {
                                    AsyncImage(
                                        model = cand.artworkUrl,
                                        contentDescription = cand.album,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(CadenceSpacing.md))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cand.title, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${cand.artist} • ${cand.album ?: ""}", style = MaterialTheme.typography.caption, color = ext.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${cand.provider.displayName} • ${(cand.confidenceScore * 100).toInt()}% match", style = MaterialTheme.typography.overline.tabular(), color = MaterialTheme.colors.primary, maxLines = 1)
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMatcherDialog = false }) { SingleLineButtonLabel("CLOSE") } }
        )
    }

    if (showTemplateDialog) {
        FilenameTemplateDialog(
            tracks = tracks,
            viewModel = viewModel,
            onApplyParsedToSingle = { parsedMap ->
                updateTags(
                    tags.copy(
                        artist = parsedMap["artist"] ?: tags.artist,
                        title = parsedMap["title"] ?: tags.title,
                        album = parsedMap["album"] ?: tags.album,
                        trackNumber = parsedMap["track"] ?: tags.trackNumber,
                        year = parsedMap["year"] ?: tags.year
                    )
                )
            },
            onDismiss = { showTemplateDialog = false }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Tag Edit History & Rollback") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (history.isEmpty()) {
                        Text("No tag modifications recorded yet.", style = MaterialTheme.typography.body2)
                    } else {
                        history.take(25).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = CadenceSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Track #${entry.trackId}", style = MaterialTheme.typography.subtitle2)
                                    Text(if (entry.rolledBack) "Rolled back" else "Editable rollback snapshot", style = MaterialTheme.typography.caption, color = ext.subtleText)
                                }
                                if (!entry.rolledBack) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            when (val r = viewModel.container.tagEditorEngine.rollbackTagEdit(entry)) {
                                                is TagWriteOutcome.Success -> {
                                                    viewModel.container.playerConnection.refreshCurrentTrackFromDb(syncMediaMetadata = true)
                                                    viewModel.showMessage("Rolled back tags.")
                                                }
                                                is TagWriteOutcome.Failure -> viewModel.showMessage(r.reason)
                                                else -> Unit
                                            }
                                        }
                                    }) { SingleLineButtonLabel("ROLLBACK") }
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistoryDialog = false }) { SingleLineButtonLabel("CLOSE") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("tag_editor_screen")
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(if (isBatch) "Batch Tag Editor (${tracks.size})" else "Tag Editor", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (hasUnsavedChanges) {
                        Text("Unsaved edits", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { attemptExit() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(
                    onClick = {
                        if (undoStack.isNotEmpty()) {
                            tags = undoStack.last()
                            undoStack = undoStack.dropLast(1)
                        }
                    },
                    enabled = undoStack.isNotEmpty()
                ) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Edit") }
                IconButton(onClick = { showTemplateDialog = true }) {
                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Filename Templates")
                }
                IconButton(onClick = { showHistoryDialog = true }) {
                    Icon(Icons.Default.History, contentDescription = "Edit History")
                }
                if (isLookingUp) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp).padding(4.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { runOnlineLookup() }, modifier = Modifier.testTag("tag_auto_match_btn")) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Auto-Match Online Metadata")
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Section 1: Artwork Studio (full-width elevated tonal band, zero body card)
            Surface(
                color = ext.elevatedSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md)) {
                    Text("ARTWORK STUDIO", style = MaterialTheme.typography.overline, color = MaterialTheme.colors.primary)
                    Spacer(Modifier.height(CadenceSpacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val bmp = remember(artworkBytes) {
                            artworkBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        }
                        if (bmp != null && !removeArtwork) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Embedded Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                        } else {
                            val firstTrack = tracks.firstOrNull()
                            TrackArtwork(
                                if (removeArtwork) null else firstTrack?.artworkUri,
                                if (removeArtwork) null else firstTrack?.contentUri,
                                "Cover",
                                Modifier.size(72.dp)
                            )
                        }
                        Spacer(Modifier.width(CadenceSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Album Artwork", style = MaterialTheme.typography.subtitle1)
                            Text(artworkInfo, style = MaterialTheme.typography.caption, color = ext.subtleText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(CadenceSpacing.xs),
                                modifier = Modifier.padding(top = CadenceSpacing.xs)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    SingleLineButtonLabel("Pick Cover", style = MaterialTheme.typography.caption)
                                }
                                if (artworkBytes != null && !removeArtwork) {
                                    OutlinedButton(
                                        onClick = {
                                            val name = TextNormalizer.sanitizeFilename("${tags.album.ifBlank { "cover" }}.jpg")
                                            exportArtworkLauncher.launch(name)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) { SingleLineButtonLabel("Export", style = MaterialTheme.typography.caption) }
                                }
                                IconButton(onClick = {
                                    removeArtwork = true
                                    artworkModified = true
                                    artworkBytes = null
                                    artworkInfo = "Artwork will be removed on save"
                                }) { Icon(Icons.Default.Delete, contentDescription = "Remove Artwork") }
                            }
                        }
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
                // Quick Normalization Utilities
                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            updateTags(
                                tags.copy(
                                    title = TextNormalizer.toTitleCase(tags.title),
                                    artist = TextNormalizer.toTitleCase(tags.artist),
                                    album = TextNormalizer.toTitleCase(tags.album),
                                    albumArtist = TextNormalizer.toTitleCase(tags.albumArtist),
                                    genre = TextNormalizer.toTitleCase(tags.genre)
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        SingleLineButtonLabel("Title Case", style = MaterialTheme.typography.caption)
                    }
                    OutlinedButton(
                        onClick = {
                            updateTags(tags.copy(albumArtist = tags.artist))
                            if (isBatch) batchSel = batchSel.copy(albumArtist = true)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) { SingleLineButtonLabel("Artist -> Album Artist", style = MaterialTheme.typography.caption) }
                }

                if (isBatch) {
                    Surface(
                        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(CadenceSpacing.md)) {
                            Text("Batch Options", style = MaterialTheme.typography.subtitle2, color = MaterialTheme.colors.primary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = batchSel.autoIncrementTrackNumber,
                                    onCheckedChange = { batchSel = batchSel.copy(autoIncrementTrackNumber = it) }
                                )
                                Text("Auto-increment track numbers (1..${tracks.size})", style = MaterialTheme.typography.body2)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = batchSel.setTotalTracksToBatchSize,
                                    onCheckedChange = { batchSel = batchSel.copy(setTotalTracksToBatchSize = it) }
                                )
                                Text("Set total tracks to ${tracks.size}", style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                }

                // Section 2: Core Info & Numbering
                SectionToggleHeader("Core Track Metadata", showCoreSection) { showCoreSection = !showCoreSection }
                if (showCoreSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                        if (!isBatch) {
                            TagFieldRow("Title", tags.title, false, false, onCheck = {}) { updateTags(tags.copy(title = it)) }
                        }
                        TagFieldRow("Artist", tags.artist, isBatch, batchSel.artist, { batchSel = batchSel.copy(artist = it) }) {
                            updateTags(tags.copy(artist = it))
                            if (isBatch) batchSel = batchSel.copy(artist = true)
                        }
                        TagFieldRow("Album", tags.album, isBatch, batchSel.album, { batchSel = batchSel.copy(album = it) }) {
                            updateTags(tags.copy(album = it))
                            if (isBatch) batchSel = batchSel.copy(album = true)
                        }
                        TagFieldRow("Album Artist", tags.albumArtist, isBatch, batchSel.albumArtist, { batchSel = batchSel.copy(albumArtist = it) }) {
                            updateTags(tags.copy(albumArtist = it))
                            if (isBatch) batchSel = batchSel.copy(albumArtist = true)
                        }
                        TagFieldRow("Genre", tags.genre, isBatch, batchSel.genre, { batchSel = batchSel.copy(genre = it) }) {
                            updateTags(tags.copy(genre = it))
                            if (isBatch) batchSel = batchSel.copy(genre = true)
                        }
                        TagFieldRow(
                            label = "Year / Release Date",
                            value = tags.releaseDate.ifBlank { tags.year },
                            showCheckbox = isBatch,
                            checked = batchSel.year,
                            onCheck = { batchSel = batchSel.copy(year = it) },
                            keyboardType = KeyboardType.Number
                        ) {
                            updateTags(tags.copy(year = it.take(4), releaseDate = it))
                            if (isBatch) batchSel = batchSel.copy(year = true)
                        }

                        if (!isBatch) {
                            Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                                OutlinedTextField(
                                    value = tags.trackNumber,
                                    onValueChange = { updateTags(tags.copy(trackNumber = it)) },
                                    label = { Text("Track #") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = tags.totalTracks,
                                    onValueChange = { updateTags(tags.copy(totalTracks = it)) },
                                    label = { Text("Total") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = tags.discNumber,
                                    onValueChange = { updateTags(tags.copy(discNumber = it)) },
                                    label = { Text("Disc #") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = tags.totalDiscs,
                                    onValueChange = { updateTags(tags.copy(totalDiscs = it)) },
                                    label = { Text("Total") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Divider(color = ext.dividerColor)

                // Section 3: Release & Publishing
                SectionToggleHeader("Release & Publishing", showReleaseSection) { showReleaseSection = !showReleaseSection }
                if (showReleaseSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                        TagFieldRow("Record Label", tags.recordLabel, isBatch, batchSel.recordLabel, { batchSel = batchSel.copy(recordLabel = it) }) {
                            updateTags(tags.copy(recordLabel = it))
                            if (isBatch) batchSel = batchSel.copy(recordLabel = true)
                        }
                        TagFieldRow("Publisher", tags.publisher, isBatch, batchSel.publisher, { batchSel = batchSel.copy(publisher = it) }) {
                            updateTags(tags.copy(publisher = it))
                            if (isBatch) batchSel = batchSel.copy(publisher = true)
                        }
                        TagFieldRow("Copyright", tags.copyright, isBatch, batchSel.copyright, { batchSel = batchSel.copy(copyright = it) }) {
                            updateTags(tags.copy(copyright = it))
                            if (isBatch) batchSel = batchSel.copy(copyright = true)
                        }
                        if (!isBatch) {
                            OutlinedTextField(
                                value = tags.originalReleaseDate,
                                onValueChange = { updateTags(tags.copy(originalReleaseDate = it)) },
                                label = { Text("Original Release Date") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = tags.isCompilation,
                                onCheckedChange = {
                                    updateTags(tags.copy(isCompilation = it))
                                    if (isBatch) batchSel = batchSel.copy(isCompilation = true)
                                }
                            )
                            Spacer(Modifier.width(CadenceSpacing.sm))
                            Text("Part of Compilation Album", style = MaterialTheme.typography.body2)
                        }
                    }
                }

                Divider(color = ext.dividerColor)

                // Section 4: Credits, Musical & User Tags
                SectionToggleHeader("Credits, Musical & User Tags", showCreditsSection) { showCreditsSection = !showCreditsSection }
                if (showCreditsSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                        TagFieldRow("Composer", tags.composer, isBatch, batchSel.composer, { batchSel = batchSel.copy(composer = it) }) {
                            updateTags(tags.copy(composer = it))
                            if (isBatch) batchSel = batchSel.copy(composer = true)
                        }
                        TagFieldRow("Conductor", tags.conductor, isBatch, batchSel.conductor, { batchSel = batchSel.copy(conductor = it) }) {
                            updateTags(tags.copy(conductor = it))
                            if (isBatch) batchSel = batchSel.copy(conductor = true)
                        }
                        TagFieldRow(
                            label = "BPM",
                            value = tags.bpm,
                            showCheckbox = isBatch,
                            checked = batchSel.bpm,
                            onCheck = { batchSel = batchSel.copy(bpm = it) },
                            keyboardType = KeyboardType.Number
                        ) {
                            updateTags(tags.copy(bpm = it))
                            if (isBatch) batchSel = batchSel.copy(bpm = true)
                        }
                        if (!isBatch) {
                            Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                                OutlinedTextField(tags.musicalKey, { updateTags(tags.copy(musicalKey = it)) }, label = { Text("Musical Key") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(tags.isrc, { updateTags(tags.copy(isrc = it)) }, label = { Text("ISRC") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                                OutlinedTextField(tags.subtitle, { updateTags(tags.copy(subtitle = it)) }, label = { Text("Subtitle") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(tags.encoder, { updateTags(tags.copy(encoder = it)) }, label = { Text("Encoder") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                        }
                        TagFieldRow("Grouping", tags.grouping, isBatch, batchSel.grouping, { batchSel = batchSel.copy(grouping = it) }) {
                            updateTags(tags.copy(grouping = it))
                            if (isBatch) batchSel = batchSel.copy(grouping = true)
                        }
                        TagFieldRow("Language", tags.language, isBatch, batchSel.language, { batchSel = batchSel.copy(language = it) }) {
                            updateTags(tags.copy(language = it))
                            if (isBatch) batchSel = batchSel.copy(language = true)
                        }
                        TagFieldRow("User-Defined Tags (Smart Rules)", tags.customTags, isBatch, batchSel.customTags, { batchSel = batchSel.copy(customTags = it) }) {
                            updateTags(tags.copy(customTags = it))
                            if (isBatch) batchSel = batchSel.copy(customTags = true)
                        }
                        TagFieldRow("Comment", tags.comment, isBatch, batchSel.comment, { batchSel = batchSel.copy(comment = it) }) {
                            updateTags(tags.copy(comment = it))
                            if (isBatch) batchSel = batchSel.copy(comment = true)
                        }
                    }
                }

                Divider(color = ext.dividerColor)

                // Section 5: Embedded Lyrics
                SectionToggleHeader("Embedded Lyrics Tag", showLyricsSection) { showLyricsSection = !showLyricsSection }
                if (showLyricsSection) {
                    OutlinedTextField(
                        value = tags.lyrics,
                        onValueChange = { updateTags(tags.copy(lyrics = it)) },
                        label = { Text("Embedded Lyrics (Plain or LRC)") },
                        minLines = 4,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Divider(color = ext.dividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = hPad, vertical = CadenceSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
        ) {
            OutlinedButton(
                onClick = { attemptExit() },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) { SingleLineButtonLabel("CANCEL") }
            Button(
                onClick = { executeSave() },
                enabled = !isSaving && tracks.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f).testTag("tag_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colors.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    SingleLineButtonLabel("SAVE TO FILE")
                }
            }
        }
    }
}

@Composable
private fun SectionToggleHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = CadenceSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TagDiffRow(field: String, current: String, proposed: String) {
    val ext = LocalCadenceTheme.current
    if (proposed.isBlank() && current.isBlank()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(field, style = MaterialTheme.typography.overline, color = ext.subtleText)
        Text(
            text = current.ifBlank { "(empty)" },
            style = MaterialTheme.typography.caption,
            color = ext.subtleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "-> ${proposed.ifBlank { "(unchanged)" }}",
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium,
            color = if (proposed.isNotBlank() && proposed != current) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TagFieldRow(
    label: String,
    value: String,
    showCheckbox: Boolean,
    checked: Boolean,
    onCheck: (Boolean) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (showCheckbox) {
            Checkbox(checked = checked, onCheckedChange = onCheck)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilenameTemplateDialog(
    tracks: List<TrackEntity>,
    viewModel: MainViewModel,
    onApplyParsedToSingle: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var template by remember { mutableStateOf("%artist% - %title%") }
    val first = tracks.firstOrNull()
    val renamePreview = first?.let { viewModel.container.tagEditorEngine.formatFilenameFromTemplate(it, template) }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filename <-> Tag Templates") },
        text = {
            Column {
                OutlinedTextField(
                    value = template,
                    onValueChange = { template = it },
                    label = { Text("Pattern (%artist%, %title%, %album%, %track%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(CadenceSpacing.sm))
                Text("Presets:", style = MaterialTheme.typography.caption)
                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                    TextButton(onClick = { template = "%artist% - %title%" }) { SingleLineButtonLabel("Artist - Title") }
                    TextButton(onClick = { template = "%track% - %title%" }) { SingleLineButtonLabel("Track - Title") }
                }
                if (first != null) {
                    Spacer(Modifier.height(CadenceSpacing.xs))
                    Text("Source: ${first.fileName}", style = MaterialTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Preview: $renamePreview", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)) {
                if (tracks.size == 1 && first != null) {
                    TextButton(onClick = {
                        val parsed = viewModel.container.tagEditorEngine.parseTagsFromFilename(first.fileName, template)
                        if (parsed.isNotEmpty()) {
                            onApplyParsedToSingle(parsed)
                            viewModel.showMessage("Parsed tags from filename.")
                            onDismiss()
                        } else {
                            viewModel.showMessage("Filename did not match template.")
                        }
                    }) { SingleLineButtonLabel("TO TAGS") }
                }
                Button(onClick = {
                    scope.launch {
                        var renamed = 0
                        for (t in tracks) {
                            val newName = viewModel.container.tagEditorEngine.formatFilenameFromTemplate(t, template)
                            val res = viewModel.container.tagEditorEngine.renameTrackFile(t, newName)
                            if (res.isSuccess) renamed++
                        }
                        viewModel.showMessage("Renamed $renamed/${tracks.size} file(s).")
                        onDismiss()
                    }
                }) { SingleLineButtonLabel("RENAME") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { SingleLineButtonLabel("CLOSE") } }
    )
}
