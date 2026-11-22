package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.util.TextNormalizer
import com.example.data.db.TrackEntity
import com.example.ui.components.CollectionBatchActionBar
import com.example.ui.components.CollectionListRow
import com.example.ui.components.CollectionSheetTarget
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FastScrollLazyListOverlay
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackListItem
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    albumArtist: String,
    viewModel: MainViewModel,
    currentTrackId: Long?,
    onBack: () -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onOpenArtist: (String, Boolean) -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
    onBatchTagEdit: (List<Long>) -> Unit,
    onManageUserTags: (List<Long>) -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onAlbumMore: (CollectionSheetTarget.Album) -> Unit
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val tracks by remember(albumName, albumArtist) {
        viewModel.observeTracksForScreen(
            "album_$albumName",
            extraWhere = "album = ? AND COALESCE(NULLIF(albumArtist, ''), artist) = ?",
            extraArgs = listOf(albumName, albumArtist)
        )
    }.collectAsState(initial = emptyList())

    val sortedTracks = remember(tracks) {
        tracks.sortedWith(
            compareBy<TrackEntity> { it.discNumber.coerceAtLeast(1) }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )
    }

    val artistAlbums by remember(albumArtist) {
        viewModel.observeAlbumsForArtist(albumArtist, isAlbumArtist = true)
    }.collectAsState(initial = emptyList())
    val moreByArtist = remember(artistAlbums, albumName) {
        artistAlbums.filterNot { it.album.equals(albumName, ignoreCase = true) }
    }

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var menuOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
    }

    val coverArtwork = remember(sortedTracks) { sortedTracks.firstNotNullOfOrNull { it.artworkUri } }
    val fallbackUri = remember(sortedTracks) { sortedTracks.firstOrNull()?.contentUri }
    val year = remember(sortedTracks) { sortedTracks.maxOfOrNull { it.year } ?: 0 }
    val totalDurationMs = remember(sortedTracks) { sortedTracks.sumOf { it.durationMs } }
    val formatLabel = remember(sortedTracks) {
        sortedTracks.map { it.format }.filter { it.isNotBlank() }.distinct().take(2).joinToString(" / ")
    }
    val hasMultiDisc = remember(sortedTracks) {
        sortedTracks.map { it.discNumber.coerceAtLeast(1) }.distinct().size > 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("album_detail_screen")
    ) {
        if (selectedIds.isNotEmpty()) {
            CollectionBatchActionBar(
                selectedCount = selectedIds.size,
                onCancel = { selectedIds = emptySet() },
                onSelectAll = { selectedIds = sortedTracks.map { it.id }.toSet() },
                onPlayNext = {
                    val chosen = sortedTracks.filter { it.id in selectedIds }
                    viewModel.container.playerConnection.addPlayNext(chosen)
                    viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                    selectedIds = emptySet()
                },
                onAddToQueue = {
                    val chosen = sortedTracks.filter { it.id in selectedIds }
                    viewModel.container.playerConnection.addToEnd(chosen)
                    viewModel.showMessage("Queued ${chosen.size} track(s).")
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
                    viewModel.shareTracks(sortedTracks.filter { it.id in selectedIds })
                    selectedIds = emptySet()
                }
            )
        } else {
            TopAppBar(
                title = {
                    Text(albumName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (sortedTracks.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.container.playerConnection.addToEnd(sortedTracks)
                            viewModel.showMessage("Added ${sortedTracks.size} track(s) to queue.")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add Album to Queue")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Album options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(onClick = {
                                menuOpen = false
                                onOpenArtist(albumArtist, true)
                            }) { Text("Go to Artist") }
                            if (sortedTracks.isNotEmpty()) {
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    selectedIds = sortedTracks.map { it.id }.toSet()
                                }) { Text("Select All Tracks") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    viewModel.container.playerConnection.addPlayNext(sortedTracks)
                                    viewModel.showMessage("Queued album next.")
                                }) { Text("Play Album Next") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onAddToPlaylist(sortedTracks.map { it.id })
                                }) { Text("Add Album to Playlist") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onBatchTagEdit(sortedTracks.map { it.id })
                                }) { Text("Batch Edit Tags") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    viewModel.shareTracks(sortedTracks)
                                }) { Text("Share Album Tracks") }
                            }
                        }
                    }
                }
            )
        }

        if (sortedTracks.isEmpty()) {
            EmptyStateView(
                title = "No Tracks Found",
                subtitle = "No audio tracks currently match this album.",
                icon = Icons.Default.Album
            )
        } else {
            val listState = rememberLazyListState()
            val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
            val artDim = if (isCompact) 86.dp else 104.dp

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = CadenceSpacing.xl)
                ) {
                    item(key = "album_hero") {
                        Surface(
                            color = ext.elevatedSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TrackArtwork(
                                        artworkUri = coverArtwork,
                                        contentUri = fallbackUri,
                                        contentDescription = albumName,
                                        highRes = true,
                                        modifier = Modifier.size(artDim)
                                    )
                                    Spacer(Modifier.width(CadenceSpacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = albumName,
                                            style = MaterialTheme.typography.h6,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable { onOpenArtist(albumArtist, true) }
                                                .padding(vertical = 2.dp)
                                                .testTag("album_hero_artist_link")
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = albumArtist,
                                                style = MaterialTheme.typography.subtitle2,
                                                color = MaterialTheme.colors.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (year > 0) MetadataBadge(year.toString())
                                            MetadataBadge("${sortedTracks.size} tracks")
                                            MetadataBadge(TextNormalizer.formatDuration(totalDurationMs))
                                            if (formatLabel.isNotBlank()) MetadataBadge(formatLabel)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(CadenceSpacing.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                                ) {
                                    Button(
                                        onClick = { viewModel.container.playerConnection.playTracks(sortedTracks, 0, false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f).testTag("album_play_all")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        SingleLineButtonLabel("PLAY ALL")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.container.playerConnection.playTracks(sortedTracks.shuffled(), 0, true) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f).testTag("album_shuffle")
                                    ) {
                                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        SingleLineButtonLabel("SHUFFLE")
                                    }
                                    OutlinedButton(
                                        onClick = { onAddToPlaylist(sortedTracks.map { it.id }) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add Album to Playlist", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Divider(color = ext.dividerColor)
                    }

                    var lastDisc = -1
                    sortedTracks.forEachIndexed { idx, track ->
                        val disc = track.discNumber.coerceAtLeast(1)
                        if (hasMultiDisc && disc != lastDisc) {
                            lastDisc = disc
                            item(key = "disc_hdr_$disc") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = hPad, vertical = CadenceSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DiscFull,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Disc $disc",
                                        style = MaterialTheme.typography.subtitle2,
                                        color = MaterialTheme.colors.primary
                                    )
                                }
                                Divider(color = ext.dividerColor)
                            }
                        }
                        item(key = "alb_tr_${track.id}", contentType = "track") {
                            val isSelected = track.id in selectedIds
                            TrackListItem(
                                track = track,
                                isPlayingCurrent = currentTrackId == track.id,
                                isSelected = isSelected,
                                selectionMode = selectedIds.isNotEmpty(),
                                leadingNumber = if (track.trackNumber > 0) "${track.trackNumber}" else "${idx + 1}",
                                hideAlbumInSubtitle = true,
                                onArtistClick = { artist -> onOpenArtist(artist, false) },
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        selectedIds = if (isSelected) selectedIds - track.id else selectedIds + track.id
                                    } else {
                                        viewModel.container.playerConnection.playTracks(sortedTracks, idx, false)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = if (isSelected) selectedIds - track.id else selectedIds + track.id
                                },
                                onMoreClick = { onTrackMore(track) },
                                onToggleFavorite = { viewModel.toggleFavorite(track) }
                            )
                        }
                    }

                    if (moreByArtist.isNotEmpty()) {
                        item(key = "more_by_artist") {
                            Column(modifier = Modifier.padding(top = CadenceSpacing.lg)) {
                                Text(
                                    text = "More by $albumArtist",
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.sm)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = hPad),
                                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                                ) {
                                    items(moreByArtist, key = { "more_alb_${it.album}_${it.albumArtist}" }) { other ->
                                        val cardWidth = if (isCompact) 124.dp else 140.dp
                                        Surface(
                                            shape = RoundedCornerShape(CadenceRadius.md),
                                            color = ext.elevatedSurface,
                                            border = BorderStroke(0.75.dp, ext.subtleBorder),
                                            modifier = Modifier
                                                .width(cardWidth)
                                                .combinedClickable(
                                                    onClick = { onOpenAlbum(other.album, other.albumArtist) },
                                                    onLongClick = { onAlbumMore(CollectionSheetTarget.Album(other)) }
                                                )
                                        ) {
                                            Column {
                                                TrackArtwork(other.artworkUri, null, other.album, Modifier.size(cardWidth), cornerRadius = 0.dp)
                                                Row(
                                                    modifier = Modifier.padding(start = CadenceSpacing.sm, end = 2.dp, top = 6.dp, bottom = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(other.album, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        val yr = if (other.year > 0) "${other.year} • " else ""
                                                        Text(
                                                            "$yr${other.trackCount} tracks",
                                                            style = MaterialTheme.typography.caption.tabular(),
                                                            color = ext.subtleText,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onAlbumMore(CollectionSheetTarget.Album(other)) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.MoreVert, contentDescription = "Album actions", tint = ext.subtleText, modifier = Modifier.size(18.dp))
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
                FastScrollLazyListOverlay(
                    listState = listState,
                    itemCount = sortedTracks.size + 1,
                    labelForIndex = { idx ->
                        val tIdx = (idx - 1).coerceIn(0, sortedTracks.lastIndex)
                        "#${tIdx + 1}"
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    isAlbumArtist: Boolean,
    viewModel: MainViewModel,
    currentTrackId: Long?,
    onBack: () -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
    onBatchTagEdit: (List<Long>) -> Unit,
    onManageUserTags: (List<Long>) -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onAlbumMore: (CollectionSheetTarget.Album) -> Unit
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val tracks by remember(artistName, isAlbumArtist) {
        val where = if (isAlbumArtist) {
            "LOWER(TRIM(COALESCE(NULLIF(albumArtist, ''), artist))) = LOWER(TRIM(?))"
        } else {
            "(LOWER(TRIM(artist)) = LOWER(TRIM(?)) OR LOWER(TRIM(albumArtist)) = LOWER(TRIM(?)))"
        }
        val args = if (isAlbumArtist) listOf(artistName) else listOf(artistName, artistName)
        viewModel.observeTracksForScreen(
            "artist_$artistName",
            extraWhere = where,
            extraArgs = args
        )
    }.collectAsState(initial = emptyList())

    val albums by remember(artistName, isAlbumArtist) {
        viewModel.observeAlbumsForArtist(artistName, isAlbumArtist)
    }.collectAsState(initial = emptyList())

    var albumsAsList by rememberSaveable { mutableStateOf(true) }
    var trackSortMode by rememberSaveable { mutableIntStateOf(0) } // 0=Album, 1=Title, 2=Most Played
    val displayedTracks = remember(tracks, trackSortMode) {
        when (trackSortMode) {
            1 -> tracks.sortedBy { it.title.lowercase() }
            2 -> tracks.sortedWith(compareByDescending<TrackEntity> { it.playCount }.thenBy { it.title.lowercase() })
            else -> tracks.sortedWith(
                compareBy<TrackEntity> { it.album.lowercase() }
                    .thenBy { it.discNumber.coerceAtLeast(1) }
                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
            )
        }
    }

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var menuOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
    }

    val heroArtwork = remember(albums, tracks) {
        albums.firstNotNullOfOrNull { it.artworkUri } ?: tracks.firstNotNullOfOrNull { it.artworkUri }
    }
    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs } }
    val topGenres = remember(tracks) {
        tracks.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct().take(2)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("artist_detail_screen")
    ) {
        if (selectedIds.isNotEmpty()) {
            CollectionBatchActionBar(
                selectedCount = selectedIds.size,
                onCancel = { selectedIds = emptySet() },
                onSelectAll = { selectedIds = displayedTracks.map { it.id }.toSet() },
                onPlayNext = {
                    val chosen = displayedTracks.filter { it.id in selectedIds }
                    viewModel.container.playerConnection.addPlayNext(chosen)
                    viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                    selectedIds = emptySet()
                },
                onAddToQueue = {
                    val chosen = displayedTracks.filter { it.id in selectedIds }
                    viewModel.container.playerConnection.addToEnd(chosen)
                    viewModel.showMessage("Queued ${chosen.size} track(s).")
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
                    viewModel.shareTracks(displayedTracks.filter { it.id in selectedIds })
                    selectedIds = emptySet()
                }
            )
        } else {
            TopAppBar(
                title = { Text(artistName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (displayedTracks.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.container.playerConnection.addToEnd(displayedTracks)
                            viewModel.showMessage("Added ${displayedTracks.size} track(s) to queue.")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add Artist Tracks to Queue")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Artist options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (displayedTracks.isNotEmpty()) {
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    selectedIds = displayedTracks.map { it.id }.toSet()
                                }) { Text("Select All Tracks") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    viewModel.container.playerConnection.addPlayNext(displayedTracks)
                                    viewModel.showMessage("Queued artist tracks next.")
                                }) { Text("Play All Next") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onAddToPlaylist(displayedTracks.map { it.id })
                                }) { Text("Add All to Playlist") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    onBatchTagEdit(displayedTracks.map { it.id })
                                }) { Text("Batch Edit Tags") }
                                DropdownMenuItem(onClick = {
                                    menuOpen = false
                                    viewModel.shareTracks(displayedTracks)
                                }) { Text("Share Tracks") }
                            }
                        }
                    }
                }
            )
        }

        if (tracks.isEmpty()) {
            EmptyStateView(
                title = "No Tracks Found",
                subtitle = "No audio tracks currently match this artist.",
                icon = Icons.Default.Person
            )
        } else {
            val listState = rememberLazyListState()
            val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
            val avatarSize = if (isCompact) 80.dp else 96.dp
            val albumsHeaderCount = if (albums.isNotEmpty()) {
                if (albumsAsList) albums.size + 1 else 1
            } else 0

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = CadenceSpacing.xl)
                ) {
                    item(key = "artist_hero") {
                        Surface(
                            color = ext.elevatedSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TrackArtwork(
                                        artworkUri = heroArtwork,
                                        contentUri = null,
                                        contentDescription = artistName,
                                        isCircular = true,
                                        highRes = true,
                                        modifier = Modifier.size(avatarSize)
                                    )
                                    Spacer(Modifier.width(CadenceSpacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artistName,
                                            style = MaterialTheme.typography.h6,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            MetadataBadge("${albums.size} albums")
                                            MetadataBadge("${tracks.size} tracks")
                                            MetadataBadge(TextNormalizer.formatDuration(totalDurationMs))
                                            topGenres.forEach { g -> MetadataBadge(g) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(CadenceSpacing.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                                ) {
                                    Button(
                                        onClick = { viewModel.container.playerConnection.playTracks(displayedTracks, 0, false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f).testTag("artist_play_all")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        SingleLineButtonLabel("PLAY ALL")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.container.playerConnection.playTracks(displayedTracks.shuffled(), 0, true) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f).testTag("artist_shuffle")
                                    ) {
                                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        SingleLineButtonLabel("SHUFFLE")
                                    }
                                    OutlinedButton(
                                        onClick = { onAddToPlaylist(displayedTracks.map { it.id }) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add All to Playlist", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Divider(color = ext.dividerColor)
                    }

                    if (albums.isNotEmpty()) {
                        item(key = "artist_albums_hdr") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = hPad, end = CadenceSpacing.xs, top = CadenceSpacing.sm, bottom = CadenceSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Albums (${albums.size})",
                                    style = MaterialTheme.typography.h6.tabular(),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { albumsAsList = !albumsAsList },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (albumsAsList) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                        contentDescription = if (albumsAsList) "Switch Albums to Grid" else "Switch Albums to List"
                                    )
                                }
                            }
                        }

                        if (albumsAsList) {
                            items(albums, key = { "art_albl_${it.album}_${it.albumArtist}" }) { album ->
                                val yr = if (album.year > 0) "${album.year} • " else ""
                                CollectionListRow(
                                    title = album.album,
                                    subtitle = "$yr${album.trackCount} tracks",
                                    artworkUri = album.artworkUri,
                                    leadingIcon = if (album.artworkUri == null) Icons.Default.Album else null,
                                    onClick = { onOpenAlbum(album.album, album.albumArtist) },
                                    onLongClick = { onAlbumMore(CollectionSheetTarget.Album(album)) },
                                    onMoreClick = { onAlbumMore(CollectionSheetTarget.Album(album)) }
                                )
                            }
                        } else {
                            item(key = "artist_albums_shelf") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = hPad, vertical = CadenceSpacing.xs),
                                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                                ) {
                                    items(albums, key = { "art_alb_${it.album}_${it.albumArtist}" }) { album ->
                                        val cardWidth = if (isCompact) 124.dp else 140.dp
                                        Surface(
                                            shape = RoundedCornerShape(CadenceRadius.md),
                                            color = ext.elevatedSurface,
                                            border = BorderStroke(0.75.dp, ext.subtleBorder),
                                            modifier = Modifier
                                                .width(cardWidth)
                                                .combinedClickable(
                                                    onClick = { onOpenAlbum(album.album, album.albumArtist) },
                                                    onLongClick = { onAlbumMore(CollectionSheetTarget.Album(album)) }
                                                )
                                        ) {
                                            Column {
                                                TrackArtwork(album.artworkUri, null, album.album, Modifier.size(cardWidth), cornerRadius = 0.dp)
                                                Row(
                                                    modifier = Modifier.padding(start = CadenceSpacing.sm, end = 2.dp, top = 6.dp, bottom = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(album.album, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        val yr = if (album.year > 0) "${album.year} • " else ""
                                                        Text(
                                                            "$yr${album.trackCount} tracks",
                                                            style = MaterialTheme.typography.caption.tabular(),
                                                            color = ext.subtleText,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onAlbumMore(CollectionSheetTarget.Album(album)) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.MoreVert, contentDescription = "Album actions", tint = ext.subtleText, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "artist_tracks_header") {
                        Column(modifier = Modifier.padding(top = CadenceSpacing.sm)) {
                            Text(
                                text = "Tracks (${displayedTracks.size})",
                                style = MaterialTheme.typography.h6.tabular(),
                                modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.xs)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = hPad, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
                            ) {
                                itemsIndexed(listOf("By Album", "Title", "Most Played")) { idx, label ->
                                    FilterChip(
                                        selected = trackSortMode == idx,
                                        onClick = { trackSortMode = idx }
                                    ) {
                                        SingleLineButtonLabel(label, style = MaterialTheme.typography.caption)
                                    }
                                }
                            }
                            Divider(color = ext.dividerColor, modifier = Modifier.padding(top = CadenceSpacing.xs))
                        }
                    }

                    itemsIndexed(displayedTracks, key = { _, t -> "art_tr_${t.id}" }, contentType = { _, _ -> "track" }) { idx, track ->
                        val isSelected = track.id in selectedIds
                        TrackListItem(
                            track = track,
                            isPlayingCurrent = currentTrackId == track.id,
                            isSelected = isSelected,
                            selectionMode = selectedIds.isNotEmpty(),
                            hideArtistInSubtitle = true,
                            onAlbumClick = onOpenAlbum,
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    selectedIds = if (isSelected) selectedIds - track.id else selectedIds + track.id
                                } else {
                                    viewModel.container.playerConnection.playTracks(displayedTracks, idx, false)
                                }
                            },
                            onLongClick = {
                                selectedIds = if (isSelected) selectedIds - track.id else selectedIds + track.id
                            },
                            onMoreClick = { onTrackMore(track) },
                            onToggleFavorite = { viewModel.toggleFavorite(track) }
                        )
                    }
                }
                val headerItems = 1 + albumsHeaderCount + 1
                FastScrollLazyListOverlay(
                    listState = listState,
                    itemCount = displayedTracks.size + headerItems,
                    labelForIndex = { idx ->
                        val tIdx = (idx - headerItems).coerceIn(0, displayedTracks.lastIndex)
                        displayedTracks.getOrNull(tIdx)?.title?.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                    }
                )
            }
        }
    }
}

@Composable
private fun MetadataBadge(text: String) {
    val ext = LocalCadenceTheme.current
    Surface(
        shape = MaterialTheme.shapes.small,
        color = ext.tonalSurface,
        border = BorderStroke(0.5.dp, ext.subtleBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.overline.tabular(),
            color = MaterialTheme.colors.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
