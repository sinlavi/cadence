package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.AlertDialog
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationDefaults
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.model.LibraryTabKey
import com.example.core.model.NavDestinationKey
import com.example.core.model.NowPlayingStyle
import com.example.core.util.TextNormalizer
import com.example.data.db.PlaylistWithCount
import com.example.data.db.SmartPlaylistEntity
import com.example.data.db.TrackEntity
import com.example.data.preferences.AppSettingsSnapshot
import com.example.data.smart.SmartPlaylistEvaluator
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CadenceBottomTabItem
import com.example.ui.components.CodecInfoDialog
import com.example.ui.components.CollectionActionsSheet
import com.example.ui.components.CollectionSheetTarget
import com.example.ui.components.PersistentMiniPlayer
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.TabBarEditorDialog
import com.example.ui.components.TrackActionsSheet
import com.example.ui.components.UserTagsDialog
import com.example.ui.navigation.CadenceRoute
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartRuleEditorScreen
import com.example.ui.screens.TagEditorScreen
import com.example.ui.screens.ThemeGeneratorScreen
import com.example.ui.screens.TrackCollectionDetailScreen
import com.example.ui.theme.ArtworkPaletteColors
import com.example.ui.theme.CadenceMaterial2Theme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.extractArtworkPalette
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface ActiveSheetPayload {
    data class Track(
        val track: TrackEntity,
        val onPlayNow: (() -> Unit)? = null,
        val onRemoveFromPlaylist: (() -> Unit)? = null
    ) : ActiveSheetPayload

    data class Collection(val target: CollectionSheetTarget) : ActiveSheetPayload
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CadenceApp(
    externalAudioUri: Uri?,
    onExternalAudioHandled: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val libraryStats by viewModel.libraryStats.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val smartPlaylists by viewModel.smartPlaylists.collectAsState()
    val personalizedMixes by viewModel.personalizedMixes.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val playbackBanner by viewModel.lastPlaybackBanner.collectAsState()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasAudioPermission = grants[audioPermission] == true ||
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (hasAudioPermission) {
            viewModel.triggerLibraryScan()
        }
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioPermission = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            val totalTracks = libraryStats?.totalTracks ?: 0
            if (settings.autoScanOnLaunch || totalTracks == 0) {
                viewModel.triggerLibraryScan()
            }
        } else {
            val perms = buildList {
                add(audioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            audioPermissionLauncher.launch(perms)
        }
    }

    var artworkPalette by remember { mutableStateOf<ArtworkPaletteColors?>(null) }
    val needArtworkPalette = settings.themeConfig.useArtworkColors ||
        settings.themeConfig.nowPlayingStyle == NowPlayingStyle.ARTWORK_FOCUSED ||
        settings.themeConfig.nowPlayingStyle == NowPlayingStyle.FULL_SCREEN

    LaunchedEffect(playback.currentTrack?.id, needArtworkPalette) {
        val track = playback.currentTrack
        artworkPalette = if (track != null && needArtworkPalette) {
            withContext(Dispatchers.IO) {
                loadSmallTrackBitmap(context, track.artworkUri, track.contentUri)?.let { bmp ->
                    val pal = extractArtworkPalette(bmp)
                    bmp.recycle()
                    pal
                }
            }
        } else {
            null
        }
    }

    val navController = rememberNavController()

    LaunchedEffect(externalAudioUri) {
        if (externalAudioUri != null) {
            viewModel.container.playerConnection.playExternalUri(externalAudioUri)
            onExternalAudioHandled()
            navController.navigate(CadenceRoute.NowPlaying)
        }
    }

    val scaffoldState = rememberScaffoldState()
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        scaffoldState.snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }
    LaunchedEffect(playbackBanner) {
        val msg = playbackBanner ?: return@LaunchedEffect
        scaffoldState.snackbarHostState.showSnackbar(msg)
        viewModel.container.playbackDiagnostics.dismissBanner()
    }

    var activeSheet by remember { mutableStateOf<ActiveSheetPayload?>(null) }
    var playlistTargetIds by remember { mutableStateOf<List<Long>?>(null) }
    var userTagsTarget by remember { mutableStateOf<Pair<List<Long>, String>?>(null) }
    var codecInfoTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var deleteConfirmTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var renamePlaylistTarget by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deletePlaylistTarget by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var exportPlaylistTarget by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deleteSmartTarget by remember { mutableStateOf<SmartPlaylistEntity?>(null) }
    var editingSmartPlaylist by remember { mutableStateOf<SmartPlaylistEntity?>(null) }
    var showTabBarEditor by remember { mutableStateOf(false) }

    fun moveVisibleNavTab(key: NavDestinationKey, delta: Int) {
        scope.launch {
            viewModel.container.settingsRepository.updateSettings { snap ->
                val visible = snap.navOrder.filter { it != NavDestinationKey.SETTINGS && it !in snap.hiddenNavItems }
                val visIdx = visible.indexOf(key)
                val targetVisIdx = visIdx + delta
                if (visIdx < 0 || targetVisIdx !in visible.indices) return@updateSettings snap
                val neighbor = visible[targetVisIdx]
                val order = snap.navOrder.toMutableList()
                val idxA = order.indexOf(key)
                val idxB = order.indexOf(neighbor)
                if (idxA >= 0 && idxB >= 0) {
                    order[idxA] = neighbor
                    order[idxB] = key
                }
                snap.copy(navOrder = order)
            }
        }
    }

    fun moveNavOrderItem(key: NavDestinationKey, delta: Int) {
        scope.launch {
            viewModel.container.settingsRepository.updateSettings { snap ->
                val configurable = snap.navOrder.filter { it != NavDestinationKey.SETTINGS }.toMutableList()
                val idx = configurable.indexOf(key)
                val target = idx + delta
                if (idx < 0 || target !in configurable.indices) return@updateSettings snap
                val moved = configurable.removeAt(idx)
                configurable.add(target, moved)
                snap.copy(navOrder = configurable + NavDestinationKey.SETTINGS)
            }
        }
    }

    fun toggleNavTabVisibility(key: NavDestinationKey, show: Boolean) {
        scope.launch {
            viewModel.container.settingsRepository.updateSettings { snap ->
                val visibleCount = snap.navOrder.count { it != NavDestinationKey.SETTINGS && it !in snap.hiddenNavItems }
                if (!show && visibleCount <= 2) return@updateSettings snap
                val nextHidden = if (show) snap.hiddenNavItems - key else snap.hiddenNavItems + key
                snap.copy(hiddenNavItems = nextHidden + NavDestinationKey.SETTINGS)
            }
        }
    }

    fun applyNavPreset(order: List<NavDestinationKey>, hidden: Set<NavDestinationKey>) {
        scope.launch {
            viewModel.container.settingsRepository.updateSettings { snap ->
                snap.copy(
                    navOrder = order,
                    hiddenNavItems = hidden + NavDestinationKey.SETTINGS
                )
            }
        }
    }

    fun resetDefaultNavTabs() {
        applyNavPreset(
            AppSettingsSnapshot.defaultNavOrder(),
            AppSettingsSnapshot.defaultHiddenNavItems()
        )
        viewModel.showMessage("Navigation tabs restored to default.")
    }

    val m3uExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val pl = exportPlaylistTarget
        exportPlaylistTarget = null
        if (uri != null && pl != null) {
            scope.launch {
                val tracks = withContext(Dispatchers.IO) {
                    viewModel.container.database.playlistDao().getPlaylistTracksOnce(pl.id)
                }
                viewModel.container.backupManager.exportM3u8Playlist(pl.name, tracks, uri)
                    .onSuccess { viewModel.showMessage("Exported \"${pl.name}.m3u8\" (${tracks.size} tracks).") }
                    .onFailure { viewModel.showMessage("Export failed: ${it.localizedMessage}") }
            }
        }
    }

    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    BackHandler(enabled = modalSheetState.isVisible) {
        scope.launch { modalSheetState.hide() }
    }

    fun openTrackSheet(
        track: TrackEntity,
        onPlayNow: (() -> Unit)? = null,
        onRemoveFromPlaylist: (() -> Unit)? = null
    ) {
        activeSheet = ActiveSheetPayload.Track(track, onPlayNow, onRemoveFromPlaylist)
        scope.launch { modalSheetState.show() }
    }

    fun openCollectionSheet(target: CollectionSheetTarget) {
        activeSheet = ActiveSheetPayload.Collection(target)
        scope.launch { modalSheetState.show() }
    }

    suspend fun resolveCollectionTracks(target: CollectionSheetTarget): List<TrackEntity> {
        return when (target) {
            is CollectionSheetTarget.Album -> viewModel.getTracksForAlbum(target.album.album, target.album.albumArtist)
            is CollectionSheetTarget.Artist -> viewModel.getTracksForArtist(target.artist.artist, target.isAlbumArtist)
            is CollectionSheetTarget.Genre -> viewModel.getTracksForGenre(target.genre.genre)
            is CollectionSheetTarget.Year -> viewModel.getTracksForYear(target.year.year)
            is CollectionSheetTarget.Folder -> viewModel.getTracksForFolder(target.folder.folderPath)
            is CollectionSheetTarget.Playlist -> withContext(Dispatchers.IO) {
                viewModel.container.database.playlistDao().getPlaylistTracksOnce(target.playlist.id)
            }
            is CollectionSheetTarget.SmartPlaylist -> withContext(Dispatchers.IO) {
                val def = SmartPlaylistEvaluator.parseDefinition(target.smartPlaylist.definitionJson)
                viewModel.container.database.trackDao().queryTracksRaw(SmartPlaylistEvaluator.buildSmartPlaylistQuery(def))
            }
        }
    }

    fun openCollectionDestination(target: CollectionSheetTarget) {
        when (target) {
            is CollectionSheetTarget.Album -> navController.navigate(CadenceRoute.AlbumDetail(target.album.album, target.album.albumArtist))
            is CollectionSheetTarget.Artist -> navController.navigate(CadenceRoute.ArtistDetail(target.artist.artist, target.isAlbumArtist))
            is CollectionSheetTarget.Genre -> navController.navigate(CadenceRoute.GenreDetail(target.genre.genre))
            is CollectionSheetTarget.Year -> navController.navigate(CadenceRoute.YearDetail(target.year.year))
            is CollectionSheetTarget.Folder -> navController.navigate(CadenceRoute.FolderDetail(target.folder.folderPath, target.folder.folderName))
            is CollectionSheetTarget.Playlist -> navController.navigate(CadenceRoute.PlaylistDetail(target.playlist.id))
            is CollectionSheetTarget.SmartPlaylist -> navController.navigate(CadenceRoute.SmartPlaylistDetail(target.smartPlaylist.id))
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val isPrimaryTopLevel = currentDest?.let { dest ->
        dest.hasRoute<CadenceRoute.Home>() ||
            dest.hasRoute<CadenceRoute.Artists>() ||
            dest.hasRoute<CadenceRoute.Albums>() ||
            dest.hasRoute<CadenceRoute.Files>() ||
            dest.hasRoute<CadenceRoute.Songs>() ||
            dest.hasRoute<CadenceRoute.Genres>() ||
            dest.hasRoute<CadenceRoute.Years>() ||
            dest.hasRoute<CadenceRoute.Composers>() ||
            dest.hasRoute<CadenceRoute.SmartViews>() ||
            dest.hasRoute<CadenceRoute.Library>() ||
            dest.hasRoute<CadenceRoute.Playlists>() ||
            dest.hasRoute<CadenceRoute.Search>()
    } ?: true

    val hideMiniPlayer = currentDest?.let { dest ->
        dest.hasRoute<CadenceRoute.NowPlaying>() ||
            dest.hasRoute<CadenceRoute.Lyrics>()
    } ?: false

    val phoneNavItems = remember(settings.navOrder, settings.hiddenNavItems) {
        val filtered = settings.navOrder.filter { it != NavDestinationKey.SETTINGS && it !in settings.hiddenNavItems }
        filtered.ifEmpty {
            listOf(
                NavDestinationKey.HOME,
                NavDestinationKey.ARTISTS,
                NavDestinationKey.ALBUMS,
                NavDestinationKey.FILES,
                NavDestinationKey.PLAYLISTS
            )
        }
    }

    CadenceMaterial2Theme(
        themeConfig = settings.themeConfig,
        artworkPalette = artworkPalette,
        ratingSystemMode = settings.ratingSystemMode
    ) {
        if (showTabBarEditor) {
            TabBarEditorDialog(
                navOrder = settings.navOrder,
                hiddenNavItems = settings.hiddenNavItems,
                iconForKey = ::navIconFor,
                onMoveUp = { key -> moveNavOrderItem(key, -1) },
                onMoveDown = { key -> moveNavOrderItem(key, 1) },
                onToggleVisibility = { key, show -> toggleNavTabVisibility(key, show) },
                onApplyPreset = { order, hidden -> applyNavPreset(order, hidden) },
                onResetDefaults = ::resetDefaultNavTabs,
                onDismiss = { showTabBarEditor = false }
            )
        }

        if (editingSmartPlaylist != null) {
            SmartRuleEditorScreen(
                initial = editingSmartPlaylist,
                viewModel = viewModel,
                onSave = { entity ->
                    viewModel.saveSmartPlaylist(entity)
                    editingSmartPlaylist = null
                },
                onDismiss = { editingSmartPlaylist = null }
            )
        }

        playlistTargetIds?.let { ids ->
            AddToPlaylistDialog(
                playlists = playlists,
                onSelectPlaylist = { plId -> viewModel.addTracksToPlaylist(plId, ids) },
                onCreateNew = { newName -> viewModel.createPlaylist(newName, initialTrackIds = ids) },
                onDismiss = { playlistTargetIds = null }
            )
        }

        userTagsTarget?.let { (ids, initialCsv) ->
            UserTagsDialog(
                initialTagsCsv = initialCsv,
                trackCount = ids.size,
                onSave = { csv -> viewModel.updateTrackCustomTags(ids, csv) },
                onDismiss = { userTagsTarget = null }
            )
        }

        codecInfoTrack?.let { tr ->
            CodecInfoDialog(track = tr, onDismiss = { codecInfoTrack = null })
        }

        deleteConfirmTrack?.let { tr ->
            AlertDialog(
                onDismissRequest = { deleteConfirmTrack = null },
                title = { Text("Delete Track File?") },
                text = { Text("Permanently delete \"${tr.title}\" from device storage?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteTrack(tr)
                        deleteConfirmTrack = null
                    }) { SingleLineButtonLabel("DELETE") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmTrack = null }) { SingleLineButtonLabel("CANCEL") }
                }
            )
        }

        renamePlaylistTarget?.let { target ->
            var name by remember(target.id) { mutableStateOf(target.name) }
            AlertDialog(
                onDismissRequest = { renamePlaylistTarget = null },
                title = { Text("Rename Playlist") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.renamePlaylist(target.id, name)
                            renamePlaylistTarget = null
                        },
                        enabled = name.isNotBlank()
                    ) { SingleLineButtonLabel("SAVE") }
                },
                dismissButton = {
                    TextButton(onClick = { renamePlaylistTarget = null }) { SingleLineButtonLabel("CANCEL") }
                }
            )
        }

        deletePlaylistTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deletePlaylistTarget = null },
                title = { Text("Delete Playlist?") },
                text = { Text("Remove \"${target.name}\"? Audio files on your device will not be deleted.") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deletePlaylist(target.id)
                        deletePlaylistTarget = null
                    }) { SingleLineButtonLabel("DELETE") }
                },
                dismissButton = {
                    TextButton(onClick = { deletePlaylistTarget = null }) { SingleLineButtonLabel("CANCEL") }
                }
            )
        }

        deleteSmartTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteSmartTarget = null },
                title = { Text("Delete Smart Playlist?") },
                text = { Text("Remove \"${target.name}\" smart rules?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteSmartPlaylist(target.id)
                        deleteSmartTarget = null
                    }) { SingleLineButtonLabel("DELETE") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteSmartTarget = null }) { SingleLineButtonLabel("CANCEL") }
                }
            )
        }

        ModalBottomSheetLayout(
            sheetState = modalSheetState,
            sheetBackgroundColor = MaterialTheme.colors.surface,
            sheetContent = {
                when (val payload = activeSheet) {
                    is ActiveSheetPayload.Track -> {
                        val track = payload.track
                        TrackActionsSheet(
                            track = track,
                            onDismiss = { scope.launch { modalSheetState.hide() } },
                            onPlayNow = payload.onPlayNow,
                            onPlayNext = {
                                viewModel.container.playerConnection.addPlayNext(listOf(track))
                                viewModel.showMessage("Queued \"${track.title}\" next.")
                            },
                            onAddToEnd = {
                                viewModel.container.playerConnection.addToEnd(listOf(track))
                                viewModel.showMessage("Added \"${track.title}\" to queue.")
                            },
                            onAddToPlaylist = { playlistTargetIds = listOf(track.id) },
                            onRemoveFromPlaylist = payload.onRemoveFromPlaylist,
                            onToggleFavorite = { viewModel.toggleFavorite(track) },
                            onSetRating = { r -> viewModel.setTrackRating(track.id, r) },
                            onGoToAlbum = {
                                navController.navigate(
                                    CadenceRoute.AlbumDetail(
                                        albumName = track.album,
                                        albumArtist = track.albumArtist.ifBlank { track.artist }
                                    )
                                )
                            },
                            onGoToArtist = {
                                navController.navigate(CadenceRoute.ArtistDetail(artistName = track.artist, isAlbumArtist = false))
                            },
                            onOpenTagEditor = {
                                navController.navigate(CadenceRoute.TagEditor(track.id.toString()))
                            },
                            onManageUserTags = {
                                userTagsTarget = listOf(track.id) to track.customTags
                            },
                            onOpenLyrics = {
                                navController.navigate(CadenceRoute.Lyrics(trackId = track.id, startInEditor = false))
                            },
                            onShowFileInfo = { codecInfoTrack = track },
                            onShare = { viewModel.shareTracks(listOf(track)) },
                            onExcludeFolder = {
                                viewModel.excludeFolder(track.folderPath, track.folderName)
                            },
                            onDelete = { deleteConfirmTrack = track }
                        )
                    }
                    is ActiveSheetPayload.Collection -> {
                        CollectionActionsSheet(
                            target = payload.target,
                            onDismiss = { scope.launch { modalSheetState.hide() } },
                            onPlayTarget = { target, shuffle ->
                                scope.launch {
                                    val tracks = resolveCollectionTracks(target)
                                    if (tracks.isNotEmpty()) {
                                        viewModel.container.playerConnection.playTracks(
                                            if (shuffle) tracks.shuffled() else tracks,
                                            0,
                                            shuffle = shuffle
                                        )
                                    } else {
                                        viewModel.showMessage("No playable tracks found.")
                                    }
                                }
                            },
                            onPlayNextTarget = { target ->
                                scope.launch {
                                    val tracks = resolveCollectionTracks(target)
                                    if (tracks.isNotEmpty()) {
                                        viewModel.container.playerConnection.addPlayNext(tracks)
                                        viewModel.showMessage("Queued ${tracks.size} track(s) next.")
                                    }
                                }
                            },
                            onAddToQueueTarget = { target ->
                                scope.launch {
                                    val tracks = resolveCollectionTracks(target)
                                    if (tracks.isNotEmpty()) {
                                        viewModel.container.playerConnection.addToEnd(tracks)
                                        viewModel.showMessage("Added ${tracks.size} track(s) to queue.")
                                    }
                                }
                            },
                            onAddToPlaylistTarget = { target ->
                                scope.launch {
                                    val tracks = resolveCollectionTracks(target)
                                    if (tracks.isNotEmpty()) {
                                        playlistTargetIds = tracks.map { it.id }
                                    }
                                }
                            },
                            onBatchEditTagsTarget = { target ->
                                scope.launch {
                                    val tracks = resolveCollectionTracks(target)
                                    if (tracks.isNotEmpty()) {
                                        navController.navigate(CadenceRoute.TagEditor(tracks.joinToString(",") { it.id.toString() }))
                                    }
                                }
                            },
                            onOpenTarget = ::openCollectionDestination,
                            onRenamePlaylist = { renamePlaylistTarget = it },
                            onDuplicatePlaylist = { viewModel.duplicatePlaylist(it.id) },
                            onExportPlaylist = {
                                exportPlaylistTarget = it
                                m3uExportLauncher.launch("${TextNormalizer.sanitizeFilename(it.name)}.m3u8")
                            },
                            onDeletePlaylist = { deletePlaylistTarget = it },
                            onEditSmartPlaylist = { editingSmartPlaylist = it },
                            onDuplicateSmartPlaylist = { sp ->
                                val copy = sp.copy(
                                    id = 0L,
                                    name = "${sp.name} (Copy)",
                                    isBuiltIn = false,
                                    createdAt = System.currentTimeMillis()
                                )
                                viewModel.saveSmartPlaylist(copy)
                            },
                            onDeleteSmartPlaylist = { deleteSmartTarget = it },
                            onRescanLibrary = { viewModel.triggerLibraryScan() },
                            onExcludeFolder = { f -> viewModel.excludeFolder(f.folderPath, f.folderName) }
                        )
                    }
                    null -> Spacer(Modifier.navigationBarsPadding())
                }
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 700.dp
                val isCompactWidth = maxWidth < 360.dp

                CompositionLocalProvider(LocalIsCompactWidth provides isCompactWidth) {

                    fun navigateToTopLevel(key: NavDestinationKey) {
                        val route: CadenceRoute = when (key) {
                            NavDestinationKey.HOME -> CadenceRoute.Home
                            NavDestinationKey.ARTISTS -> CadenceRoute.Artists
                            NavDestinationKey.ALBUMS -> CadenceRoute.Albums
                            NavDestinationKey.FILES -> CadenceRoute.Files
                            NavDestinationKey.PLAYLISTS -> CadenceRoute.Playlists
                            NavDestinationKey.SONGS -> CadenceRoute.Songs
                            NavDestinationKey.GENRES -> CadenceRoute.Genres
                            NavDestinationKey.YEARS -> CadenceRoute.Years
                            NavDestinationKey.COMPOSERS -> CadenceRoute.Composers
                            NavDestinationKey.SMART_VIEWS -> CadenceRoute.SmartViews
                            NavDestinationKey.LIBRARY -> CadenceRoute.Library()
                            NavDestinationKey.SEARCH -> CadenceRoute.Search
                            NavDestinationKey.SETTINGS -> CadenceRoute.Settings
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    fun isNavSelected(key: NavDestinationKey): Boolean {
                        val dest = currentDest ?: return key == NavDestinationKey.HOME
                        return when (key) {
                            NavDestinationKey.HOME -> dest.hasRoute<CadenceRoute.Home>()
                            NavDestinationKey.ARTISTS -> dest.hasRoute<CadenceRoute.Artists>()
                            NavDestinationKey.ALBUMS -> dest.hasRoute<CadenceRoute.Albums>()
                            NavDestinationKey.FILES -> dest.hasRoute<CadenceRoute.Files>()
                            NavDestinationKey.PLAYLISTS -> dest.hasRoute<CadenceRoute.Playlists>()
                            NavDestinationKey.SONGS -> dest.hasRoute<CadenceRoute.Songs>()
                            NavDestinationKey.GENRES -> dest.hasRoute<CadenceRoute.Genres>()
                            NavDestinationKey.YEARS -> dest.hasRoute<CadenceRoute.Years>()
                            NavDestinationKey.COMPOSERS -> dest.hasRoute<CadenceRoute.Composers>()
                            NavDestinationKey.SMART_VIEWS -> dest.hasRoute<CadenceRoute.SmartViews>()
                            NavDestinationKey.LIBRARY -> dest.hasRoute<CadenceRoute.Library>()
                            NavDestinationKey.SEARCH -> dest.hasRoute<CadenceRoute.Search>()
                            NavDestinationKey.SETTINGS -> dest.hasRoute<CadenceRoute.Settings>()
                        }
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (isWideScreen && (isPrimaryTopLevel || isNavSelected(NavDestinationKey.SETTINGS))) {
                            NavigationRail(
                                backgroundColor = MaterialTheme.colors.surface,
                                elevation = 2.dp,
                                modifier = Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding()
                            ) {
                                phoneNavItems.forEach { key ->
                                    NavigationRailItem(
                                        selected = isNavSelected(key),
                                        onClick = { navigateToTopLevel(key) },
                                        icon = { Icon(navIconFor(key), contentDescription = key.label) },
                                        label = { SingleLineButtonLabel(key.label, style = MaterialTheme.typography.caption) },
                                        modifier = Modifier.testTag("nav_rail_${key.name.lowercase()}")
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                NavigationRailItem(
                                    selected = false,
                                    onClick = { showTabBarEditor = true },
                                    icon = { Icon(Icons.Default.Tune, contentDescription = "Customize Tabs") },
                                    label = { SingleLineButtonLabel("Tabs", style = MaterialTheme.typography.caption) },
                                    modifier = Modifier.testTag("nav_rail_edit_tabs")
                                )
                                NavigationRailItem(
                                    selected = isNavSelected(NavDestinationKey.SETTINGS),
                                    onClick = { navController.navigate(CadenceRoute.Settings) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { SingleLineButtonLabel("Settings", style = MaterialTheme.typography.caption) },
                                    modifier = Modifier.testTag("nav_rail_settings")
                                )
                            }
                        }

                        Scaffold(
                            scaffoldState = scaffoldState,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            snackbarHost = { SnackbarHost(it) },
                            topBar = {
                                if (isPrimaryTopLevel) {
                                    val titleText = when {
                                        isNavSelected(NavDestinationKey.HOME) -> "Library"
                                        isNavSelected(NavDestinationKey.ARTISTS) -> "Artists"
                                        isNavSelected(NavDestinationKey.ALBUMS) -> "Albums"
                                        isNavSelected(NavDestinationKey.FILES) -> "Files"
                                        isNavSelected(NavDestinationKey.PLAYLISTS) -> "Playlists"
                                        isNavSelected(NavDestinationKey.SONGS) -> "Songs"
                                        isNavSelected(NavDestinationKey.GENRES) -> "Genres"
                                        isNavSelected(NavDestinationKey.YEARS) -> "Years"
                                        isNavSelected(NavDestinationKey.COMPOSERS) -> "Composers"
                                        isNavSelected(NavDestinationKey.SMART_VIEWS) -> "Smart Views"
                                        isNavSelected(NavDestinationKey.LIBRARY) -> "Browse"
                                        isNavSelected(NavDestinationKey.SEARCH) -> "Search"
                                        else -> "Cadence"
                                    }
                                    var appBarOverflow by remember { mutableStateOf(false) }
                                    val hasTabBarBelow = isNavSelected(NavDestinationKey.LIBRARY) || isNavSelected(NavDestinationKey.PLAYLISTS)
                                    TopAppBar(
                                        modifier = Modifier.statusBarsPadding(),
                                        title = { Text(titleText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        elevation = if (hasTabBarBelow) 0.dp else AppBarDefaults.TopAppBarElevation,
                                        actions = {
                                            IconButton(
                                                onClick = { navController.navigate(CadenceRoute.Queue) },
                                                modifier = Modifier.testTag("topbar_queue_button")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Active Queue")
                                            }
                                            IconButton(
                                                onClick = { navController.navigate(CadenceRoute.Settings) },
                                                modifier = Modifier.testTag("topbar_settings_button")
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                                            }
                                            Box {
                                                IconButton(
                                                    onClick = { appBarOverflow = true },
                                                    modifier = Modifier.testTag("topbar_overflow_button")
                                                ) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                                }
                                                DropdownMenu(
                                                    expanded = appBarOverflow,
                                                    onDismissRequest = { appBarOverflow = false }
                                                ) {
                                                    DropdownMenuItem(onClick = {
                                                        appBarOverflow = false
                                                        showTabBarEditor = true
                                                    }) {
                                                        Text("Customize Bottom Tabs")
                                                    }
                                                    DropdownMenuItem(onClick = {
                                                        appBarOverflow = false
                                                        navController.navigate(CadenceRoute.Equalizer)
                                                    }) {
                                                        Text("Equalizer & Audio DSP")
                                                    }
                                                    DropdownMenuItem(onClick = {
                                                        appBarOverflow = false
                                                        viewModel.triggerLibraryScan()
                                                    }) {
                                                        Text("Rescan Audio Library")
                                                    }
                                                    DropdownMenuItem(onClick = {
                                                        appBarOverflow = false
                                                        safFolderLauncher.launch(null)
                                                    }) {
                                                        Text("Add Music Folder (SAF)")
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            bottomBar = {
                                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                                    val activeTrack = playback.currentTrack
                                    if (activeTrack != null && !hideMiniPlayer) {
                                        PersistentMiniPlayer(
                                            track = activeTrack,
                                            isPlaying = playback.isPlaying,
                                            progressFlow = viewModel.playbackProgress,
                                            onOpenNowPlaying = { navController.navigate(CadenceRoute.NowPlaying) },
                                            onTogglePlayPause = { viewModel.container.playerConnection.togglePlayPause() },
                                            onNext = { viewModel.container.playerConnection.next() },
                                            onPrevious = { viewModel.container.playerConnection.previous() },
                                            onToggleFavorite = { viewModel.toggleFavorite(activeTrack) },
                                            onSetRating = { r -> viewModel.setTrackRating(activeTrack.id, r) },
                                            onOpenQueue = { navController.navigate(CadenceRoute.Queue) },
                                            showArtwork = settings.miniPlayerShowArtwork,
                                            showFavorite = settings.miniPlayerShowFavorite,
                                            showPrevious = settings.miniPlayerShowPrevious,
                                            showPlayPause = settings.miniPlayerShowPlayPause,
                                            showNext = settings.miniPlayerShowNext,
                                            showQueue = settings.miniPlayerShowQueue
                                        )
                                    }
                                    if (isPrimaryTopLevel && !isWideScreen) {
                                        val visibleBottomItems = phoneNavItems.take(5)
                                        val canHideAnyTab = phoneNavItems.size > 2
                                        BottomNavigation(
                                            backgroundColor = MaterialTheme.colors.surface,
                                            elevation = BottomNavigationDefaults.Elevation
                                        ) {
                                            visibleBottomItems.forEachIndexed { idx, key ->
                                                CadenceBottomTabItem(
                                                    selected = isNavSelected(key),
                                                    icon = navIconFor(key),
                                                    label = key.label,
                                                    canMoveLeft = idx > 0,
                                                    canMoveRight = idx < visibleBottomItems.lastIndex,
                                                    canHide = canHideAnyTab,
                                                    onClick = { navigateToTopLevel(key) },
                                                    onMoveLeft = { moveVisibleNavTab(key, -1) },
                                                    onMoveRight = { moveVisibleNavTab(key, 1) },
                                                    onHideTab = {
                                                        val wasSelected = isNavSelected(key)
                                                        toggleNavTabVisibility(key, false)
                                                        if (wasSelected) {
                                                            val fallback = visibleBottomItems.firstOrNull { it != key }
                                                            if (fallback != null) navigateToTopLevel(fallback)
                                                        }
                                                    },
                                                    onOpenTabEditor = { showTabBarEditor = true },
                                                    onResetTabs = ::resetDefaultNavTabs,
                                                    modifier = Modifier.testTag("bottom_nav_${key.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                @Composable
                                fun RenderLibraryPane(tab: LibraryTabKey, singleTab: Boolean) {
                                    LibraryScreen(
                                        viewModel = viewModel,
                                        initialTab = tab,
                                        forcedSingleTab = singleTab,
                                        onOpenAlbum = { alb ->
                                            navController.navigate(CadenceRoute.AlbumDetail(alb.album, alb.albumArtist))
                                        },
                                        onOpenArtist = { name, isAlbumArtist ->
                                            navController.navigate(CadenceRoute.ArtistDetail(name, isAlbumArtist))
                                        },
                                        onOpenGenre = { genre ->
                                            navController.navigate(CadenceRoute.GenreDetail(genre))
                                        },
                                        onOpenFolder = { folder ->
                                            navController.navigate(CadenceRoute.FolderDetail(folder.folderPath, folder.folderName))
                                        },
                                        onOpenSmartView = {},
                                        onOpenYear = { year ->
                                            navController.navigate(CadenceRoute.YearDetail(year))
                                        },
                                        onOpenBatchTagEditor = { ids ->
                                            navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                        },
                                        onManageBatchUserTags = { ids ->
                                            scope.launch {
                                                val first = if (ids.size == 1) viewModel.getTrackById(ids.first()) else null
                                                userTagsTarget = ids to (first?.customTags.orEmpty())
                                            }
                                        },
                                        onAddTracksToPlaylist = { ids ->
                                            playlistTargetIds = ids
                                        },
                                        onPickSafFolder = { safFolderLauncher.launch(null) },
                                        onTrackMore = { tr -> openTrackSheet(tr) },
                                        onCollectionMore = ::openCollectionSheet
                                    )
                                }

                                NavHost(
                                    navController = navController,
                                    startDestination = CadenceRoute.Home
                                ) {
                                    composable<CadenceRoute.Home> {
                                        HomeScreen(
                                            viewModel = viewModel,
                                            hasAudioPermission = hasAudioPermission,
                                            onRequestAudioPermission = {
                                                val perms = buildList {
                                                    add(audioPermission)
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                }.toTypedArray()
                                                audioPermissionLauncher.launch(perms)
                                            },
                                            onPickSafFolder = { safFolderLauncher.launch(null) },
                                            onOpenAlbum = { alb ->
                                                navController.navigate(CadenceRoute.AlbumDetail(alb.album, alb.albumArtist))
                                            },
                                            onOpenArtist = { artist ->
                                                navController.navigate(CadenceRoute.ArtistDetail(artist, false))
                                            },
                                            onOpenPlaylist = { plId ->
                                                navController.navigate(CadenceRoute.PlaylistDetail(plId))
                                            },
                                            onOpenFolder = { folder ->
                                                navController.navigate(CadenceRoute.FolderDetail(folder.folderPath, folder.folderName))
                                            },
                                            onOpenLibraryTab = { tab ->
                                                navController.navigate(CadenceRoute.Library(tab.name))
                                            },
                                            onOpenGeneratedMix = { mix ->
                                                navController.navigate(CadenceRoute.GeneratedMixDetail(mix.id))
                                            },
                                            onOpenEqualizer = { navController.navigate(CadenceRoute.Equalizer) },
                                            onOpenNowPlaying = { navController.navigate(CadenceRoute.NowPlaying) },
                                            onOpenQueue = { navController.navigate(CadenceRoute.Queue) },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onAlbumMore = { alb -> openCollectionSheet(CollectionSheetTarget.Album(alb)) },
                                            onPlaylistMore = { pl -> openCollectionSheet(CollectionSheetTarget.Playlist(pl)) },
                                            onCollectionMore = ::openCollectionSheet
                                        )
                                    }

                                    composable<CadenceRoute.Artists> { RenderLibraryPane(LibraryTabKey.ARTISTS, singleTab = true) }
                                    composable<CadenceRoute.Albums> { RenderLibraryPane(LibraryTabKey.ALBUMS, singleTab = true) }
                                    composable<CadenceRoute.Files> { RenderLibraryPane(LibraryTabKey.FOLDERS, singleTab = true) }
                                    composable<CadenceRoute.Songs> { RenderLibraryPane(LibraryTabKey.SONGS, singleTab = true) }
                                    composable<CadenceRoute.Genres> { RenderLibraryPane(LibraryTabKey.GENRES, singleTab = true) }
                                    composable<CadenceRoute.Years> { RenderLibraryPane(LibraryTabKey.YEARS, singleTab = true) }
                                    composable<CadenceRoute.Composers> { RenderLibraryPane(LibraryTabKey.COMPOSERS, singleTab = true) }
                                    composable<CadenceRoute.SmartViews> { RenderLibraryPane(LibraryTabKey.COLLECTIONS, singleTab = true) }

                                    composable<CadenceRoute.Library> { entry ->
                                        val route = entry.toRoute<CadenceRoute.Library>()
                                        val initialTab = remember(route.initialTab) {
                                            runCatching { LibraryTabKey.valueOf(route.initialTab) }.getOrDefault(LibraryTabKey.SONGS)
                                        }
                                        RenderLibraryPane(initialTab, singleTab = false)
                                    }

                                    composable<CadenceRoute.Search> {
                                        SearchScreen(
                                            viewModel = viewModel,
                                            onOpenAlbum = { alb ->
                                                navController.navigate(CadenceRoute.AlbumDetail(alb.album, alb.albumArtist))
                                            },
                                            onOpenArtist = { artist ->
                                                navController.navigate(CadenceRoute.ArtistDetail(artist, false))
                                            },
                                            onOpenPlaylist = { plId ->
                                                navController.navigate(CadenceRoute.PlaylistDetail(plId))
                                            },
                                            onOpenFolder = { f ->
                                                navController.navigate(CadenceRoute.FolderDetail(f.folderPath, f.folderName))
                                            },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onCollectionMore = ::openCollectionSheet
                                        )
                                    }

                                    composable<CadenceRoute.Playlists> {
                                        PlaylistsScreen(
                                            viewModel = viewModel,
                                            onOpenPlaylist = { plId ->
                                                navController.navigate(CadenceRoute.PlaylistDetail(plId))
                                            },
                                            onOpenSmartPlaylist = { spId ->
                                                navController.navigate(CadenceRoute.SmartPlaylistDetail(spId))
                                            },
                                            onPlaylistMore = { pl -> openCollectionSheet(CollectionSheetTarget.Playlist(pl)) },
                                            onSmartPlaylistMore = { sp -> openCollectionSheet(CollectionSheetTarget.SmartPlaylist(sp)) }
                                        )
                                    }

                                    composable<CadenceRoute.Settings> {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() },
                                            onOpenThemeGenerator = { navController.navigate(CadenceRoute.ThemeGenerator) },
                                            onOpenEqualizer = { navController.navigate(CadenceRoute.Equalizer) }
                                        )
                                    }

                                    composable<CadenceRoute.NowPlaying> {
                                        NowPlayingScreen(
                                            viewModel = viewModel,
                                            artworkPalette = artworkPalette,
                                            onBack = { navController.popBackStack() },
                                            onOpenQueue = { navController.navigate(CadenceRoute.Queue) },
                                            onOpenLyrics = { trId -> navController.navigate(CadenceRoute.Lyrics(trId, false)) },
                                            onOpenEqualizer = { navController.navigate(CadenceRoute.Equalizer) },
                                            onOpenTagEditor = { trId -> navController.navigate(CadenceRoute.TagEditor(trId.toString())) },
                                            onOpenAlbum = { albName, albArtist ->
                                                navController.navigate(CadenceRoute.AlbumDetail(albName, albArtist))
                                            },
                                            onOpenArtist = { artist ->
                                                navController.navigate(CadenceRoute.ArtistDetail(artist, false))
                                            },
                                            onAddToPlaylist = { trId -> playlistTargetIds = listOf(trId) },
                                            onManageUserTags = { tr -> userTagsTarget = listOf(tr.id) to tr.customTags }
                                        )
                                    }

                                    composable<CadenceRoute.Queue> {
                                        QueueScreen(
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() },
                                            onAddTracksToPlaylist = { ids -> playlistTargetIds = ids },
                                            onOpenTagEditor = { ids -> navController.navigate(CadenceRoute.TagEditor(ids.joinToString(","))) },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onOpenArtist = { artist ->
                                                navController.navigate(CadenceRoute.ArtistDetail(artist, false))
                                            },
                                            onOpenAlbum = { albName, albArtist ->
                                                navController.navigate(CadenceRoute.AlbumDetail(albName, albArtist))
                                            }
                                        )
                                    }

                                    composable<CadenceRoute.Lyrics> { entry ->
                                        val route = entry.toRoute<CadenceRoute.Lyrics>()
                                        LyricsScreen(
                                            trackId = route.trackId,
                                            startInEditor = route.startInEditor,
                                            viewModel = viewModel,
                                            artworkPalette = artworkPalette,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable<CadenceRoute.TagEditor> { entry ->
                                        val route = entry.toRoute<CadenceRoute.TagEditor>()
                                        val ids = remember(route.trackIdsCsv) {
                                            route.trackIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
                                        }
                                        TagEditorScreen(
                                            trackIds = ids,
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable<CadenceRoute.Equalizer> {
                                        EqualizerScreen(
                                            viewModel = viewModel,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable<CadenceRoute.ThemeGenerator> {
                                        ThemeGeneratorScreen(
                                            viewModel = viewModel,
                                            artworkPalette = artworkPalette,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable<CadenceRoute.AlbumDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.AlbumDetail>()
                                        AlbumDetailScreen(
                                            albumName = route.albumName,
                                            albumArtist = route.albumArtist,
                                            viewModel = viewModel,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onOpenAlbum = { albName, albArt ->
                                                navController.navigate(CadenceRoute.AlbumDetail(albName, albArt))
                                            },
                                            onOpenArtist = { artistName, isAlbArt ->
                                                navController.navigate(CadenceRoute.ArtistDetail(artistName, isAlbArt))
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                scope.launch {
                                                    val first = if (ids.size == 1) viewModel.getTrackById(ids.first()) else null
                                                    userTagsTarget = ids to (first?.customTags.orEmpty())
                                                }
                                            },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onAlbumMore = { target -> openCollectionSheet(target) }
                                        )
                                    }

                                    composable<CadenceRoute.ArtistDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.ArtistDetail>()
                                        ArtistDetailScreen(
                                            artistName = route.artistName,
                                            isAlbumArtist = route.isAlbumArtist,
                                            viewModel = viewModel,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onOpenAlbum = { albName, albArt ->
                                                navController.navigate(CadenceRoute.AlbumDetail(albName, albArt))
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                scope.launch {
                                                    val first = if (ids.size == 1) viewModel.getTrackById(ids.first()) else null
                                                    userTagsTarget = ids to (first?.customTags.orEmpty())
                                                }
                                            },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onAlbumMore = { target -> openCollectionSheet(target) }
                                        )
                                    }

                                    composable<CadenceRoute.GeneratedMixDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.GeneratedMixDetail>()
                                        val mix = personalizedMixes.firstOrNull { it.id == route.mixId }
                                        val tracks by remember(route.mixId) {
                                            viewModel.observeMixTracks(route.mixId)
                                        }.collectAsState(initial = emptyList())
                                        TrackCollectionDetailScreen(
                                            title = mix?.title ?: "Generated Mix",
                                            subtitle = mix?.subtitle ?: "${tracks.size} tracks",
                                            tracks = tracks,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onPlayAll = { idx, shuf -> viewModel.container.playerConnection.playTracks(tracks, idx, shuf) },
                                            onPlayNext = { chosen ->
                                                viewModel.container.playerConnection.addPlayNext(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                                            },
                                            onQueueTracks = { chosen ->
                                                viewModel.container.playerConnection.addToEnd(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s).")
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                val first = tracks.firstOrNull { it.id in ids }
                                                userTagsTarget = ids to (if (ids.size == 1) first?.customTags.orEmpty() else "")
                                            },
                                            onShareTracks = { chosen -> viewModel.shareTracks(chosen) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onOpenArtist = { artist -> navController.navigate(CadenceRoute.ArtistDetail(artist, false)) },
                                            onOpenAlbum = { albName, albArt -> navController.navigate(CadenceRoute.AlbumDetail(albName, albArt)) },
                                            onSaveAsPlaylist = if (mix != null) {
                                                { viewModel.saveGeneratedMixAsPlaylist(mix) }
                                            } else null
                                        )
                                    }

                                    composable<CadenceRoute.PlaylistDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.PlaylistDetail>()
                                        val plMeta = playlists.firstOrNull { it.id == route.playlistId }
                                        val tracks by remember(route.playlistId) {
                                            viewModel.container.database.playlistDao().observePlaylistTracks(route.playlistId)
                                        }.collectAsState(initial = emptyList())
                                        TrackCollectionDetailScreen(
                                            title = plMeta?.name ?: "Playlist",
                                            subtitle = "${tracks.size} tracks • ${TextNormalizer.formatDuration(tracks.sumOf { it.durationMs })}",
                                            tracks = tracks,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onPlayAll = { idx, shuf -> viewModel.container.playerConnection.playTracks(tracks, idx, shuf) },
                                            onPlayNext = { chosen ->
                                                viewModel.container.playerConnection.addPlayNext(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                                            },
                                            onQueueTracks = { chosen ->
                                                viewModel.container.playerConnection.addToEnd(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s).")
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                val first = tracks.firstOrNull { it.id in ids }
                                                userTagsTarget = ids to (if (ids.size == 1) first?.customTags.orEmpty() else "")
                                            },
                                            onShareTracks = { chosen -> viewModel.shareTracks(chosen) },
                                            onRemoveSelectedFromCollection = { selectedIds ->
                                                viewModel.removeTracksFromPlaylist(route.playlistId, selectedIds)
                                            },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onTrackMore = { tr ->
                                                openTrackSheet(
                                                    tr,
                                                    onRemoveFromPlaylist = {
                                                        viewModel.removeTracksFromPlaylist(route.playlistId, setOf(tr.id))
                                                    }
                                                )
                                            },
                                            onOpenArtist = { artist -> navController.navigate(CadenceRoute.ArtistDetail(artist, false)) },
                                            onOpenAlbum = { albName, albArt -> navController.navigate(CadenceRoute.AlbumDetail(albName, albArt)) }
                                        )
                                    }

                                    composable<CadenceRoute.SmartPlaylistDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.SmartPlaylistDetail>()
                                        val sp = smartPlaylists.firstOrNull { it.id == route.smartPlaylistId }
                                        val tracks by remember(route.smartPlaylistId) {
                                            viewModel.observeSmartPlaylistTracks(route.smartPlaylistId)
                                        }.collectAsState(initial = emptyList())
                                        TrackCollectionDetailScreen(
                                            title = sp?.name ?: "Smart Playlist",
                                            subtitle = "${tracks.size} dynamic matches",
                                            tracks = tracks,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onPlayAll = { idx, shuf -> viewModel.container.playerConnection.playTracks(tracks, idx, shuf) },
                                            onPlayNext = { chosen ->
                                                viewModel.container.playerConnection.addPlayNext(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                                            },
                                            onQueueTracks = { chosen ->
                                                viewModel.container.playerConnection.addToEnd(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s).")
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                val first = tracks.firstOrNull { it.id in ids }
                                                userTagsTarget = ids to (if (ids.size == 1) first?.customTags.orEmpty() else "")
                                            },
                                            onShareTracks = { chosen -> viewModel.shareTracks(chosen) },
                                            onEditSmartRules = if (sp != null) { { editingSmartPlaylist = sp } } else null,
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onOpenArtist = { artist -> navController.navigate(CadenceRoute.ArtistDetail(artist, false)) },
                                            onOpenAlbum = { albName, albArt -> navController.navigate(CadenceRoute.AlbumDetail(albName, albArt)) }
                                        )
                                    }

                                    composable<CadenceRoute.FolderDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.FolderDetail>()
                                        val tracks by remember(route.folderPath) {
                                            viewModel.observeTracksForScreen(
                                                "folder_${route.folderPath}",
                                                extraWhere = "folderPath = ?",
                                                extraArgs = listOf(route.folderPath)
                                            )
                                        }.collectAsState(initial = emptyList())
                                        TrackCollectionDetailScreen(
                                            title = route.folderName,
                                            subtitle = "${tracks.size} tracks • ${route.folderPath}",
                                            tracks = tracks,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onPlayAll = { idx, shuf -> viewModel.container.playerConnection.playTracks(tracks, idx, shuf) },
                                            onPlayNext = { chosen ->
                                                viewModel.container.playerConnection.addPlayNext(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                                            },
                                            onQueueTracks = { chosen ->
                                                viewModel.container.playerConnection.addToEnd(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s).")
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                val first = tracks.firstOrNull { it.id in ids }
                                                userTagsTarget = ids to (if (ids.size == 1) first?.customTags.orEmpty() else "")
                                            },
                                            onShareTracks = { chosen -> viewModel.shareTracks(chosen) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onOpenArtist = { artist -> navController.navigate(CadenceRoute.ArtistDetail(artist, false)) },
                                            onOpenAlbum = { albName, albArt -> navController.navigate(CadenceRoute.AlbumDetail(albName, albArt)) }
                                        )
                                    }

                                    composable<CadenceRoute.GenreDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.GenreDetail>()
                                        val tracks by remember(route.genreName) {
                                            viewModel.observeTracksForScreen(
                                                "genre_${route.genreName}",
                                                extraWhere = "COALESCE(NULLIF(genre, ''), 'Unknown Genre') = ?",
                                                extraArgs = listOf(route.genreName)
                                            )
                                        }.collectAsState(initial = emptyList())
                                        TrackCollectionDetailScreen(
                                            title = route.genreName,
                                            subtitle = "${tracks.size} tracks",
                                            tracks = tracks,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onPlayAll = { idx, shuf -> viewModel.container.playerConnection.playTracks(tracks, idx, shuf) },
                                            onPlayNext = { chosen ->
                                                viewModel.container.playerConnection.addPlayNext(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                                            },
                                            onQueueTracks = { chosen ->
                                                viewModel.container.playerConnection.addToEnd(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s).")
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                val first = tracks.firstOrNull { it.id in ids }
                                                userTagsTarget = ids to (if (ids.size == 1) first?.customTags.orEmpty() else "")
                                            },
                                            onShareTracks = { chosen -> viewModel.shareTracks(chosen) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onOpenArtist = { artist -> navController.navigate(CadenceRoute.ArtistDetail(artist, false)) },
                                            onOpenAlbum = { albName, albArt -> navController.navigate(CadenceRoute.AlbumDetail(albName, albArt)) }
                                        )
                                    }

                                    composable<CadenceRoute.YearDetail> { entry ->
                                        val route = entry.toRoute<CadenceRoute.YearDetail>()
                                        val tracks by remember(route.year) {
                                            viewModel.observeTracksForScreen(
                                                "year_${route.year}",
                                                extraWhere = "year = ?",
                                                extraArgs = listOf(route.year)
                                            )
                                        }.collectAsState(initial = emptyList())
                                        TrackCollectionDetailScreen(
                                            title = route.year.toString(),
                                            subtitle = "${tracks.size} tracks",
                                            tracks = tracks,
                                            currentTrackId = playback.currentTrack?.id,
                                            onBack = { navController.popBackStack() },
                                            onPlayAll = { idx, shuf -> viewModel.container.playerConnection.playTracks(tracks, idx, shuf) },
                                            onPlayNext = { chosen ->
                                                viewModel.container.playerConnection.addPlayNext(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                                            },
                                            onQueueTracks = { chosen ->
                                                viewModel.container.playerConnection.addToEnd(chosen)
                                                viewModel.showMessage("Queued ${chosen.size} track(s).")
                                            },
                                            onAddToPlaylist = { ids -> playlistTargetIds = ids },
                                            onBatchTagEdit = { ids ->
                                                navController.navigate(CadenceRoute.TagEditor(ids.joinToString(",")))
                                            },
                                            onManageUserTags = { ids ->
                                                val first = tracks.firstOrNull { it.id in ids }
                                                userTagsTarget = ids to (if (ids.size == 1) first?.customTags.orEmpty() else "")
                                            },
                                            onShareTracks = { chosen -> viewModel.shareTracks(chosen) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onTrackMore = { tr -> openTrackSheet(tr) },
                                            onOpenArtist = { artist -> navController.navigate(CadenceRoute.ArtistDetail(artist, false)) },
                                            onOpenAlbum = { albName, albArt -> navController.navigate(CadenceRoute.AlbumDetail(albName, albArt)) }
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
}

private fun navIconFor(key: NavDestinationKey): ImageVector {
    return when (key) {
        NavDestinationKey.HOME -> Icons.Default.Home
        NavDestinationKey.ARTISTS -> Icons.Default.Person
        NavDestinationKey.ALBUMS -> Icons.Default.Album
        NavDestinationKey.FILES -> Icons.Default.Folder
        NavDestinationKey.PLAYLISTS -> Icons.Default.QueueMusic
        NavDestinationKey.SONGS -> Icons.Default.MusicNote
        NavDestinationKey.GENRES -> Icons.Default.Category
        NavDestinationKey.YEARS -> Icons.Default.CalendarToday
        NavDestinationKey.COMPOSERS -> Icons.Default.Person
        NavDestinationKey.SMART_VIEWS -> Icons.Default.AutoAwesome
        NavDestinationKey.LIBRARY -> Icons.Default.LibraryMusic
        NavDestinationKey.SEARCH -> Icons.Default.Search
        NavDestinationKey.SETTINGS -> Icons.Default.Settings
    }
}

private fun loadSmallTrackBitmap(context: Context, artworkUri: String?, contentUri: String): Bitmap? {
    val opts = BitmapFactory.Options().apply {
        inSampleSize = 4
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    runCatching {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, Uri.parse(contentUri))
        val bytes = mmr.embeddedPicture
        mmr.release()
        if (bytes != null) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        }
    }
    if (!artworkUri.isNullOrBlank()) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(artworkUri))?.use { stream ->
                return BitmapFactory.decodeStream(stream, null, opts)
            }
        }
    }
    return null
}
