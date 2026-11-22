package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.SmartCombinator
import com.example.core.model.SmartCondition
import com.example.core.model.SmartField
import com.example.core.model.SmartFieldKind
import com.example.core.model.SmartOperator
import com.example.core.model.SmartPlaylistDefinition
import com.example.core.model.SmartRuleGroup
import com.example.core.model.SmartSortSpec
import com.example.core.model.SortDirection
import com.example.core.model.SortField
import com.example.core.util.TextNormalizer
import com.example.data.db.PlaylistWithCount
import com.example.data.db.SmartPlaylistEntity
import com.example.data.db.TrackEntity
import com.example.data.smart.SmartPlaylistEvaluator
import com.example.ui.components.CadenceInlineSearchBar
import com.example.ui.components.CollectionBatchActionBar
import com.example.ui.components.CollectionListRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.TrackArtwork
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun PlaylistsScreen(
    viewModel: MainViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSmartPlaylist: (Long) -> Unit,
    onPlaylistMore: (PlaylistWithCount) -> Unit,
    onSmartPlaylistMore: (SmartPlaylistEntity) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val smartPlaylists by viewModel.smartPlaylists.collectAsState()
    val scope = rememberCoroutineScope()
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var playlistSearch by rememberSaveable(selectedTab) { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showDeleteBatchConfirm by remember { mutableStateOf(false) }
    var creatingSmartPlaylist by remember { mutableStateOf(false) }
    var selectedPlaylistIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val normSearch = remember(playlistSearch) { TextNormalizer.normalizeForSearch(playlistSearch.trim()) }
    val filteredPlaylists = remember(playlists, normSearch) {
        if (normSearch.isBlank()) playlists else playlists.filter {
            TextNormalizer.normalizeForSearch("${it.name} ${it.description}").contains(normSearch)
        }
    }
    val filteredSmartPlaylists = remember(smartPlaylists, normSearch) {
        if (normSearch.isBlank()) smartPlaylists else smartPlaylists.filter {
            TextNormalizer.normalizeForSearch("${it.name} ${it.description}").contains(normSearch)
        }
    }

    BackHandler(enabled = selectedPlaylistIds.isNotEmpty()) {
        selectedPlaylistIds = emptySet()
    }

    val m3uImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val baseName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Imported M3U" }
                    ?: "Imported M3U"
                viewModel.container.backupManager.importM3uPlaylist(baseName, uri)
                    .onSuccess { (matched, missing) ->
                        viewModel.showMessage("Imported \"$baseName\": $matched matched, $missing missing.")
                    }
                    .onFailure {
                        viewModel.showMessage("M3U import failed: ${it.localizedMessage}")
                    }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_playlist_name_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createPlaylist(name, desc)
                        showCreatePlaylistDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { SingleLineButtonLabel("CREATE") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { SingleLineButtonLabel("CANCEL") }
            }
        )
    }

    if (showDeleteBatchConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteBatchConfirm = false },
            title = { Text("Delete ${selectedPlaylistIds.size} Playlist(s)?") },
            text = { Text("Remove selected playlists? Audio files on your device will not be affected.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deletePlaylists(selectedPlaylistIds)
                    selectedPlaylistIds = emptySet()
                    showDeleteBatchConfirm = false
                }) { SingleLineButtonLabel("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBatchConfirm = false }) { SingleLineButtonLabel("CANCEL") }
            }
        )
    }

    if (showMergeDialog && playlists.size >= 2) {
        var sourceId by remember { mutableStateOf(playlists[0].id) }
        var targetId by remember { mutableStateOf(playlists[1].id) }
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Merge Playlists") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Source (copy tracks from):", style = MaterialTheme.typography.subtitle2)
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sourceId = pl.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (sourceId == pl.id) "● ${pl.name}" else "○ ${pl.name}",
                                color = if (sourceId == pl.id) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Target (append into):", style = MaterialTheme.typography.subtitle2)
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetId = pl.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (targetId == pl.id) "● ${pl.name}" else "○ ${pl.name}",
                                color = if (targetId == pl.id) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.mergePlaylists(sourceId, targetId)
                        showMergeDialog = false
                    },
                    enabled = sourceId != targetId
                ) { SingleLineButtonLabel("MERGE") }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) { SingleLineButtonLabel("CANCEL") }
            }
        )
    }

    if (creatingSmartPlaylist) {
        SmartRuleEditorScreen(
            initial = null,
            viewModel = viewModel,
            onSave = { entity ->
                viewModel.saveSmartPlaylist(entity)
                creatingSmartPlaylist = false
            },
            onDismiss = { creatingSmartPlaylist = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlists_screen")
    ) {
        if (selectedPlaylistIds.isNotEmpty()) {
            CollectionBatchActionBar(
                selectedCount = selectedPlaylistIds.size,
                onCancel = { selectedPlaylistIds = emptySet() },
                onSelectAll = { selectedPlaylistIds = filteredPlaylists.map { it.id }.toSet() },
                onPlayNext = {
                    scope.launch {
                        val tracks = withContext(Dispatchers.IO) {
                            selectedPlaylistIds.flatMap { id ->
                                viewModel.container.database.playlistDao().getPlaylistTracksOnce(id)
                            }.distinctBy { it.id }
                        }
                        if (tracks.isNotEmpty()) {
                            viewModel.container.playerConnection.addPlayNext(tracks)
                            viewModel.showMessage("Queued ${tracks.size} track(s) next.")
                        }
                        selectedPlaylistIds = emptySet()
                    }
                },
                onAddToQueue = {
                    scope.launch {
                        val tracks = withContext(Dispatchers.IO) {
                            selectedPlaylistIds.flatMap { id ->
                                viewModel.container.database.playlistDao().getPlaylistTracksOnce(id)
                            }.distinctBy { it.id }
                        }
                        if (tracks.isNotEmpty()) {
                            viewModel.container.playerConnection.addToEnd(tracks)
                            viewModel.showMessage("Added ${tracks.size} track(s) to queue.")
                        }
                        selectedPlaylistIds = emptySet()
                    }
                },
                onDelete = { showDeleteBatchConfirm = true }
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; selectedPlaylistIds = emptySet(); playlistSearch = "" },
                text = { SingleLineButtonLabel("Playlists (${playlists.size})", style = MaterialTheme.typography.button) },
                modifier = Modifier.testTag("tab_standard_playlists")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; selectedPlaylistIds = emptySet(); playlistSearch = "" },
                text = { SingleLineButtonLabel("Smart Rules (${smartPlaylists.size})", style = MaterialTheme.typography.button) },
                modifier = Modifier.testTag("tab_smart_playlists")
            )
        }

        CadenceInlineSearchBar(
            query = playlistSearch,
            onQueryChange = { playlistSearch = it },
            placeholder = if (selectedTab == 0) "Filter playlists..." else "Filter smart rule sets..."
        )

        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ext.elevatedSurface)
                    .padding(horizontal = hPad, vertical = CadenceSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
            ) {
                Button(
                    onClick = { showCreatePlaylistDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).testTag("create_playlist_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    SingleLineButtonLabel("NEW")
                }
                OutlinedButton(
                    onClick = {
                        m3uImportLauncher.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "*/*"))
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    SingleLineButtonLabel(if (isCompact) "IMPORT" else "IMPORT M3U")
                }
                if (playlists.size >= 2) {
                    OutlinedButton(
                        onClick = {
                            if (selectedPlaylistIds.size == 2) {
                                val pair = selectedPlaylistIds.toList()
                                viewModel.mergePlaylists(pair[0], pair[1])
                                selectedPlaylistIds = emptySet()
                            } else {
                                showMergeDialog = true
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = "Merge Playlists", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Divider(color = ext.dividerColor)

            if (filteredPlaylists.isEmpty()) {
                EmptyStateView(
                    title = if (playlistSearch.isNotBlank()) "No Matching Playlists" else "No Playlists Yet",
                    subtitle = if (playlistSearch.isNotBlank()) "Try another search keyword." else "Create a custom playlist or import an existing .m3u / .m3u8 file.",
                    actionLabel = if (playlistSearch.isNotBlank()) "Clear Search" else "Create Playlist",
                    onAction = {
                        if (playlistSearch.isNotBlank()) playlistSearch = "" else showCreatePlaylistDialog = true
                    },
                    icon = Icons.Default.PlaylistPlay
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredPlaylists, key = { "pl_${it.id}" }) { pl ->
                        val isSelected = pl.id in selectedPlaylistIds
                        CollectionListRow(
                            title = pl.name,
                            subtitle = "${pl.trackCount} tracks • ${TextNormalizer.formatDuration(pl.totalDurationMs)}",
                            leadingIcon = Icons.Default.PlaylistPlay,
                            isSelected = isSelected,
                            selectionMode = selectedPlaylistIds.isNotEmpty(),
                            onClick = {
                                if (selectedPlaylistIds.isNotEmpty()) {
                                    selectedPlaylistIds = if (isSelected) selectedPlaylistIds - pl.id else selectedPlaylistIds + pl.id
                                } else {
                                    onOpenPlaylist(pl.id)
                                }
                            },
                            onLongClick = {
                                selectedPlaylistIds = if (isSelected) selectedPlaylistIds - pl.id else selectedPlaylistIds + pl.id
                            },
                            onMoreClick = { onPlaylistMore(pl) },
                            modifier = Modifier.testTag("playlist_card_${pl.id}")
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ext.elevatedSurface)
                    .padding(horizontal = hPad, vertical = CadenceSpacing.sm)
            ) {
                Button(
                    onClick = { creatingSmartPlaylist = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("create_smart_playlist_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    SingleLineButtonLabel(if (isCompact) "NEW SMART RULES" else "CREATE SMART PLAYLIST")
                }
            }
            Divider(color = ext.dividerColor)
            if (filteredSmartPlaylists.isEmpty()) {
                EmptyStateView(
                    title = "No Smart Rules Match",
                    subtitle = "Clear your filter or create a new rule group.",
                    icon = Icons.Default.AutoAwesome
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredSmartPlaylists, key = { "sp_${it.id}" }) { sp ->
                        CollectionListRow(
                            title = sp.name,
                            subtitle = sp.description.ifBlank { "Dynamic smart rule set" },
                            trailingMeta = if (sp.isBuiltIn) "Preset" else null,
                            leadingIcon = Icons.Default.AutoAwesome,
                            onClick = { onOpenSmartPlaylist(sp.id) },
                            onLongClick = { onSmartPlaylistMore(sp) },
                            onMoreClick = { onSmartPlaylistMore(sp) },
                            modifier = Modifier.testTag("smart_playlist_card_${sp.id}")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SmartRuleEditorScreen(
    initial: SmartPlaylistEntity?,
    viewModel: MainViewModel,
    onSave: (SmartPlaylistEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    val defaultDefinition = remember(initial) {
        if (initial != null) {
            SmartPlaylistEvaluator.parseDefinition(initial.definitionJson)
        } else {
            SmartPlaylistDefinition(
                rootGroup = SmartRuleGroup(
                    id = UUID.randomUUID().toString(),
                    combinator = SmartCombinator.ALL,
                    conditions = listOf(
                        SmartCondition(
                            id = UUID.randomUUID().toString(),
                            field = SmartField.PLAY_COUNT,
                            operator = SmartOperator.GREATER_THAN,
                            value = "5"
                        )
                    )
                )
            )
        }
    }

    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var definition by remember(initial) { mutableStateOf(defaultDefinition) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showPreviewTracks by remember { mutableStateOf(false) }

    val hasUnsavedChanges = remember(name, description, definition, initial, defaultDefinition) {
        name != (initial?.name ?: "") ||
            description != (initial?.description ?: "") ||
            definition != defaultDefinition
    }

    fun attemptDismiss() {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    var matchedTracks by remember { mutableStateOf<List<TrackEntity>>(emptyList()) }
    LaunchedEffect(definition) {
        matchedTracks = withContext(Dispatchers.IO) {
            viewModel.container.database.trackDao()
                .queryTracksRaw(SmartPlaylistEvaluator.buildSmartPlaylistQuery(definition))
        }
    }

    val ruleSummaryText = remember(definition.rootGroup) {
        summarizeRuleGroup(definition.rootGroup)
    }

    fun buildEntity(asNew: Boolean): SmartPlaylistEntity {
        val baseName = name.trim().ifBlank { "Smart Mix" }
        val finalName = if (asNew && initial != null && baseName == initial.name) "$baseName (Copy)" else baseName
        return SmartPlaylistEntity(
            id = if (asNew) 0L else (initial?.id ?: 0L),
            name = finalName,
            description = description.trim(),
            definitionJson = SmartPlaylistEvaluator.encodeDefinition(definition),
            isBuiltIn = if (asNew) false else (initial?.isBuiltIn ?: false),
            createdAt = if (asNew) System.currentTimeMillis() else (initial?.createdAt ?: System.currentTimeMillis())
        )
    }

    Dialog(
        onDismissRequest = { attemptDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        BackHandler { attemptDismiss() }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Discard Unsaved Rules?") },
                text = { Text("You have modified this smart playlist's rules or details.") },
                confirmButton = {
                    Button(onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    }) { SingleLineButtonLabel("DISCARD") }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) { SingleLineButtonLabel("KEEP EDITING") }
                }
            )
        }

        Surface(
            color = MaterialTheme.colors.background,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("smart_rule_editor_screen")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (initial == null) "New Smart Playlist" else "Edit Smart Rules", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { attemptDismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Smart Rule Editor")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            name = initial?.name ?: ""
                            description = initial?.description ?: ""
                            definition = defaultDefinition
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Rules")
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(CadenceSpacing.md)
                ) {
                    // Live Match Preview Banner + Collapsible Sample List
                    Surface(
                        color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(CadenceRadius.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(CadenceSpacing.md)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPreviewTracks = !showPreviewTracks },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Live preview: ${matchedTracks.size} matching track(s)",
                                        style = MaterialTheme.typography.subtitle2.tabular(),
                                        color = MaterialTheme.colors.primary
                                    )
                                    if (ruleSummaryText.isNotBlank()) {
                                        Text(
                                            text = ruleSummaryText,
                                            style = MaterialTheme.typography.caption,
                                            color = ext.subtleText,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (showPreviewTracks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle preview tracks",
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                            if (showPreviewTracks) {
                                Spacer(Modifier.height(CadenceSpacing.sm))
                                Divider(color = ext.dividerColor)
                                if (matchedTracks.isEmpty()) {
                                    Text(
                                        "No tracks in your library match these criteria yet.",
                                        style = MaterialTheme.typography.caption,
                                        color = ext.subtleText,
                                        modifier = Modifier.padding(top = CadenceSpacing.sm)
                                    )
                                } else {
                                    matchedTracks.take(8).forEach { track ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TrackArtwork(track.artworkUri, track.contentUri, track.title, Modifier.size(32.dp))
                                            Spacer(Modifier.width(CadenceSpacing.sm))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(track.title, style = MaterialTheme.typography.body2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(track.artist, style = MaterialTheme.typography.caption, color = ext.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(
                                                TextNormalizer.formatDuration(track.durationMs),
                                                style = MaterialTheme.typography.caption.tabular(),
                                                color = ext.subtleText
                                            )
                                        }
                                    }
                                    if (matchedTracks.size > 8) {
                                        Text(
                                            "+ ${matchedTracks.size - 8} more matches",
                                            style = MaterialTheme.typography.caption.tabular(),
                                            color = MaterialTheme.colors.primary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Smart Playlist Name") },
                        placeholder = { Text("e.g. Forgotten Rock Favorites") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("smart_name_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Condition Groups", style = MaterialTheme.typography.subtitle1)

                    RuleGroupEditorCard(
                        group = definition.rootGroup,
                        depth = 0,
                        onChange = { updatedRoot -> definition = definition.copy(rootGroup = updatedRoot) },
                        onRemoveGroup = null
                    )

                    Divider(color = ext.dividerColor)
                    Text("Sorting & Result Limit", style = MaterialTheme.typography.subtitle1)
                    val primarySort = definition.sorts.firstOrNull() ?: SmartSortSpec()
                    var sortMenuOpen by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { sortMenuOpen = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SingleLineButtonLabel(
                                    text = "Sort: ${primarySort.field.label}",
                                    style = MaterialTheme.typography.caption,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                SortField.entries.forEach { sf ->
                                    DropdownMenuItem(onClick = {
                                        definition = definition.copy(sorts = listOf(primarySort.copy(field = sf)))
                                        sortMenuOpen = false
                                    }) { Text(sf.label) }
                                }
                            }
                        }
                        FilterChip(
                            selected = primarySort.direction == SortDirection.DESC,
                            onClick = {
                                val nextDir = if (primarySort.direction == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
                                definition = definition.copy(sorts = listOf(primarySort.copy(direction = nextDir)))
                            }
                        ) {
                            SingleLineButtonLabel(
                                text = if (primarySort.direction == SortDirection.DESC) "Desc" else "Asc",
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }

                    var maxSlider by remember(definition.maxResults) { mutableFloatStateOf(definition.maxResults.toFloat()) }
                    Text("Maximum Tracks: ${maxSlider.toInt()}", style = MaterialTheme.typography.caption.tabular(), color = ext.subtleText)
                    Slider(
                        value = maxSlider,
                        onValueChange = { maxSlider = it },
                        onValueChangeFinished = { definition = definition.copy(maxResults = maxSlider.toInt()) },
                        valueRange = 10f..500f
                    )
                }

                Divider(color = ext.dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                ) {
                    if (initial != null) {
                        OutlinedButton(
                            onClick = { onSave(buildEntity(asNew = true)) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SingleLineButtonLabel("SAVE AS NEW")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { attemptDismiss() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SingleLineButtonLabel("CANCEL")
                        }
                    }
                    Button(
                        onClick = { onSave(buildEntity(asNew = false)) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).testTag("save_smart_rules_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        SingleLineButtonLabel("SAVE RULES")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RuleGroupEditorCard(
    group: SmartRuleGroup,
    depth: Int,
    onChange: (SmartRuleGroup) -> Unit,
    onRemoveGroup: (() -> Unit)?
) {
    val ext = LocalCadenceTheme.current
    val accentBarColor = if (depth == 0) MaterialTheme.colors.primary else MaterialTheme.colors.secondary

    Surface(
        shape = RoundedCornerShape(CadenceRadius.sm),
        color = if (depth % 2 == 0) ext.elevatedSurface else MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, ext.subtleBorder),
        modifier = Modifier.fillMaxWidth().padding(vertical = CadenceSpacing.xs)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .background(accentBarColor.copy(alpha = 0.78f))
            )
            Column(modifier = Modifier.weight(1f).padding(CadenceSpacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = group.combinator == SmartCombinator.ALL,
                        onClick = { onChange(group.copy(combinator = SmartCombinator.ALL)) }
                    ) { SingleLineButtonLabel("Match ALL", style = MaterialTheme.typography.caption) }
                    FilterChip(
                        selected = group.combinator == SmartCombinator.ANY,
                        onClick = { onChange(group.copy(combinator = SmartCombinator.ANY)) }
                    ) { SingleLineButtonLabel("Match ANY", style = MaterialTheme.typography.caption) }
                    Spacer(Modifier.weight(1f))
                    if (onRemoveGroup != null) {
                        IconButton(onClick = onRemoveGroup, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Group", tint = MaterialTheme.colors.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                group.conditions.forEachIndexed { idx, cond ->
                    ConditionRowEditor(
                        condition = cond,
                        onUpdate = { updated ->
                            val next = group.conditions.toMutableList()
                            next[idx] = updated
                            onChange(group.copy(conditions = next))
                        },
                        onDelete = {
                            val next = group.conditions.toMutableList()
                            next.removeAt(idx)
                            onChange(group.copy(conditions = next))
                        }
                    )
                }

                group.childGroups.forEachIndexed { gIdx, child ->
                    RuleGroupEditorCard(
                        group = child,
                        depth = depth + 1,
                        onChange = { updatedChild ->
                            val nextGroups = group.childGroups.toMutableList()
                            nextGroups[gIdx] = updatedChild
                            onChange(group.copy(childGroups = nextGroups))
                        },
                        onRemoveGroup = {
                            val nextGroups = group.childGroups.toMutableList()
                            nextGroups.removeAt(gIdx)
                            onChange(group.copy(childGroups = nextGroups))
                        }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm),
                    modifier = Modifier.padding(top = CadenceSpacing.sm)
                ) {
                    OutlinedButton(
                        onClick = {
                            val nextCond = SmartCondition(
                                id = UUID.randomUUID().toString(),
                                field = SmartField.GENRE,
                                operator = SmartOperator.CONTAINS,
                                value = ""
                            )
                            onChange(group.copy(conditions = group.conditions + nextCond))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        SingleLineButtonLabel("Rule", style = MaterialTheme.typography.caption)
                    }
                    if (depth < 2) {
                        OutlinedButton(
                            onClick = {
                                val sub = SmartRuleGroup(
                                    id = UUID.randomUUID().toString(),
                                    combinator = if (group.combinator == SmartCombinator.ALL) SmartCombinator.ANY else SmartCombinator.ALL,
                                    conditions = listOf(
                                        SmartCondition(
                                            id = UUID.randomUUID().toString(),
                                            field = SmartField.ARTIST,
                                            operator = SmartOperator.CONTAINS,
                                            value = ""
                                        )
                                    )
                                )
                                onChange(group.copy(childGroups = group.childGroups + sub))
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            SingleLineButtonLabel("Subgroup", style = MaterialTheme.typography.caption)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
private fun ConditionRowEditor(
    condition: SmartCondition,
    onUpdate: (SmartCondition) -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    var fieldMenu by remember { mutableStateOf(false) }
    var opMenu by remember { mutableStateOf(false) }

    val allowedOps = remember(condition.field) {
        when (condition.field.kind) {
            SmartFieldKind.TEXT -> listOf(
                SmartOperator.CONTAINS, SmartOperator.NOT_CONTAINS,
                SmartOperator.EQUALS, SmartOperator.NOT_EQUALS,
                SmartOperator.STARTS_WITH, SmartOperator.IS_EMPTY, SmartOperator.IS_NOT_EMPTY
            )
            SmartFieldKind.NUMBER -> listOf(
                SmartOperator.GREATER_THAN, SmartOperator.LESS_THAN,
                SmartOperator.EQUALS, SmartOperator.NOT_EQUALS,
                SmartOperator.BETWEEN, SmartOperator.IS_EMPTY, SmartOperator.IS_NOT_EMPTY
            )
            SmartFieldKind.BOOL, SmartFieldKind.PRESENCE_BOOL -> listOf(
                SmartOperator.IS_TRUE, SmartOperator.IS_FALSE
            )
        }
    }

    val needsValue = condition.field.kind in setOf(SmartFieldKind.TEXT, SmartFieldKind.NUMBER) &&
        condition.operator !in setOf(SmartOperator.IS_EMPTY, SmartOperator.IS_NOT_EMPTY)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = CadenceSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { fieldMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SingleLineButtonLabel(condition.field.label, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = fieldMenu, onDismissRequest = { fieldMenu = false }) {
                    SmartField.entries.forEach { sf ->
                        DropdownMenuItem(onClick = {
                            val defaultOp = when (sf.kind) {
                                SmartFieldKind.TEXT -> SmartOperator.CONTAINS
                                SmartFieldKind.NUMBER -> SmartOperator.GREATER_THAN
                                SmartFieldKind.BOOL, SmartFieldKind.PRESENCE_BOOL -> SmartOperator.IS_TRUE
                            }
                            onUpdate(condition.copy(field = sf, operator = defaultOp, value = "", secondValue = ""))
                            fieldMenu = false
                        }) { Text(sf.label) }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { opMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SingleLineButtonLabel(condition.operator.label, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = opMenu, onDismissRequest = { opMenu = false }) {
                    allowedOps.forEach { op ->
                        DropdownMenuItem(onClick = {
                            onUpdate(condition.copy(operator = op))
                            opMenu = false
                        }) { Text(op.label) }
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Rule", modifier = Modifier.size(18.dp))
            }
        }

        if (condition.field.kind == SmartFieldKind.BOOL || condition.field.kind == SmartFieldKind.PRESENCE_BOOL) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                FilterChip(
                    selected = condition.operator == SmartOperator.IS_TRUE,
                    onClick = { onUpdate(condition.copy(operator = SmartOperator.IS_TRUE)) }
                ) { SingleLineButtonLabel("Yes", style = MaterialTheme.typography.caption) }
                FilterChip(
                    selected = condition.operator == SmartOperator.IS_FALSE,
                    onClick = { onUpdate(condition.copy(operator = SmartOperator.IS_FALSE)) }
                ) { SingleLineButtonLabel("No", style = MaterialTheme.typography.caption) }
            }
        } else if (needsValue) {
            when (condition.field) {
                SmartField.RATING -> {
                    val curStar = condition.value.toIntOrNull() ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Stars:", style = MaterialTheme.typography.caption, color = ext.subtleText)
                        Spacer(Modifier.width(6.dp))
                        for (s in 1..5) {
                            IconButton(onClick = { onUpdate(condition.copy(value = s.toString())) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = if (s <= curStar) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$s stars",
                                    tint = if (s <= curStar) MaterialTheme.colors.primary else ext.subtleText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (condition.operator == SmartOperator.BETWEEN) {
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = condition.secondValue,
                                onValueChange = { onUpdate(condition.copy(secondValue = it.filter { c -> c.isDigit() })) },
                                label = { Text("Max") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(72.dp)
                            )
                        }
                    }
                }
                SmartField.USER_TAGS -> {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedTextField(
                            value = condition.value,
                            onValueChange = { onUpdate(condition.copy(value = it)) },
                            label = { Text("Tag name") },
                            placeholder = { Text("e.g. Workout") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf("Workout", "Chill", "Focus", "Drive", "Late Night", "Study", "Live").forEach { tag ->
                                FilterChip(
                                    selected = condition.value.equals(tag, ignoreCase = true),
                                    onClick = { onUpdate(condition.copy(value = tag)) }
                                ) { SingleLineButtonLabel(tag, style = MaterialTheme.typography.caption) }
                            }
                        }
                    }
                }
                else -> {
                    val kb = if (condition.field.kind == SmartFieldKind.NUMBER) KeyboardType.Number else KeyboardType.Text
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedTextField(
                            value = condition.value,
                            onValueChange = { onUpdate(condition.copy(value = it)) },
                            label = { Text(if (condition.operator == SmartOperator.BETWEEN) "Min" else "Value") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = kb),
                            modifier = Modifier.weight(1f)
                        )
                        if (condition.operator == SmartOperator.BETWEEN) {
                            OutlinedTextField(
                                value = condition.secondValue,
                                onValueChange = { onUpdate(condition.copy(secondValue = it)) },
                                label = { Text("Max") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = kb),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = condition.exclude,
                onCheckedChange = { onUpdate(condition.copy(exclude = it)) }
            )
            Text("Exclude match (NOT)", style = MaterialTheme.typography.caption, color = ext.subtleText)
        }
        Divider(color = ext.dividerColor)
    }
}

private fun summarizeRuleGroup(group: SmartRuleGroup): String {
    val joiner = if (group.combinator == SmartCombinator.ALL) " AND " else " OR "
    val condTexts = group.conditions.map { c ->
        val prefix = if (c.exclude) "NOT " else ""
        val valPart = when {
            c.field.kind in setOf(SmartFieldKind.BOOL, SmartFieldKind.PRESENCE_BOOL) -> ""
            c.operator in setOf(SmartOperator.IS_EMPTY, SmartOperator.IS_NOT_EMPTY) -> ""
            c.operator == SmartOperator.BETWEEN -> " ${c.value}–${c.secondValue}"
            else -> " \"${c.value}\""
        }
        "$prefix${c.field.label} ${c.operator.label}$valPart"
    }
    val childTexts = group.childGroups.map { "(${summarizeRuleGroup(it)})" }
    return (condTexts + childTexts).filter { it.isNotBlank() }.joinToString(joiner)
}
