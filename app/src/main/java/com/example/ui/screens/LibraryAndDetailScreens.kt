package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.LibraryTabKey
import com.example.core.model.SmartViewCategory
import com.example.core.util.TextNormalizer
import com.example.data.db.AlbumSummary
import com.example.data.db.ArtistSummary
import com.example.data.db.FolderSummary
import com.example.data.db.GenreSummary
import com.example.data.db.TrackEntity
import com.example.data.preferences.ScreenSortPreference
import com.example.ui.components.ArtistCardGridItem
import com.example.ui.components.ArtistRowItem
import com.example.ui.components.CadenceInlineSearchBar
import com.example.ui.components.CollectionBatchActionBar
import com.example.ui.components.CollectionListRow
import com.example.ui.components.CollectionSheetTarget
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FastScrollLazyListOverlay
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.SortAndFilterDialog
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackListItem
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    initialTab: LibraryTabKey = LibraryTabKey.SONGS,
    forcedSingleTab: Boolean = false,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onOpenArtist: (String, Boolean) -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onOpenSmartView: (SmartViewCategory) -> Unit,
    onOpenYear: (Int) -> Unit,
    onOpenBatchTagEditor: (List<Long>) -> Unit,
    onManageBatchUserTags: (List<Long>) -> Unit,
    onAddTracksToPlaylist: (List<Long>) -> Unit,
    onPickSafFolder: () -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onCollectionMore: (CollectionSheetTarget) -> Unit
) {
    val scope = rememberCoroutineScope()
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    var selectedTab by rememberSaveable(initialTab, forcedSingleTab) { mutableStateOf(initialTab) }
    var filterQuery by rememberSaveable(selectedTab, forcedSingleTab) { mutableStateOf("") }
    var showSortDialog by remember { mutableStateOf(false) }
    var albumsGridMode by rememberSaveable { mutableStateOf(true) }
    var artistsGridMode by rememberSaveable { mutableStateOf(false) }
    var activeSmartView by remember { mutableStateOf<SmartViewCategory?>(null) }

    var selectedTrackIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedCollectionKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    val settings by viewModel.settings.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val screenKey = "lib_${selectedTab.name}"
    val sortPref = settings.screenSortPrefs[screenKey] ?: ScreenSortPreference()

    val songs by remember(screenKey) {
        viewModel.observeTracksForScreen(screenKey)
    }.collectAsState(initial = emptyList())

    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albumArtists by viewModel.albumArtists.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val composers by viewModel.composers.collectAsState()
    val years by viewModel.years.collectAsState()
    val folders by viewModel.folders.collectAsState()

    val smartViewTracks by remember(activeSmartView) {
        activeSmartView?.let { viewModel.observeSmartViewCategory(it) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val normFilter = remember(filterQuery) { TextNormalizer.normalizeForSearch(filterQuery.trim()) }
    val filteredSongs = remember(songs, normFilter) {
        if (normFilter.isBlank()) songs else songs.filter {
            TextNormalizer.normalizeForSearch("${it.title} ${it.artist} ${it.album}").contains(normFilter)
        }
    }
    val filteredAlbums = remember(albums, normFilter) {
        if (normFilter.isBlank()) albums else albums.filter {
            TextNormalizer.normalizeForSearch("${it.album} ${it.albumArtist}").contains(normFilter)
        }
    }
    val filteredArtists = remember(artists, normFilter) {
        if (normFilter.isBlank()) artists else artists.filter {
            TextNormalizer.normalizeForSearch(it.artist).contains(normFilter)
        }
    }
    val filteredAlbumArtists = remember(albumArtists, normFilter) {
        if (normFilter.isBlank()) albumArtists else albumArtists.filter {
            TextNormalizer.normalizeForSearch(it.artist).contains(normFilter)
        }
    }
    val filteredGenres = remember(genres, normFilter) {
        if (normFilter.isBlank()) genres else genres.filter {
            TextNormalizer.normalizeForSearch(it.genre).contains(normFilter)
        }
    }
    val filteredComposers = remember(composers, normFilter) {
        if (normFilter.isBlank()) composers else composers.filter {
            TextNormalizer.normalizeForSearch(it.artist).contains(normFilter)
        }
    }
    val filteredYears = remember(years, normFilter) {
        if (normFilter.isBlank()) years else years.filter {
            it.year.toString().contains(normFilter)
        }
    }
    val filteredFolders = remember(folders, normFilter) {
        if (normFilter.isBlank()) folders else folders.filter {
            TextNormalizer.normalizeForSearch("${it.folderName} ${it.folderPath}").contains(normFilter)
        }
    }
    val filteredSmartViews = remember(normFilter) {
        if (normFilter.isBlank()) SmartViewCategory.entries else SmartViewCategory.entries.filter {
            TextNormalizer.normalizeForSearch("${it.label} ${it.description}").contains(normFilter)
        }
    }
    val filteredSmartViewTracks = remember(smartViewTracks, normFilter) {
        if (normFilter.isBlank()) smartViewTracks else smartViewTracks.filter {
            TextNormalizer.normalizeForSearch("${it.title} ${it.artist} ${it.album}").contains(normFilter)
        }
    }

    BackHandler(enabled = selectedTrackIds.isNotEmpty() || selectedCollectionKeys.isNotEmpty() || activeSmartView != null) {
        when {
            selectedTrackIds.isNotEmpty() -> selectedTrackIds = emptySet()
            selectedCollectionKeys.isNotEmpty() -> selectedCollectionKeys = emptySet()
            activeSmartView != null -> activeSmartView = null
        }
    }

    if (showSortDialog) {
        SortAndFilterDialog(
            current = sortPref,
            onApply = { viewModel.updateScreenSort(screenKey, it) },
            onDismiss = { showSortDialog = false }
        )
    }

    suspend fun resolveSelectedCollectionTracks(): List<TrackEntity> {
        val list = mutableListOf<TrackEntity>()
        when (selectedTab) {
            LibraryTabKey.ALBUMS -> filteredAlbums.filter { "${it.album}::${it.albumArtist}" in selectedCollectionKeys }
                .forEach { list.addAll(viewModel.getTracksForAlbum(it.album, it.albumArtist)) }
            LibraryTabKey.ARTISTS -> filteredArtists.filter { it.artist in selectedCollectionKeys }
                .forEach { list.addAll(viewModel.getTracksForArtist(it.artist, false)) }
            LibraryTabKey.ALBUM_ARTISTS -> filteredAlbumArtists.filter { it.artist in selectedCollectionKeys }
                .forEach { list.addAll(viewModel.getTracksForArtist(it.artist, true)) }
            LibraryTabKey.COMPOSERS -> filteredComposers.filter { it.artist in selectedCollectionKeys }
                .forEach { list.addAll(viewModel.getTracksForArtist(it.artist, false)) }
            LibraryTabKey.GENRES -> filteredGenres.filter { it.genre in selectedCollectionKeys }
                .forEach { list.addAll(viewModel.getTracksForGenre(it.genre)) }
            LibraryTabKey.YEARS -> filteredYears.filter { it.year.toString() in selectedCollectionKeys }
                .forEach { list.addAll(viewModel.getTracksForYear(it.year)) }
            LibraryTabKey.FOLDERS -> filteredFolders.filter { it.folderPath in selectedCollectionKeys }
                .forEach { list.addAll(viewModel.getTracksForFolder(it.folderPath)) }
            else -> Unit
        }
        return list.distinctBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        if (selectedTrackIds.isNotEmpty()) {
            val activeTrackList = if (selectedTab == LibraryTabKey.COLLECTIONS && activeSmartView != null) {
                filteredSmartViewTracks
            } else {
                filteredSongs
            }
            CollectionBatchActionBar(
                selectedCount = selectedTrackIds.size,
                onCancel = { selectedTrackIds = emptySet() },
                onSelectAll = { selectedTrackIds = activeTrackList.map { it.id }.toSet() },
                onPlayNext = {
                    val chosen = activeTrackList.filter { it.id in selectedTrackIds }
                    viewModel.container.playerConnection.addPlayNext(chosen)
                    viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                    selectedTrackIds = emptySet()
                },
                onAddToQueue = {
                    val chosen = activeTrackList.filter { it.id in selectedTrackIds }
                    viewModel.container.playerConnection.addToEnd(chosen)
                    viewModel.showMessage("Added ${chosen.size} track(s) to queue.")
                    selectedTrackIds = emptySet()
                },
                onAddToPlaylist = {
                    onAddTracksToPlaylist(selectedTrackIds.toList())
                    selectedTrackIds = emptySet()
                },
                onBatchTagEdit = {
                    onOpenBatchTagEditor(selectedTrackIds.toList())
                    selectedTrackIds = emptySet()
                },
                onManageUserTags = {
                    onManageBatchUserTags(selectedTrackIds.toList())
                    selectedTrackIds = emptySet()
                },
                onShare = {
                    val chosen = activeTrackList.filter { it.id in selectedTrackIds }
                    viewModel.shareTracks(chosen)
                    selectedTrackIds = emptySet()
                }
            )
        } else if (selectedCollectionKeys.isNotEmpty()) {
            CollectionBatchActionBar(
                selectedCount = selectedCollectionKeys.size,
                onCancel = { selectedCollectionKeys = emptySet() },
                onSelectAll = {
                    selectedCollectionKeys = when (selectedTab) {
                        LibraryTabKey.ALBUMS -> filteredAlbums.map { "${it.album}::${it.albumArtist}" }.toSet()
                        LibraryTabKey.ARTISTS -> filteredArtists.map { it.artist }.toSet()
                        LibraryTabKey.ALBUM_ARTISTS -> filteredAlbumArtists.map { it.artist }.toSet()
                        LibraryTabKey.COMPOSERS -> filteredComposers.map { it.artist }.toSet()
                        LibraryTabKey.GENRES -> filteredGenres.map { it.genre }.toSet()
                        LibraryTabKey.YEARS -> filteredYears.map { it.year.toString() }.toSet()
                        LibraryTabKey.FOLDERS -> filteredFolders.map { it.folderPath }.toSet()
                        else -> emptySet()
                    }
                },
                onPlayNext = {
                    scope.launch {
                        val tracks = resolveSelectedCollectionTracks()
                        if (tracks.isNotEmpty()) {
                            viewModel.container.playerConnection.addPlayNext(tracks)
                            viewModel.showMessage("Queued ${tracks.size} track(s) next.")
                        }
                        selectedCollectionKeys = emptySet()
                    }
                },
                onAddToQueue = {
                    scope.launch {
                        val tracks = resolveSelectedCollectionTracks()
                        if (tracks.isNotEmpty()) {
                            viewModel.container.playerConnection.addToEnd(tracks)
                            viewModel.showMessage("Added ${tracks.size} track(s) to queue.")
                        }
                        selectedCollectionKeys = emptySet()
                    }
                },
                onAddToPlaylist = {
                    scope.launch {
                        val tracks = resolveSelectedCollectionTracks()
                        if (tracks.isNotEmpty()) onAddTracksToPlaylist(tracks.map { it.id })
                        selectedCollectionKeys = emptySet()
                    }
                },
                onBatchTagEdit = {
                    scope.launch {
                        val tracks = resolveSelectedCollectionTracks()
                        if (tracks.isNotEmpty()) onOpenBatchTagEditor(tracks.map { it.id })
                        selectedCollectionKeys = emptySet()
                    }
                },
                onManageUserTags = {
                    scope.launch {
                        val tracks = resolveSelectedCollectionTracks()
                        if (tracks.isNotEmpty()) onManageBatchUserTags(tracks.map { it.id })
                        selectedCollectionKeys = emptySet()
                    }
                },
                onShare = {
                    scope.launch {
                        val tracks = resolveSelectedCollectionTracks()
                        if (tracks.isNotEmpty()) viewModel.shareTracks(tracks)
                        selectedCollectionKeys = emptySet()
                    }
                }
            )
        }

        if (!forcedSingleTab) {
            ScrollableTabRow(
                selectedTabIndex = LibraryTabKey.entries.indexOf(selectedTab),
                edgePadding = hPad
            ) {
                LibraryTabKey.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            activeSmartView = null
                            selectedTrackIds = emptySet()
                            selectedCollectionKeys = emptySet()
                            filterQuery = ""
                        },
                        text = { SingleLineButtonLabel(tab.label, style = MaterialTheme.typography.button) },
                        modifier = Modifier.testTag("lib_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        val searchPlaceholder = when (selectedTab) {
            LibraryTabKey.SONGS -> "Filter songs, artists, albums..."
            LibraryTabKey.ALBUMS -> "Filter albums or album artists..."
            LibraryTabKey.ARTISTS -> "Filter artists..."
            LibraryTabKey.ALBUM_ARTISTS -> "Filter album artists..."
            LibraryTabKey.GENRES -> "Filter genres..."
            LibraryTabKey.COMPOSERS -> "Filter composers..."
            LibraryTabKey.YEARS -> "Filter release years..."
            LibraryTabKey.FOLDERS -> "Filter music folders..."
            LibraryTabKey.COLLECTIONS -> if (activeSmartView != null) "Filter ${activeSmartView!!.label}..." else "Filter smart views..."
        }
        CadenceInlineSearchBar(
            query = filterQuery,
            onQueryChange = { filterQuery = it },
            placeholder = searchPlaceholder
        )

        when (selectedTab) {
            LibraryTabKey.SONGS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = hPad, end = CadenceSpacing.xs, top = CadenceSpacing.xs, bottom = CadenceSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredSongs.size} tracks",
                        style = MaterialTheme.typography.caption.tabular(),
                        color = ext.subtleText,
                        modifier = Modifier.weight(1f)
                    )
                    if (sortPref.filter.isActive) {
                        Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.14f), shape = MaterialTheme.shapes.small) {
                            Text(
                                "Filtered",
                                style = MaterialTheme.typography.overline,
                                color = MaterialTheme.colors.primary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = { showSortDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("songs_sort_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort and Filter")
                    }
                    IconButton(
                        onClick = {
                            if (filteredSongs.isNotEmpty()) {
                                viewModel.container.playerConnection.playTracks(filteredSongs.shuffled(), 0, shuffle = true)
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle Songs")
                    }
                }
                Divider(color = ext.dividerColor)
                if (filteredSongs.isEmpty()) {
                    EmptyStateView(
                        title = "No Songs Found",
                        subtitle = if (filterQuery.isNotBlank()) "No tracks match \"$filterQuery\"." else "Scan your audio folders or clear active filters.",
                        actionLabel = if (filterQuery.isNotBlank()) "Clear Filter" else "Rescan",
                        onAction = {
                            if (filterQuery.isNotBlank()) filterQuery = "" else viewModel.triggerLibraryScan()
                        },
                        icon = Icons.Default.MusicNote
                    )
                } else {
                    val songsListState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(state = songsListState, modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(filteredSongs, key = { _, t -> "lib_song_${t.id}" }, contentType = { _, _ -> "track" }) { idx, track ->
                                val isSelected = track.id in selectedTrackIds
                                TrackListItem(
                                    track = track,
                                    isPlayingCurrent = playback.currentTrack?.id == track.id,
                                    isSelected = isSelected,
                                    selectionMode = selectedTrackIds.isNotEmpty(),
                                    onClick = {
                                        if (selectedTrackIds.isNotEmpty()) {
                                            selectedTrackIds = if (isSelected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                        } else {
                                            viewModel.container.playerConnection.playTracks(filteredSongs, idx)
                                        }
                                    },
                                    onLongClick = {
                                        selectedTrackIds = if (isSelected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                    },
                                    onMoreClick = { onTrackMore(track) },
                                    onToggleFavorite = { viewModel.toggleFavorite(track) },
                                    onArtistClick = { artist -> onOpenArtist(artist, false) },
                                    onAlbumClick = { alb, albArt ->
                                        onOpenAlbum(AlbumSummary(alb, albArt, track.albumId, track.artworkUri, 0, 0L, track.year))
                                    }
                                )
                            }
                        }
                        FastScrollLazyListOverlay(
                            listState = songsListState,
                            itemCount = filteredSongs.size,
                            labelForIndex = { idx ->
                                filteredSongs.getOrNull(idx)?.title?.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            }
                        )
                    }
                }
            }

            LibraryTabKey.ALBUMS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = hPad, end = CadenceSpacing.xs, top = CadenceSpacing.xs, bottom = CadenceSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filteredAlbums.size} albums",
                        style = MaterialTheme.typography.caption.tabular(),
                        color = ext.subtleText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { albumsGridMode = !albumsGridMode },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("albums_view_toggle")
                    ) {
                        Icon(
                            imageVector = if (albumsGridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (albumsGridMode) "Switch to List View" else "Switch to Grid View"
                        )
                    }
                }
                Divider(color = ext.dividerColor)
                if (filteredAlbums.isEmpty()) {
                    EmptyStateView("No Albums", "No matching indexed albums found.", icon = Icons.Default.Album)
                } else if (albumsGridMode) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isCompact) 136.dp else 156.dp),
                        contentPadding = PaddingValues(hPad),
                        verticalArrangement = Arrangement.spacedBy(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md)
                    ) {
                        items(filteredAlbums, key = { "alb_${it.album}_${it.albumArtist}" }, contentType = { "album_grid" }) { album ->
                            val key = "${album.album}::${album.albumArtist}"
                            val isSelected = key in selectedCollectionKeys
                            val selectionActive = selectedCollectionKeys.isNotEmpty()
                            Surface(
                                shape = RoundedCornerShape(CadenceRadius.sm),
                                color = ext.elevatedSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colors.primary else ext.subtleBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionActive) {
                                                selectedCollectionKeys = if (isSelected) selectedCollectionKeys - key else selectedCollectionKeys + key
                                            } else {
                                                onOpenAlbum(album)
                                            }
                                        },
                                        onLongClick = {
                                            selectedCollectionKeys = if (isSelected) selectedCollectionKeys - key else selectedCollectionKeys + key
                                        }
                                    )
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    ) {
                                        TrackArtwork(album.artworkUri, null, album.album, Modifier.fillMaxSize(), cornerRadius = 0.dp)
                                        if (selectionActive) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    selectedCollectionKeys = if (isSelected) selectedCollectionKeys - key else selectedCollectionKeys + key
                                                },
                                                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.14f) else Color.Transparent)
                                            .padding(start = CadenceSpacing.sm, end = 2.dp, top = CadenceSpacing.xs, bottom = CadenceSpacing.xs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(album.album, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val yearPart = if (album.year > 0) " • ${album.year}" else ""
                                            Text(
                                                "${album.albumArtist} • ${album.trackCount} tracks$yearPart",
                                                style = MaterialTheme.typography.caption.tabular(),
                                                color = ext.subtleText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = if (!selectionActive) {
                                                    Modifier.clickable { onOpenArtist(album.albumArtist, true) }
                                                } else Modifier
                                            )
                                        }
                                        if (!selectionActive) {
                                            IconButton(
                                                onClick = { onCollectionMore(CollectionSheetTarget.Album(album)) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "Album actions",
                                                    tint = ext.subtleText,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredAlbums, key = { "albl_${it.album}_${it.albumArtist}" }) { album ->
                            val key = "${album.album}::${album.albumArtist}"
                            val isSelected = key in selectedCollectionKeys
                            CollectionListRow(
                                title = album.album,
                                subtitle = "${album.albumArtist} • ${album.trackCount} tracks",
                                trailingMeta = if (album.year > 0) album.year.toString() else null,
                                artworkUri = album.artworkUri,
                                leadingIcon = if (album.artworkUri == null) Icons.Default.Album else null,
                                isSelected = isSelected,
                                selectionMode = selectedCollectionKeys.isNotEmpty(),
                                onClick = {
                                    if (selectedCollectionKeys.isNotEmpty()) {
                                        selectedCollectionKeys = if (isSelected) selectedCollectionKeys - key else selectedCollectionKeys + key
                                    } else {
                                        onOpenAlbum(album)
                                    }
                                },
                                onLongClick = {
                                    selectedCollectionKeys = if (isSelected) selectedCollectionKeys - key else selectedCollectionKeys + key
                                },
                                onMoreClick = { onCollectionMore(CollectionSheetTarget.Album(album)) }
                            )
                        }
                    }
                }
            }

            LibraryTabKey.ARTISTS -> ArtistListPane(
                items = filteredArtists,
                isAlbumArtist = false,
                gridMode = artistsGridMode,
                onToggleGridMode = { artistsGridMode = !artistsGridMode },
                selectedKeys = selectedCollectionKeys,
                onToggleSelect = { k -> selectedCollectionKeys = if (k in selectedCollectionKeys) selectedCollectionKeys - k else selectedCollectionKeys + k },
                onOpen = { onOpenArtist(it.artist, false) },
                onMore = { onCollectionMore(CollectionSheetTarget.Artist(it, false)) }
            )

            LibraryTabKey.ALBUM_ARTISTS -> ArtistListPane(
                items = filteredAlbumArtists,
                isAlbumArtist = true,
                gridMode = artistsGridMode,
                onToggleGridMode = { artistsGridMode = !artistsGridMode },
                selectedKeys = selectedCollectionKeys,
                onToggleSelect = { k -> selectedCollectionKeys = if (k in selectedCollectionKeys) selectedCollectionKeys - k else selectedCollectionKeys + k },
                onOpen = { onOpenArtist(it.artist, true) },
                onMore = { onCollectionMore(CollectionSheetTarget.Artist(it, true)) }
            )

            LibraryTabKey.GENRES -> {
                if (filteredGenres.isEmpty()) {
                    EmptyStateView("No Genres", "No matching genres found.", icon = Icons.Default.Category)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredGenres, key = { "genre_${it.genre}" }) { g: GenreSummary ->
                            val isSelected = g.genre in selectedCollectionKeys
                            CollectionListRow(
                                title = g.genre,
                                subtitle = "${g.trackCount} tracks",
                                trailingMeta = TextNormalizer.formatDuration(g.totalDurationMs),
                                leadingIcon = Icons.Default.Category,
                                isSelected = isSelected,
                                selectionMode = selectedCollectionKeys.isNotEmpty(),
                                onClick = {
                                    if (selectedCollectionKeys.isNotEmpty()) {
                                        selectedCollectionKeys = if (isSelected) selectedCollectionKeys - g.genre else selectedCollectionKeys + g.genre
                                    } else {
                                        onOpenGenre(g.genre)
                                    }
                                },
                                onLongClick = {
                                    selectedCollectionKeys = if (isSelected) selectedCollectionKeys - g.genre else selectedCollectionKeys + g.genre
                                },
                                onMoreClick = { onCollectionMore(CollectionSheetTarget.Genre(g)) }
                            )
                        }
                    }
                }
            }

            LibraryTabKey.COMPOSERS -> ArtistListPane(
                items = filteredComposers,
                isAlbumArtist = false,
                gridMode = artistsGridMode,
                onToggleGridMode = { artistsGridMode = !artistsGridMode },
                selectedKeys = selectedCollectionKeys,
                onToggleSelect = { k -> selectedCollectionKeys = if (k in selectedCollectionKeys) selectedCollectionKeys - k else selectedCollectionKeys + k },
                onOpen = { onOpenArtist(it.artist, false) },
                onMore = { onCollectionMore(CollectionSheetTarget.Artist(it, false)) }
            )

            LibraryTabKey.YEARS -> {
                if (filteredYears.isEmpty()) {
                    EmptyStateView("No Release Years", "No matching release years found.", icon = Icons.Default.CalendarToday)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredYears, key = { "yr_${it.year}" }) { y ->
                            val k = y.year.toString()
                            val isSelected = k in selectedCollectionKeys
                            CollectionListRow(
                                title = k,
                                subtitle = "${y.trackCount} tracks",
                                leadingIcon = Icons.Default.CalendarToday,
                                isSelected = isSelected,
                                selectionMode = selectedCollectionKeys.isNotEmpty(),
                                onClick = {
                                    if (selectedCollectionKeys.isNotEmpty()) {
                                        selectedCollectionKeys = if (isSelected) selectedCollectionKeys - k else selectedCollectionKeys + k
                                    } else {
                                        onOpenYear(y.year)
                                    }
                                },
                                onLongClick = {
                                    selectedCollectionKeys = if (isSelected) selectedCollectionKeys - k else selectedCollectionKeys + k
                                },
                                onMoreClick = { onCollectionMore(CollectionSheetTarget.Year(y)) }
                            )
                        }
                    }
                }
            }

            LibraryTabKey.FOLDERS -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = hPad, vertical = CadenceSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = onPickSafFolder,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            SingleLineButtonLabel("ADD FOLDER")
                        }
                        OutlinedButton(
                            onClick = { viewModel.triggerLibraryScan() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            SingleLineButtonLabel("RESCAN")
                        }
                    }
                    Divider(color = ext.dividerColor)
                    if (filteredFolders.isEmpty()) {
                        EmptyStateView("No Folders Indexed", "Add a storage folder via SAF or rescan MediaStore.", icon = Icons.Default.Folder)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredFolders, key = { "fld_${it.folderPath}" }) { folder ->
                                val isSelected = folder.folderPath in selectedCollectionKeys
                                CollectionListRow(
                                    title = folder.folderName,
                                    subtitle = folder.folderPath,
                                    trailingMeta = "${folder.trackCount} tracks",
                                    leadingIcon = Icons.Default.Folder,
                                    isSelected = isSelected,
                                    selectionMode = selectedCollectionKeys.isNotEmpty(),
                                    onClick = {
                                        if (selectedCollectionKeys.isNotEmpty()) {
                                            selectedCollectionKeys = if (isSelected) selectedCollectionKeys - folder.folderPath else selectedCollectionKeys + folder.folderPath
                                        } else {
                                            onOpenFolder(folder)
                                        }
                                    },
                                    onLongClick = {
                                        selectedCollectionKeys = if (isSelected) selectedCollectionKeys - folder.folderPath else selectedCollectionKeys + folder.folderPath
                                    },
                                    onMoreClick = { onCollectionMore(CollectionSheetTarget.Folder(folder)) }
                                )
                            }
                        }
                    }
                }
            }

            LibraryTabKey.COLLECTIONS -> {
                if (activeSmartView == null) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredSmartViews, key = { it.name }) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeSmartView = cat
                                        filterQuery = ""
                                        onOpenSmartView(cat)
                                    }
                                    .padding(horizontal = hPad, vertical = CadenceSpacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.label, style = MaterialTheme.typography.subtitle1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(cat.description, style = MaterialTheme.typography.caption, color = ext.subtleText)
                                }
                            }
                            Divider(color = ext.dividerColor, startIndent = hPad)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CadenceSpacing.sm, vertical = CadenceSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { activeSmartView = null; selectedTrackIds = emptySet() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Smart Views")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(activeSmartView!!.label, style = MaterialTheme.typography.subtitle1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${filteredSmartViewTracks.size} tracks", style = MaterialTheme.typography.caption.tabular(), color = ext.subtleText)
                            }
                            if (filteredSmartViewTracks.isNotEmpty()) {
                                IconButton(onClick = { viewModel.container.playerConnection.playTracks(filteredSmartViewTracks, 0) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = MaterialTheme.colors.primary)
                                }
                                IconButton(onClick = { viewModel.container.playerConnection.playTracks(filteredSmartViewTracks.shuffled(), 0, shuffle = true) }) {
                                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                                }
                            }
                        }
                        Divider(color = ext.dividerColor)
                        if (filteredSmartViewTracks.isEmpty()) {
                            EmptyStateView("No Tracks", "No tracks currently belong to ${activeSmartView!!.label}.")
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(filteredSmartViewTracks, key = { _, t -> "sv_${t.id}" }) { idx, track ->
                                    val isSelected = track.id in selectedTrackIds
                                    TrackListItem(
                                        track = track,
                                        isPlayingCurrent = playback.currentTrack?.id == track.id,
                                        isSelected = isSelected,
                                        selectionMode = selectedTrackIds.isNotEmpty(),
                                        onClick = {
                                            if (selectedTrackIds.isNotEmpty()) {
                                                selectedTrackIds = if (isSelected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                            } else {
                                                viewModel.container.playerConnection.playTracks(filteredSmartViewTracks, idx)
                                            }
                                        },
                                        onLongClick = {
                                            selectedTrackIds = if (isSelected) selectedTrackIds - track.id else selectedTrackIds + track.id
                                        },
                                        onMoreClick = { onTrackMore(track) },
                                        onToggleFavorite = { viewModel.toggleFavorite(track) },
                                        onArtistClick = { artist -> onOpenArtist(artist, false) },
                                        onAlbumClick = { alb, albArt ->
                                            onOpenAlbum(AlbumSummary(alb, albArt, track.albumId, track.artworkUri, 0, 0L, track.year))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistListPane(
    items: List<ArtistSummary>,
    isAlbumArtist: Boolean,
    gridMode: Boolean,
    onToggleGridMode: () -> Unit,
    selectedKeys: Set<String>,
    onToggleSelect: (String) -> Unit,
    onOpen: (ArtistSummary) -> Unit,
    onMore: (ArtistSummary) -> Unit
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = hPad, end = CadenceSpacing.xs, top = CadenceSpacing.xs, bottom = CadenceSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${items.size} artists",
                style = MaterialTheme.typography.caption.tabular(),
                color = ext.subtleText,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onToggleGridMode,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("artists_view_toggle")
            ) {
                Icon(
                    imageVector = if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = if (gridMode) "Switch to List View" else "Switch to Grid View"
                )
            }
        }
        Divider(color = ext.dividerColor)
        if (items.isEmpty()) {
            EmptyStateView("No Artists Found", "Scan audio files or clear search filter.", icon = Icons.Default.Person)
        } else if (gridMode) {
            val selectionActive = selectedKeys.isNotEmpty()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isCompact) 136.dp else 156.dp),
                contentPadding = PaddingValues(hPad),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md)
            ) {
                items(items, key = { "artg_${isAlbumArtist}_${it.artist}" }, contentType = { "artist_grid" }) { artist ->
                    val isSelected = artist.artist in selectedKeys
                    ArtistCardGridItem(
                        artist = artist,
                        isSelected = isSelected,
                        selectionActive = selectionActive,
                        onClick = {
                            if (selectionActive) onToggleSelect(artist.artist) else onOpen(artist)
                        },
                        onLongClick = { onToggleSelect(artist.artist) },
                        onToggleCheck = { onToggleSelect(artist.artist) },
                        onMoreClick = { onMore(artist) }
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items, key = { "art_${isAlbumArtist}_${it.artist}" }) { artist ->
                    val isSelected = artist.artist in selectedKeys
                    ArtistRowItem(
                        artist = artist,
                        isSelected = isSelected,
                        selectionMode = selectedKeys.isNotEmpty(),
                        onClick = {
                            if (selectedKeys.isNotEmpty()) onToggleSelect(artist.artist) else onOpen(artist)
                        },
                        onLongClick = { onToggleSelect(artist.artist) },
                        onMoreClick = { onMore(artist) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackCollectionDetailScreen(
    title: String,
    subtitle: String,
    tracks: List<TrackEntity>,
    currentTrackId: Long?,
    onBack: () -> Unit,
    onPlayAll: (Int, Boolean) -> Unit,
    onPlayNext: (List<TrackEntity>) -> Unit,
    onQueueTracks: (List<TrackEntity>) -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
    onBatchTagEdit: (List<Long>) -> Unit,
    onManageUserTags: (List<Long>) -> Unit,
    onShareTracks: (List<TrackEntity>) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onOpenArtist: ((String) -> Unit)? = null,
    onOpenAlbum: ((String, String) -> Unit)? = null,
    onRemoveSelectedFromCollection: ((Set<Long>) -> Unit)? = null,
    onEditSmartRules: (() -> Unit)? = null,
    onSaveAsPlaylist: (() -> Unit)? = null
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var menuOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (selectedIds.isNotEmpty()) {
            CollectionBatchActionBar(
                selectedCount = selectedIds.size,
                onCancel = { selectedIds = emptySet() },
                onSelectAll = { selectedIds = tracks.map { it.id }.toSet() },
                onPlayNext = {
                    onPlayNext(tracks.filter { it.id in selectedIds })
                    selectedIds = emptySet()
                },
                onAddToQueue = {
                    onQueueTracks(tracks.filter { it.id in selectedIds })
                    selectedIds = emptySet()
                },
                onAddToPlaylist = {
                    onAddToPlaylist(selectedIds.toList())
                    selectedIds = emptySet()
                },
                onBatchTagEdit = {
                    onBatchTagEdit(selectedIds.toList())
                    selectedIds = emptySet()
                },
                onManageUserTags = {
                    onManageUserTags(selectedIds.toList())
                    selectedIds = emptySet()
                },
                onShare = {
                    onShareTracks(tracks.filter { it.id in selectedIds })
                    selectedIds = emptySet()
                },
                onRemove = if (onRemoveSelectedFromCollection != null) {
                    {
                        onRemoveSelectedFromCollection(selectedIds)
                        selectedIds = emptySet()
                    }
                } else null,
                removeLabel = "Remove from Playlist"
            )
        } else {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, style = MaterialTheme.typography.caption.tabular(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onSaveAsPlaylist != null && tracks.isNotEmpty()) {
                        IconButton(onClick = onSaveAsPlaylist) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Save Mix as Playlist")
                        }
                    }
                    if (tracks.isNotEmpty()) {
                        IconButton(onClick = { onQueueTracks(tracks) }) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add All to Queue")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Collection actions")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (onSaveAsPlaylist != null && tracks.isNotEmpty()) {
                                DropdownMenuItem(onClick = { menuOpen = false; onSaveAsPlaylist() }) {
                                    Text("Save Mix as Playlist")
                                }
                            }
                            if (onEditSmartRules != null) {
                                DropdownMenuItem(onClick = { menuOpen = false; onEditSmartRules() }) {
                                    Text("Edit Smart Rules")
                                }
                            }
                            if (tracks.isNotEmpty()) {
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    selectedIds = tracks.map { it.id }.toSet()
                                }) { Text("Select All Tracks") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onPlayNext(tracks)
                                }) { Text("Play All Next") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onAddToPlaylist(tracks.map { it.id })
                                }) { Text("Add All to Playlist") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onBatchTagEdit(tracks.map { it.id })
                                }) { Text("Batch Edit Tags") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onShareTracks(tracks)
                                }) { Text("Share Tracks") }
                            }
                        }
                    }
                }
            )
            Divider(color = ext.dividerColor)
        }

        if (tracks.isEmpty()) {
            EmptyStateView(
                title = "No Tracks",
                subtitle = "No matching audio files in this collection.",
                actionLabel = if (onEditSmartRules != null) "Edit Rules" else null,
                onAction = onEditSmartRules
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ext.elevatedSurface)
                    .padding(horizontal = hPad, vertical = CadenceSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
            ) {
                Button(
                    onClick = { onPlayAll(0, false) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    SingleLineButtonLabel("PLAY ALL")
                }
                OutlinedButton(
                    onClick = { onPlayAll(0, true) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    SingleLineButtonLabel("SHUFFLE")
                }
            }
            Divider(color = ext.dividerColor)
            val detailListState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(state = detailListState, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(tracks, key = { _, t -> "det_${t.id}" }, contentType = { _, _ -> "track" }) { idx, track ->
                        val isSelected = track.id in selectedIds
                        TrackListItem(
                            track = track,
                            isPlayingCurrent = currentTrackId == track.id,
                            isSelected = isSelected,
                            selectionMode = selectedIds.isNotEmpty(),
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    selectedIds = if (isSelected) selectedIds - track.id else selectedIds + track.id
                                } else {
                                    onPlayAll(idx, false)
                                }
                            },
                            onLongClick = {
                                selectedIds = if (isSelected) selectedIds - track.id else selectedIds + track.id
                            },
                            onMoreClick = { onTrackMore(track) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onArtistClick = onOpenArtist,
                            onAlbumClick = onOpenAlbum
                        )
                    }
                }
                FastScrollLazyListOverlay(
                    listState = detailListState,
                    itemCount = tracks.size,
                    labelForIndex = { idx -> "#${idx + 1}" }
                )
            }
        }
    }
}
