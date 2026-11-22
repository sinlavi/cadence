package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Chip
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.model.GeneratedSmartMix
import com.example.core.model.HomeSectionKey
import com.example.core.model.LibraryTabKey
import com.example.core.model.RatingSystemMode
import com.example.core.util.TextNormalizer
import com.example.data.db.AlbumSummary
import com.example.data.db.FolderSummary
import com.example.data.db.PlaylistWithCount
import com.example.data.db.TrackEntity
import com.example.ui.components.ArtistRowItem
import com.example.ui.components.CadenceInlineSearchBar
import com.example.ui.components.CollectionListRow
import com.example.ui.components.CollectionSheetTarget
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InlineArtistAlbumMeta
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackListItem
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.LocalRatingSystemMode
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onPickSafFolder: () -> Unit,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onOpenLibraryTab: (LibraryTabKey) -> Unit,
    onOpenGeneratedMix: (GeneratedSmartMix) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onAlbumMore: (AlbumSummary) -> Unit,
    onPlaylistMore: (PlaylistWithCount) -> Unit,
    onCollectionMore: (CollectionSheetTarget) -> Unit
) {
    val shelves by viewModel.homeShelves.collectAsState()
    val personalizedMixes by viewModel.personalizedMixes.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val stats by viewModel.libraryStats.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val queue by viewModel.queueItems.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchMatches by viewModel.searchCollectionMatches.collectAsState()
    var selectedScope by rememberSaveable { mutableIntStateOf(0) }
    val scopeLabels = listOf("All", "Songs", "Albums", "Artists", "Playlists", "Folders")

    val ext = LocalCadenceTheme.current
    val ratingMode = LocalRatingSystemMode.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    val continueTracks = shelves.continueTracks
    val recentPlayed = shelves.recentPlayed
    val recentAdded = shelves.recentAdded
    val mostPlayed = shelves.mostPlayed
    val favorites = shelves.favorites
    val forgottenGems = shelves.forgottenGems
    val localDiscoveryMix = shelves.localDiscoveryMix
    val unfinishedAlbums = shelves.unfinishedAlbums
    val totalTrackCount = stats?.totalTracks ?: 0

    val visibleSections = remember(settings.homeSectionOrder, settings.hiddenHomeSections) {
        settings.homeSectionOrder.filter { it !in settings.hiddenHomeSections }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        CadenceInlineSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            placeholder = "Search songs, artists, albums, folders...",
            onSubmit = { viewModel.commitSearchToHistory(searchQuery) }
        )

        if (searchQuery.isNotBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = hPad, vertical = CadenceSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
            ) {
                itemsIndexed(scopeLabels) { idx, label ->
                    FilterChip(
                        selected = selectedScope == idx,
                        onClick = { selectedScope = idx }
                    ) {
                        SingleLineButtonLabel(label, style = MaterialTheme.typography.caption)
                    }
                }
            }
            Divider(color = ext.dividerColor)

            val matchedArtists = if (selectedScope == 0 || selectedScope == 3) searchMatches.artists else emptyList()
            val matchedAlbums = if (selectedScope == 0 || selectedScope == 2) searchMatches.albums else emptyList()
            val matchedPlaylists = if (selectedScope == 0 || selectedScope == 4) searchMatches.playlists else emptyList()
            val matchedFolders = if (selectedScope == 0 || selectedScope == 5) searchMatches.folders else emptyList()
            val matchedSongs = if (selectedScope == 0 || selectedScope == 1) searchResults else emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = CadenceSpacing.xl)
            ) {
                searchResultsItems(
                    query = searchQuery,
                    selectedScope = selectedScope,
                    matchedArtists = matchedArtists,
                    matchedAlbums = matchedAlbums,
                    matchedPlaylists = matchedPlaylists,
                    matchedFolders = matchedFolders,
                    matchedSongs = matchedSongs,
                    currentTrackId = playback.currentTrack?.id,
                    hPad = hPad,
                    onCommitSearch = { viewModel.commitSearchToHistory(searchQuery) },
                    onClearSearch = { viewModel.updateSearchQuery("") },
                    onPlaySongs = { list, idx ->
                        viewModel.container.playerConnection.playTracks(list, idx)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenFolder = onOpenFolder,
                    onTrackMore = onTrackMore,
                    onCollectionMore = onCollectionMore
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = CadenceSpacing.xl)
            ) {
                if (!hasAudioPermission) {
                    item(key = "home_permission") {
                        Surface(
                            color = ext.elevatedSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(CadenceSpacing.sm))
                                    Text("Offline Music Access", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Grant local audio access to index your tracks, albums, folders, and embedded tags, or mount folders via SAF.",
                                    style = MaterialTheme.typography.body2,
                                    color = ext.subtleText
                                )
                                Spacer(Modifier.height(CadenceSpacing.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                                ) {
                                    Button(
                                        onClick = onRequestAudioPermission,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("grant_permission_button")
                                    ) {
                                        SingleLineButtonLabel("GRANT ACCESS")
                                    }
                                    OutlinedButton(
                                        onClick = onPickSafFolder,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        SingleLineButtonLabel("ADD FOLDER")
                                    }
                                }
                            }
                        }
                        Divider(color = ext.dividerColor)
                    }
                }

                if (scanState.isScanning) {
                    item(key = "home_scanning") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ext.elevatedSurface)
                                .padding(horizontal = hPad, vertical = CadenceSpacing.sm)
                        ) {
                            Text(
                                scanState.statusText.ifBlank { "Scanning library..." },
                                style = MaterialTheme.typography.caption,
                                color = ext.subtleText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(CadenceSpacing.xs))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Divider(color = ext.dividerColor)
                    }
                }

                // Editorial Studio Library Header Band (Full-Width, Subtle Radial Aura + Brand Emblem)
                item(key = "home_header", contentType = "home_header") {
                    val primaryGlow = MaterialTheme.colors.primary.copy(alpha = if (ext.isDark) 0.14f else 0.08f)
                    val secondaryGlow = MaterialTheme.colors.secondary.copy(alpha = if (ext.isDark) 0.10f else 0.05f)
                    Surface(
                        color = ext.elevatedSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(primaryGlow, Color.Transparent),
                                            center = Offset(size.width * 0.12f, size.height * 0.15f),
                                            radius = size.maxDimension * 0.65f
                                        )
                                    )
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(secondaryGlow, Color.Transparent),
                                            center = Offset(size.width * 0.92f, size.height * 0.85f),
                                            radius = size.maxDimension * 0.55f
                                        )
                                    )
                                }
                                .padding(horizontal = hPad, vertical = CadenceSpacing.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.cadence_logo_mark_1795348934126),
                                    contentDescription = "Cadence Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(if (isCompact) 38.dp else 44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(CadenceSpacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CADENCE LIBRARY",
                                        style = MaterialTheme.typography.overline,
                                        color = MaterialTheme.colors.primary
                                    )
                                    Text(
                                        text = "$totalTrackCount Tracks • ${stats?.totalAlbums ?: albums.size} Albums",
                                        style = MaterialTheme.typography.h6.tabular(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val durText = TextNormalizer.formatDuration(stats?.totalDurationMs ?: 0L)
                                    val sizeText = TextNormalizer.formatFileSize(stats?.totalSizeBytes ?: 0L)
                                    Text(
                                        text = "$durText total • $sizeText",
                                        style = MaterialTheme.typography.caption.tabular(),
                                        color = ext.subtleText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.triggerLibraryScan() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_rescan_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Rescan Library", tint = MaterialTheme.colors.primary)
                                }
                                IconButton(
                                    onClick = onOpenEqualizer,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_quick_eq")
                                ) {
                                    Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = MaterialTheme.colors.primary)
                                }
                            }
                            Spacer(Modifier.height(CadenceSpacing.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                Button(
                                    onClick = {
                                        val pool = viewModel.allTracks.value
                                        if (pool.isNotEmpty()) {
                                            viewModel.container.playerConnection.playTracks(pool.shuffled(), 0, shuffle = true)
                                        }
                                    },
                                    enabled = totalTrackCount > 0,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_shuffle_all")
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    SingleLineButtonLabel("SHUFFLE ALL")
                                }
                                OutlinedButton(
                                    onClick = { onOpenLibraryTab(LibraryTabKey.SONGS) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_browse_all_tracks")
                                ) {
                                    Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    SingleLineButtonLabel("BROWSE ALL")
                                }
                            }
                        }
                    }
                    Divider(color = ext.dividerColor)
                }

                // Direct Library Category Shortcuts
                item(key = "home_library_shortcuts") {
                    val shortcuts = remember {
                        listOf(
                            Triple(LibraryTabKey.SONGS, "Songs", Icons.Default.MusicNote),
                            Triple(LibraryTabKey.ALBUMS, "Albums", Icons.Default.Album),
                            Triple(LibraryTabKey.ARTISTS, "Artists", Icons.Default.Person),
                            Triple(LibraryTabKey.FOLDERS, "Files", Icons.Default.Folder),
                            Triple(LibraryTabKey.GENRES, "Genres", Icons.Default.Category),
                            Triple(LibraryTabKey.YEARS, "Years", Icons.Default.CalendarToday),
                            Triple(LibraryTabKey.COMPOSERS, "Composers", Icons.Default.Person),
                            Triple(LibraryTabKey.COLLECTIONS, "Smart Views", Icons.Default.AutoAwesome)
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = hPad, vertical = CadenceSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                    ) {
                        items(shortcuts, key = { it.first.name }) { (tab, label, icon) ->
                            Surface(
                                shape = RoundedCornerShape(CadenceRadius.sm),
                                color = ext.elevatedSurface,
                                border = BorderStroke(1.dp, ext.subtleBorder),
                                modifier = Modifier
                                    .clickable { onOpenLibraryTab(tab) }
                                    .testTag("home_shortcut_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    SingleLineButtonLabel(
                                        text = label,
                                        style = MaterialTheme.typography.subtitle2
                                    )
                                }
                            }
                        }
                    }
                }

                if (totalTrackCount == 0 && !scanState.isScanning) {
                    item(key = "home_empty", contentType = "empty") {
                        EmptyStateView(
                            title = "No Tracks Indexed Yet",
                            subtitle = "Tap Scan Device Music after granting audio access or add a music folder with Storage Access Framework.",
                            actionLabel = "Scan Device Music",
                            onAction = { viewModel.triggerLibraryScan() },
                            icon = Icons.Default.LibraryMusic
                        )
                    }
                } else {
                    // Promoted Now Playing / Resume Spotlight Strip for instant Time-to-Resume
                    val spotlightTrack = playback.currentTrack ?: continueTracks.firstOrNull() ?: recentPlayed.firstOrNull()
                    if (spotlightTrack != null) {
                        val isCurrentPlayingTrack = playback.currentTrack?.id == spotlightTrack.id
                        item(key = "home_spotlight") {
                            Divider(color = ext.dividerColor)
                            Surface(
                                color = ext.elevatedSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isCurrentPlayingTrack) {
                                            onOpenNowPlaying()
                                        } else {
                                            viewModel.container.playerConnection.playTracks(listOf(spotlightTrack), 0)
                                        }
                                    }
                                    .testTag("home_spotlight_card")
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
                                            .padding(horizontal = hPad, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentPlayingTrack) Icons.Default.GraphicEq else Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isCurrentPlayingTrack) {
                                                if (playback.isPlaying) "NOW PLAYING" else "PAUSED IN PLAYER"
                                            } else {
                                                "PICK UP WHERE YOU LEFT OFF"
                                            },
                                            style = MaterialTheme.typography.overline,
                                            color = MaterialTheme.colors.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = spotlightTrack.format,
                                            style = MaterialTheme.typography.overline.tabular(),
                                            color = ext.subtleText,
                                            maxLines = 1
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = hPad, end = CadenceSpacing.xs, top = CadenceSpacing.sm, bottom = CadenceSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TrackArtwork(
                                            artworkUri = spotlightTrack.artworkUri,
                                            contentUri = spotlightTrack.contentUri,
                                            contentDescription = spotlightTrack.title,
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(Modifier.width(CadenceSpacing.md))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = spotlightTrack.title,
                                                style = MaterialTheme.typography.subtitle1,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            InlineArtistAlbumMeta(
                                                artist = spotlightTrack.artist,
                                                album = spotlightTrack.album,
                                                albumArtist = spotlightTrack.albumArtist.ifBlank { spotlightTrack.artist },
                                                clickableLinks = true,
                                                onArtistClick = onOpenArtist,
                                                onAlbumClick = { alb, albArt ->
                                                    onOpenAlbum(AlbumSummary(alb, albArt, spotlightTrack.albumId, spotlightTrack.artworkUri, 0, 0L, spotlightTrack.year))
                                                }
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (isCurrentPlayingTrack) {
                                                    viewModel.container.playerConnection.togglePlayPause()
                                                } else {
                                                    viewModel.container.playerConnection.playTracks(listOf(spotlightTrack), 0)
                                                }
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentPlayingTrack && playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play or Pause",
                                                tint = MaterialTheme.colors.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { onTrackMore(spotlightTrack) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Track actions", tint = ext.subtleText)
                                        }
                                    }
                                }
                            }
                            Divider(color = ext.dividerColor)
                            Spacer(Modifier.height(CadenceSpacing.xs))
                        }
                    }

                    // Quick-Access 2x2 Action Grid
                    item(key = "home_quick_grid") {
                        val scoredTitle = if (ratingMode == RatingSystemMode.STARS) "Top Rated" else "Favorites"
                        val scoredIcon = if (ratingMode == RatingSystemMode.STARS) Icons.Default.Star else Icons.Default.Favorite
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = hPad, vertical = CadenceSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                HomeQuickTile(
                                    title = if (queue.isNotEmpty()) "Active Queue" else "Resume",
                                    subtitle = if (queue.isNotEmpty()) "${queue.size} queued" else "${continueTracks.size} paused",
                                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                                    enabled = queue.isNotEmpty() || continueTracks.isNotEmpty() || recentPlayed.isNotEmpty(),
                                    onClick = {
                                        when {
                                            queue.isNotEmpty() -> onOpenQueue()
                                            continueTracks.isNotEmpty() -> viewModel.container.playerConnection.playTracks(continueTracks, 0)
                                            recentPlayed.isNotEmpty() -> viewModel.container.playerConnection.playTracks(recentPlayed, 0)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                HomeQuickTile(
                                    title = scoredTitle,
                                    subtitle = "${favorites.size} tracks",
                                    icon = scoredIcon,
                                    enabled = favorites.isNotEmpty(),
                                    onClick = {
                                        if (favorites.isNotEmpty()) {
                                            viewModel.container.playerConnection.playTracks(favorites, 0)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                            ) {
                                HomeQuickTile(
                                    title = "Recently Added",
                                    subtitle = "${recentAdded.size} tracks",
                                    icon = Icons.Default.LibraryAddCheck,
                                    enabled = recentAdded.isNotEmpty(),
                                    onClick = {
                                        if (recentAdded.isNotEmpty()) {
                                            viewModel.container.playerConnection.playTracks(recentAdded, 0)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                HomeQuickTile(
                                    title = "Most Played",
                                    subtitle = "${mostPlayed.size} tracks",
                                    icon = Icons.Default.TrendingUp,
                                    enabled = mostPlayed.isNotEmpty(),
                                    onClick = {
                                        if (mostPlayed.isNotEmpty()) {
                                            viewModel.container.playerConnection.playTracks(mostPlayed, 0)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Personalized Auto-Generated Smart Mixes
                    if (settings.homeShowGeneratedMixes && personalizedMixes.isNotEmpty()) {
                        item(key = "home_made_for_you") {
                            SectionHeaderRow(title = "Made For You", hPad = hPad)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = hPad),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md)
                            ) {
                                items(personalizedMixes, key = { "mix_${it.id}" }) { mix ->
                                    GeneratedMixShelfCard(
                                        mix = mix,
                                        onOpenMix = { onOpenGeneratedMix(mix) },
                                        onPlayMix = {
                                            val trackMap = viewModel.allTracks.value.associateBy { it.id }
                                            val mixTracks = mix.trackIds.mapNotNull { trackMap[it] }
                                            if (mixTracks.isNotEmpty()) {
                                                viewModel.container.playerConnection.playTracks(mixTracks, 0)
                                            }
                                        },
                                        onSaveMix = { viewModel.saveGeneratedMixAsPlaylist(mix) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(CadenceSpacing.sm))
                        }
                    }

                    visibleSections.forEach { section ->
                        val displayLabel = if (section == HomeSectionKey.FAVORITES && ratingMode == RatingSystemMode.STARS) {
                            "Top Rated"
                        } else {
                            section.label
                        }
                        when (section) {
                            HomeSectionKey.CONTINUE_LISTENING -> if (continueTracks.isNotEmpty()) {
                                item(key = "sec_continue") {
                                    HorizontalTrackShelf(
                                        title = displayLabel,
                                        tracks = continueTracks,
                                        onTrackClick = { idx -> viewModel.container.playerConnection.playTracks(continueTracks, idx) },
                                        onArtistClick = onOpenArtist,
                                        onTrackMore = onTrackMore
                                    )
                                }
                            }
                            HomeSectionKey.RECENTLY_PLAYED -> if (recentPlayed.isNotEmpty()) {
                                item(key = "sec_recent_played") {
                                    HorizontalTrackShelf(
                                        title = displayLabel,
                                        tracks = recentPlayed,
                                        onTrackClick = { idx -> viewModel.container.playerConnection.playTracks(recentPlayed, idx) },
                                        onArtistClick = onOpenArtist,
                                        onTrackMore = onTrackMore
                                    )
                                }
                            }
                            HomeSectionKey.RECENTLY_ADDED -> if (recentAdded.isNotEmpty()) {
                                item(key = "sec_recent_added") {
                                    HorizontalTrackShelf(
                                        title = displayLabel,
                                        tracks = recentAdded,
                                        onTrackClick = { idx -> viewModel.container.playerConnection.playTracks(recentAdded, idx) },
                                        onArtistClick = onOpenArtist,
                                        onTrackMore = onTrackMore
                                    )
                                }
                            }
                            HomeSectionKey.MOST_PLAYED -> if (mostPlayed.isNotEmpty()) {
                                item(key = "sec_most_played") {
                                    HorizontalTrackShelf(
                                        title = displayLabel,
                                        tracks = mostPlayed,
                                        onTrackClick = { idx -> viewModel.container.playerConnection.playTracks(mostPlayed, idx) },
                                        onArtistClick = onOpenArtist,
                                        onTrackMore = onTrackMore
                                    )
                                }
                            }
                            HomeSectionKey.FAVORITES -> if (favorites.isNotEmpty()) {
                                item(key = "sec_favorites") {
                                    HorizontalTrackShelf(
                                        title = displayLabel,
                                        tracks = favorites,
                                        onTrackClick = { idx -> viewModel.container.playerConnection.playTracks(favorites, idx) },
                                        onArtistClick = onOpenArtist,
                                        onTrackMore = onTrackMore
                                    )
                                }
                            }
                            HomeSectionKey.SUGGESTED_MIX -> if (localDiscoveryMix.isNotEmpty()) {
                                val mixPreview = localDiscoveryMix.take(5)
                                item(key = "sec_discovery_mix") {
                                    Spacer(Modifier.height(CadenceSpacing.sm))
                                    SectionHeaderRow(
                                        title = displayLabel,
                                        hPad = hPad,
                                        actionText = "PLAY MIX",
                                        onAction = { viewModel.container.playerConnection.playTracks(localDiscoveryMix, 0) }
                                    )
                                    Divider(color = ext.dividerColor)
                                    mixPreview.forEachIndexed { idx, track ->
                                        TrackListItem(
                                            track = track,
                                            isPlayingCurrent = playback.currentTrack?.id == track.id,
                                            onClick = { viewModel.container.playerConnection.playTracks(localDiscoveryMix, idx) },
                                            onLongClick = { onTrackMore(track) },
                                            onMoreClick = { onTrackMore(track) },
                                            onToggleFavorite = { viewModel.toggleFavorite(track) },
                                            onArtistClick = onOpenArtist,
                                            onAlbumClick = { alb, albArt ->
                                                onOpenAlbum(AlbumSummary(alb, albArt, track.albumId, track.artworkUri, 0, 0L, track.year))
                                            },
                                            showDivider = idx < mixPreview.lastIndex
                                        )
                                    }
                                    Divider(color = ext.dividerColor)
                                }
                            }
                            HomeSectionKey.UNFINISHED_ALBUMS -> if (unfinishedAlbums.isNotEmpty()) {
                                item(key = "sec_unfinished_albums") {
                                    SectionHeaderRow(title = displayLabel, hPad = hPad)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = hPad),
                                        horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                                    ) {
                                        items(unfinishedAlbums, key = { "${it.album}_${it.albumArtist}" }) { album ->
                                            HomeAlbumShelfItem(
                                                album = album,
                                                onClick = { onOpenAlbum(album) },
                                                onArtistClick = { onOpenArtist(album.albumArtist) },
                                                onMoreClick = { onAlbumMore(album) }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(CadenceSpacing.sm))
                                }
                            }
                            HomeSectionKey.FORGOTTEN_GEMS -> if (forgottenGems.isNotEmpty()) {
                                item(key = "sec_forgotten_gems") {
                                    HorizontalTrackShelf(
                                        title = displayLabel,
                                        tracks = forgottenGems,
                                        onTrackClick = { idx -> viewModel.container.playerConnection.playTracks(forgottenGems, idx) },
                                        onArtistClick = onOpenArtist,
                                        onTrackMore = onTrackMore
                                    )
                                }
                            }
                            HomeSectionKey.RECENT_PLAYLISTS -> if (playlists.isNotEmpty()) {
                                item(key = "sec_recent_playlists") {
                                    SectionHeaderRow(title = displayLabel, hPad = hPad)
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = hPad),
                                        horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                                    ) {
                                        items(playlists, key = { "home_pl_${it.id}" }) { pl ->
                                            PlaylistQuickTile(
                                                pl = pl,
                                                onClick = { onOpenPlaylist(pl.id) },
                                                onMoreClick = { onPlaylistMore(pl) }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(CadenceSpacing.sm))
                                }
                            }
                        }
                    }

                    // Full-Width Library Insights Strip
                    stats?.let { st ->
                        item(key = "home_insights_footer") {
                            Spacer(Modifier.height(CadenceSpacing.sm))
                            Divider(color = ext.dividerColor)
                            Surface(
                                color = ext.elevatedSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.md)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.BarChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(CadenceSpacing.xs))
                                        Text("Library Insights", style = MaterialTheme.typography.subtitle2)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Listen time: ${TextNormalizer.formatDuration(st.totalListenMs)} • Lyrics: ${st.tracksWithLyrics}/${st.totalTracks} • Artwork: ${st.tracksWithArtwork}/${st.totalTracks}",
                                        style = MaterialTheme.typography.caption.tabular(),
                                        color = ext.subtleText
                                    )
                                }
                            }
                            Divider(color = ext.dividerColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    Surface(
        shape = RoundedCornerShape(CadenceRadius.sm),
        color = ext.elevatedSurface,
        border = BorderStroke(1.dp, ext.subtleBorder),
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CadenceSpacing.sm, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(CadenceRadius.xs),
                color = if (enabled) MaterialTheme.colors.primary.copy(alpha = 0.14f) else ext.tonalSurface
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colors.primary else ext.subtleText,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(18.dp)
                )
            }
            Spacer(Modifier.width(CadenceSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle2,
                    color = if (enabled) MaterialTheme.colors.onSurface else ext.subtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption.tabular(),
                    color = ext.subtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GeneratedMixShelfCard(
    mix: GeneratedSmartMix,
    onOpenMix: () -> Unit,
    onPlayMix: () -> Unit,
    onSaveMix: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val cardWidth = if (isCompact) 144.dp else 160.dp

    Surface(
        shape = RoundedCornerShape(CadenceRadius.sm),
        color = ext.elevatedSurface,
        border = BorderStroke(1.dp, ext.subtleBorder),
        modifier = Modifier
            .width(cardWidth)
            .combinedClickable(onClick = onOpenMix, onLongClick = onSaveMix)
            .testTag("generated_mix_${mix.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardWidth)
            ) {
                TrackArtwork(
                    artworkUri = mix.artworkUri,
                    contentUri = mix.fallbackContentUri,
                    contentDescription = mix.title,
                    cornerRadius = 0.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(CadenceRadius.xs),
                    color = Color.Black.copy(alpha = 0.68f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = mix.badge,
                        style = MaterialTheme.typography.overline,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colors.primary,
                    elevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(34.dp)
                        .clickable(onClick = onPlayMix)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play ${mix.title}",
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = CadenceSpacing.sm, end = 2.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mix.title,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mix.subtitle,
                        style = MaterialTheme.typography.caption,
                        color = ext.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onSaveMix,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Save ${mix.title} as playlist",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    hPad: Dp = CadenceSpacing.lg,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = hPad, end = CadenceSpacing.xs, top = CadenceSpacing.sm, bottom = CadenceSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                SingleLineButtonLabel(actionText)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalTrackShelf(
    title: String,
    tracks: List<TrackEntity>,
    onTrackClick: (Int) -> Unit,
    onArtistClick: (String) -> Unit,
    onTrackMore: (TrackEntity) -> Unit
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val cardWidth = if (isCompact) 124.dp else 138.dp
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    Column(modifier = Modifier.padding(bottom = CadenceSpacing.xs)) {
        SectionHeaderRow(title = title, hPad = hPad, actionText = "Play All", onAction = { onTrackClick(0) })
        LazyRow(
            contentPadding = PaddingValues(horizontal = hPad),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md)
        ) {
            itemsIndexed(tracks, key = { _, t -> "${title}_${t.id}" }) { idx, track ->
                Surface(
                    shape = RoundedCornerShape(CadenceRadius.sm),
                    color = ext.elevatedSurface,
                    border = BorderStroke(1.dp, ext.subtleBorder),
                    modifier = Modifier
                        .width(cardWidth)
                        .combinedClickable(
                            onClick = { onTrackClick(idx) },
                            onLongClick = { onTrackMore(track) }
                        )
                ) {
                    Column {
                        TrackArtwork(track.artworkUri, track.contentUri, track.title, Modifier.size(cardWidth), cornerRadius = 0.dp)
                        Row(
                            modifier = Modifier.padding(start = CadenceSpacing.sm, end = 2.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.caption,
                                    color = ext.subtleText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onArtistClick(track.artist) }
                                )
                            }
                            IconButton(
                                onClick = { onTrackMore(track) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Track actions", tint = ext.subtleText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAlbumShelfItem(
    album: AlbumSummary,
    onClick: () -> Unit,
    onArtistClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    val cardWidth = if (LocalIsCompactWidth.current) 124.dp else 140.dp
    Surface(
        shape = RoundedCornerShape(CadenceRadius.sm),
        color = ext.elevatedSurface,
        border = BorderStroke(1.dp, ext.subtleBorder),
        modifier = Modifier
            .width(cardWidth)
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick)
    ) {
        Column {
            TrackArtwork(album.artworkUri, null, album.album, Modifier.size(cardWidth), cornerRadius = 0.dp)
            Row(
                modifier = Modifier.padding(start = CadenceSpacing.sm, end = 2.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(album.album, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = album.albumArtist,
                        style = MaterialTheme.typography.caption,
                        color = ext.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onArtistClick)
                    )
                }
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Album actions", tint = ext.subtleText, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistQuickTile(
    pl: PlaylistWithCount,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    val cardWidth = if (LocalIsCompactWidth.current) 156.dp else 180.dp
    Surface(
        shape = RoundedCornerShape(CadenceRadius.sm),
        color = ext.elevatedSurface,
        border = BorderStroke(1.dp, ext.subtleBorder),
        modifier = Modifier
            .width(cardWidth)
            .combinedClickable(onClick = onClick, onLongClick = onMoreClick)
    ) {
        Row(
            modifier = Modifier.padding(start = CadenceSpacing.md, top = CadenceSpacing.sm, bottom = CadenceSpacing.sm, end = CadenceSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pl.name, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${pl.trackCount} tracks", style = MaterialTheme.typography.caption.tabular(), color = ext.subtleText)
            }
            IconButton(onClick = onMoreClick, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.MoreVert, contentDescription = "Playlist actions", tint = ext.subtleText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onCollectionMore: (CollectionSheetTarget) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val matches by viewModel.searchCollectionMatches.collectAsState()
    val history by viewModel.searchHistory.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    var selectedScope by rememberSaveable { mutableIntStateOf(0) }
    val scopeLabels = listOf("All", "Songs", "Albums", "Artists", "Playlists", "Folders")

    val matchedArtists = if (selectedScope == 0 || selectedScope == 3) matches.artists else emptyList()
    val matchedAlbums = if (selectedScope == 0 || selectedScope == 2) matches.albums else emptyList()
    val matchedPlaylists = if (selectedScope == 0 || selectedScope == 4) matches.playlists else emptyList()
    val matchedFolders = if (selectedScope == 0 || selectedScope == 5) matches.folders else emptyList()
    val matchedSongs = if (selectedScope == 0 || selectedScope == 1) results else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        CadenceInlineSearchBar(
            query = query,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            placeholder = "Search songs, artists, albums, folders...",
            onSubmit = { viewModel.commitSearchToHistory(query) },
            modifier = Modifier.testTag("search_input")
        )

        if (query.isNotBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = hPad, vertical = CadenceSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
            ) {
                itemsIndexed(scopeLabels) { idx, label ->
                    FilterChip(
                        selected = selectedScope == idx,
                        onClick = { selectedScope = idx }
                    ) {
                        SingleLineButtonLabel(label, style = MaterialTheme.typography.caption)
                    }
                }
            }
            Divider(color = ext.dividerColor)
        }

        if (query.isBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = hPad, vertical = CadenceSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.xs)
            ) {
                itemsIndexed(scopeLabels) { idx, label ->
                    FilterChip(
                        selected = selectedScope == idx,
                        onClick = { selectedScope = idx }
                    ) {
                        SingleLineButtonLabel(label, style = MaterialTheme.typography.caption)
                    }
                }
            }
            Divider(color = ext.dividerColor)
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = hPad, vertical = CadenceSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = ext.subtleText)
                    Spacer(Modifier.width(6.dp))
                    Text("Recent Searches", style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.clearSearchHistory() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        SingleLineButtonLabel("CLEAR")
                    }
                }
                FlowRow(
                    modifier = Modifier.padding(horizontal = hPad, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    history.forEach { h ->
                        Chip(onClick = { viewModel.updateSearchQuery(h.query) }) {
                            SingleLineButtonLabel(h.query, style = MaterialTheme.typography.caption)
                        }
                    }
                }
            } else {
                EmptyStateView(
                    title = "Instant Local Search",
                    subtitle = "Search titles, artists, albums, playlists, folders, and normalized Persian/Arabic/Latin text.",
                    icon = Icons.Default.Search
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = CadenceSpacing.xl)
            ) {
                searchResultsItems(
                    query = query,
                    selectedScope = selectedScope,
                    matchedArtists = matchedArtists,
                    matchedAlbums = matchedAlbums,
                    matchedPlaylists = matchedPlaylists,
                    matchedFolders = matchedFolders,
                    matchedSongs = matchedSongs,
                    currentTrackId = playback.currentTrack?.id,
                    hPad = hPad,
                    onCommitSearch = { viewModel.commitSearchToHistory(query) },
                    onClearSearch = { viewModel.updateSearchQuery("") },
                    onPlaySongs = { list, idx ->
                        viewModel.container.playerConnection.playTracks(list, idx)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenFolder = onOpenFolder,
                    onTrackMore = onTrackMore,
                    onCollectionMore = onCollectionMore
                )
            }
        }
    }
}

private fun LazyListScope.searchResultsItems(
    query: String,
    selectedScope: Int,
    matchedArtists: List<com.example.data.db.ArtistSummary>,
    matchedAlbums: List<AlbumSummary>,
    matchedPlaylists: List<PlaylistWithCount>,
    matchedFolders: List<FolderSummary>,
    matchedSongs: List<TrackEntity>,
    currentTrackId: Long?,
    hPad: Dp,
    onCommitSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onPlaySongs: (List<TrackEntity>, Int) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (AlbumSummary) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFolder: (FolderSummary) -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onCollectionMore: (CollectionSheetTarget) -> Unit
) {
    if (matchedArtists.isNotEmpty()) {
        item(contentType = "header") {
            SearchSectionLabel("Artists (${matchedArtists.size})", hPad)
        }
        items(matchedArtists, key = { "sa_${it.artist}" }, contentType = { "collection" }) { a ->
            ArtistRowItem(
                artist = a,
                onClick = {
                    onCommitSearch()
                    onOpenArtist(a.artist)
                },
                onLongClick = { onCollectionMore(CollectionSheetTarget.Artist(a, false)) },
                onMoreClick = { onCollectionMore(CollectionSheetTarget.Artist(a, false)) }
            )
        }
    }

    if (matchedAlbums.isNotEmpty()) {
        item(contentType = "header") {
            SearchSectionLabel("Albums (${matchedAlbums.size})", hPad)
        }
        items(matchedAlbums, key = { "sal_${it.album}_${it.albumArtist}" }, contentType = { "collection" }) { al ->
            CollectionListRow(
                title = al.album,
                subtitle = "${al.albumArtist} • ${al.trackCount} tracks",
                artworkUri = al.artworkUri,
                leadingIcon = if (al.artworkUri == null) Icons.Default.Album else null,
                onClick = {
                    onCommitSearch()
                    onOpenAlbum(al)
                },
                onLongClick = { onCollectionMore(CollectionSheetTarget.Album(al)) },
                onMoreClick = { onCollectionMore(CollectionSheetTarget.Album(al)) }
            )
        }
    }

    if (matchedPlaylists.isNotEmpty()) {
        item(contentType = "header") {
            SearchSectionLabel("Playlists (${matchedPlaylists.size})", hPad)
        }
        items(matchedPlaylists, key = { "spl_${it.id}" }, contentType = { "collection" }) { pl ->
            CollectionListRow(
                title = pl.name,
                subtitle = "${pl.trackCount} tracks • ${TextNormalizer.formatDuration(pl.totalDurationMs)}",
                leadingIcon = Icons.Default.PlaylistPlay,
                onClick = {
                    onCommitSearch()
                    onOpenPlaylist(pl.id)
                },
                onLongClick = { onCollectionMore(CollectionSheetTarget.Playlist(pl)) },
                onMoreClick = { onCollectionMore(CollectionSheetTarget.Playlist(pl)) }
            )
        }
    }

    if (matchedFolders.isNotEmpty()) {
        item(contentType = "header") {
            SearchSectionLabel("Folders (${matchedFolders.size})", hPad)
        }
        items(matchedFolders, key = { "sfld_${it.folderPath}" }, contentType = { "collection" }) { f ->
            CollectionListRow(
                title = f.folderName,
                subtitle = "${f.trackCount} tracks • ${f.folderPath}",
                leadingIcon = Icons.Default.Folder,
                onClick = {
                    onCommitSearch()
                    onOpenFolder(f)
                },
                onLongClick = { onCollectionMore(CollectionSheetTarget.Folder(f)) },
                onMoreClick = { onCollectionMore(CollectionSheetTarget.Folder(f)) }
            )
        }
    }

    if (selectedScope == 0 || selectedScope == 1) {
        item(contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = hPad, end = CadenceSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Songs (${matchedSongs.size})",
                    style = MaterialTheme.typography.subtitle2.tabular(),
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.weight(1f).padding(vertical = CadenceSpacing.sm)
                )
                if (matchedSongs.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onCommitSearch()
                            onPlaySongs(matchedSongs, 0)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        SingleLineButtonLabel("PLAY ALL")
                    }
                }
            }
        }
    }

    if (matchedSongs.isEmpty() && matchedArtists.isEmpty() && matchedAlbums.isEmpty() && matchedPlaylists.isEmpty() && matchedFolders.isEmpty()) {
        item(contentType = "empty") {
            EmptyStateView(
                title = "No Matches for \"$query\"",
                subtitle = "Try partial keywords or switch the filter scope.",
                actionLabel = "Clear Query",
                onAction = onClearSearch,
                icon = Icons.Default.Search
            )
        }
    } else {
        itemsIndexed(matchedSongs, key = { _, t -> "sr_${t.id}" }, contentType = { _, _ -> "track" }) { idx, track ->
            TrackListItem(
                track = track,
                isPlayingCurrent = currentTrackId == track.id,
                onClick = {
                    onCommitSearch()
                    onPlaySongs(matchedSongs, idx)
                },
                onLongClick = { onTrackMore(track) },
                onMoreClick = { onTrackMore(track) },
                onToggleFavorite = { onToggleFavorite(track) },
                onArtistClick = onOpenArtist,
                onAlbumClick = { alb, albArt ->
                    onOpenAlbum(AlbumSummary(alb, albArt, track.albumId, track.artworkUri, 0, 0L, track.year))
                }
            )
        }
    }
}

@Composable
private fun SearchSectionLabel(text: String, hPad: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.subtitle2.tabular(),
        color = MaterialTheme.colors.primary,
        modifier = Modifier.padding(horizontal = hPad, vertical = CadenceSpacing.sm)
    )
}
