package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.core.model.NowPlayingStyle
import com.example.core.model.RatingSystemMode
import com.example.core.model.RepeatModeOption
import com.example.core.util.TextNormalizer
import com.example.data.db.TrackEntity
import com.example.player.SleepTimerMode
import com.example.player.SleepTimerState
import com.example.ui.components.CodecInfoDialog
import com.example.ui.components.CollectionBatchActionBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InlineArtistAlbumMeta
import com.example.ui.components.PlaybackSpeedDialog
import com.example.ui.components.SingleLineButtonLabel
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackListItem
import com.example.ui.theme.ArtworkPaletteColors
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.LocalRatingSystemMode
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MainViewModel

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    artworkPalette: ArtworkPaletteColors?,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: (Long) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTagEditor: (Long) -> Unit,
    onOpenAlbum: (String, String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onManageUserTags: (TrackEntity) -> Unit
) {
    val playback by viewModel.playbackState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val track = playback.currentTrack
    val ext = LocalCadenceTheme.current
    val ratingMode = LocalRatingSystemMode.current

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showCodecDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var rotateDisc by remember { mutableStateOf(false) }

    val sleepState by (viewModel.container.sleepTimerController?.state
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(SleepTimerState()) }).collectAsState()

    val style = settings.themeConfig.nowPlayingStyle
    val accent = if (settings.themeConfig.useArtworkColors && artworkPalette != null) {
        artworkPalette.vibrant
    } else {
        MaterialTheme.colors.primary
    }

    val useArtBg = (settings.themeConfig.useArtworkColors || style == NowPlayingStyle.ARTWORK_FOCUSED || style == NowPlayingStyle.FULL_SCREEN) && artworkPalette != null
    val bgModifier = if (useArtBg) {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    artworkPalette!!.dominant.copy(alpha = 0.56f),
                    artworkPalette.muted.copy(alpha = 0.28f),
                    MaterialTheme.colors.background
                )
            )
        )
    } else {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    accent.copy(alpha = if (ext.isDark) 0.14f else 0.08f),
                    MaterialTheme.colors.background,
                    MaterialTheme.colors.background
                )
            )
        )
    }

    if (track == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = { Text("Now Playing") },
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
            EmptyStateView("Nothing Playing", "Pick a track from your library to begin playback.")
        }
        return
    }

    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = playback.playbackSpeed,
            currentPitch = playback.playbackPitch,
            onApply = { s, p -> viewModel.container.playerConnection.setPlaybackSpeedAndPitch(s, p) },
            onDismiss = { showSpeedDialog = false }
        )
    }
    if (showSleepDialog) {
        SleepTimerDialog(
            state = sleepState,
            onStartMinutes = { m, f -> viewModel.container.sleepTimerController?.startMinutes(m, f) },
            onStopAfterCurrent = { f -> viewModel.container.sleepTimerController?.startStopAfterCurrentTrack(f) },
            onStartAfterTracks = { c, f -> viewModel.container.sleepTimerController?.startAfterTrackCount(c, f) },
            onCancelTimer = { viewModel.container.sleepTimerController?.cancel() },
            onDismiss = { showSleepDialog = false }
        )
    }
    if (showCodecDialog) {
        CodecInfoDialog(track = track, onDismiss = { showCodecDialog = false })
    }
    if (showBookmarksDialog) {
        BookmarksDialog(
            trackId = track.id,
            viewModel = viewModel,
            onSeek = { viewModel.container.playerConnection.seekTo(it) },
            onDismiss = { showBookmarksDialog = false }
        )
    }

    val spinAngle = if (rotateDisc && playback.isPlaying && !settings.themeConfig.reducedMotion) {
        val infinite = rememberInfiniteTransition(label = "disc_spin")
        val anim by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
            label = "angle"
        )
        anim
    } else 0f

    val isCompactWidth = LocalIsCompactWidth.current
    val hPad = if (isCompactWidth) CadenceSpacing.md else CadenceSpacing.lg

    // Fixed-height non-scrollable Now Playing viewport
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(bgModifier)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("now_playing_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            contentColor = MaterialTheme.colors.onBackground,
            title = {
                Column(
                    modifier = Modifier.clickable { onOpenAlbum(track.album, track.albumArtist.ifBlank { track.artist }) }
                ) {
                    Text(
                        text = track.album,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val qLabel = if (playback.queueSize > 0) " • ${playback.currentMediaIndex + 1} / ${playback.queueSize}" else ""
                    Text(
                        text = "Playing from Album$qLabel",
                        style = MaterialTheme.typography.caption.tabular(),
                        color = ext.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Now Playing")
                }
            },
            actions = {
                IconButton(onClick = onOpenQueue, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Active Queue")
                }
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("np_more_btn")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More player options")
                    }
                    DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            onAddToPlaylist(track.id)
                        }) { Text("Add to Playlist") }
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            onOpenAlbum(track.album, track.albumArtist.ifBlank { track.artist })
                        }) { Text("Go to Album") }
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            onOpenArtist(track.artist)
                        }) { Text("Go to Artist") }
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            onOpenTagEditor(track.id)
                        }) { Text("Edit Tags & Artwork") }
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            onManageUserTags(track)
                        }) { Text("User-Defined Tags") }
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            showBookmarksDialog = true
                        }) { Text("Track Bookmarks") }
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            showCodecDialog = true
                        }) { Text("File & Codec Details") }
                        DropdownMenuItem(onClick = {
                            showOverflowMenu = false
                            viewModel.shareTracks(listOf(track))
                        }) { Text("Share Audio") }
                    }
                }
            }
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = hPad)
        ) {
            val isTightHeight = maxHeight < 580.dp
            val artMaxFrac = when (style) {
                NowPlayingStyle.COMPACT -> 0.34f
                NowPlayingStyle.ARTWORK_FOCUSED, NowPlayingStyle.FULL_SCREEN -> 0.45f
                else -> 0.40f
            }
            val artSize = min(
                maxWidth * if (isCompactWidth) 0.78f else 0.84f,
                maxHeight * artMaxFrac
            ).coerceAtLeast(132.dp)

            val fabSize = if (isCompactWidth || isTightHeight) 56.dp else 64.dp
            var artSwipeAccumulated by remember { mutableFloatStateOf(0f) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = CadenceSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Framed Album Artwork Centered in Remaining Space
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = if (rotateDisc) CircleShape else RoundedCornerShape((settings.themeConfig.artworkCornerRadiusDp + 6).coerceAtLeast(12).dp),
                        elevation = 10.dp,
                        border = BorderStroke(1.dp, ext.subtleBorder),
                        color = ext.elevatedSurface,
                        modifier = Modifier
                            .size(artSize)
                            .pointerInput(track.id) {
                                detectHorizontalDragGestures(
                                    onDragStart = { artSwipeAccumulated = 0f },
                                    onHorizontalDrag = { _, dragAmount -> artSwipeAccumulated += dragAmount },
                                    onDragEnd = {
                                        if (artSwipeAccumulated < -110f) {
                                            viewModel.container.playerConnection.next()
                                        } else if (artSwipeAccumulated > 110f) {
                                            viewModel.container.playerConnection.previous()
                                        }
                                        artSwipeAccumulated = 0f
                                    }
                                )
                            }
                            .clickable { if (!settings.themeConfig.reducedMotion) rotateDisc = !rotateDisc }
                    ) {
                        TrackArtwork(
                            artworkUri = track.artworkUri,
                            contentUri = track.contentUri,
                            contentDescription = track.title,
                            cornerRadius = if (rotateDisc) 500.dp else (settings.themeConfig.artworkCornerRadiusDp + 6).coerceAtLeast(12).dp,
                            highRes = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(spinAngle)
                                .then(if (rotateDisc) Modifier.clip(CircleShape) else Modifier)
                        )
                    }
                }

                // Codec + A-B Status Tonal Pill
                val sr = if (track.sampleRateHz > 0) " • ${track.sampleRateHz / 1000.0} kHz" else ""
                val br = if (track.bitrateKbps > 0) " • ${track.bitrateKbps} kbps" else ""
                val abNote = if (playback.abLoopStartMs != null) {
                    if (playback.abLoopEndMs != null) " • A-B Loop" else " • A Set"
                } else ""
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(0.5.dp, accent.copy(alpha = 0.28f))
                ) {
                    Text(
                        text = "${track.format}$sr$br$abNote",
                        style = MaterialTheme.typography.overline.tabular(),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(if (isTightHeight) 6.dp else 10.dp))

                // Title, Clickable Artist & Album Meta, Exclusive Rating/Heart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = if (isCompactWidth || isTightHeight) MaterialTheme.typography.h6 else MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        InlineArtistAlbumMeta(
                            artist = track.artist,
                            album = track.album,
                            albumArtist = track.albumArtist.ifBlank { track.artist },
                            clickableLinks = true,
                            onArtistClick = onOpenArtist,
                            onAlbumClick = onOpenAlbum,
                            textStyle = MaterialTheme.typography.body2
                        )
                    }
                    if (ratingMode == RatingSystemMode.HEARTS) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(track) },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("np_favorite")
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (track.isFavorite) MaterialTheme.colors.secondary else ext.subtleText
                            )
                        }
                    }
                }

                // 5-Star bar (STARS mode) + Compact Bookmark trigger
                if (style != NowPlayingStyle.MINIMAL || ratingMode == RatingSystemMode.STARS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (ratingMode == RatingSystemMode.STARS) {
                            val starSize = if (isCompactWidth) 30.dp else 34.dp
                            for (star in 1..5) {
                                IconButton(
                                    onClick = {
                                        val next = if (track.rating == star) 0 else star
                                        viewModel.setTrackRating(track.id, next)
                                    },
                                    modifier = Modifier.size(starSize).testTag("np_star_$star")
                                ) {
                                    Icon(
                                        imageVector = if (star <= track.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Rate $star stars",
                                        tint = if (star <= track.rating) accent else ext.subtleText,
                                        modifier = Modifier.size(if (isCompactWidth) 17.dp else 19.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (track.playCount > 0) "${track.playCount} plays" else "Local audio",
                                style = MaterialTheme.typography.caption.tabular(),
                                color = ext.subtleText,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (style != NowPlayingStyle.MINIMAL) {
                            TextButton(
                                onClick = { showBookmarksDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                SingleLineButtonLabel(
                                    if (isCompactWidth) "Marks" else "Bookmarks",
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }
                }

                // Seek Section
                NowPlayingSeekSection(
                    viewModel = viewModel,
                    accent = accent
                )

                // Primary Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.container.playerConnection.toggleShuffle() },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("np_shuffle")
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playback.shuffleEnabled) accent else ext.subtleText
                        )
                    }
                    IconButton(
                        onClick = { viewModel.container.playerConnection.previous() },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("np_prev")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
                    }
                    FloatingActionButton(
                        onClick = { viewModel.container.playerConnection.togglePlayPause() },
                        backgroundColor = accent,
                        contentColor = contrastOnColor(accent),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
                        modifier = Modifier.size(fabSize).testTag("np_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or Pause",
                            modifier = Modifier.size(if (isCompactWidth) 28.dp else 32.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.container.playerConnection.next() },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("np_next")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
                    }
                    IconButton(
                        onClick = { viewModel.container.playerConnection.cycleRepeatMode() },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("np_repeat")
                    ) {
                        Icon(
                            imageVector = if (playback.repeatMode == RepeatModeOption.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat Mode",
                            tint = if (playback.repeatMode != RepeatModeOption.OFF) accent else ext.subtleText
                        )
                    }
                }

                // Tonal Studio Controls Pill Strip
                Surface(
                    shape = RoundedCornerShape(CadenceRadius.md),
                    color = ext.elevatedSurface.copy(alpha = 0.85f),
                    border = BorderStroke(0.75.dp, ext.subtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.container.playerConnection.seekBy(-settings.seekIntervalSec * 1000L) }) {
                            Icon(Icons.Default.Replay10, contentDescription = "Seek back", modifier = Modifier.size(20.dp))
                        }
                        TextButton(
                            onClick = {
                                when {
                                    playback.abLoopStartMs == null -> viewModel.container.playerConnection.setAbLoopPoint(true)
                                    playback.abLoopEndMs == null -> viewModel.container.playerConnection.setAbLoopPoint(false)
                                    else -> viewModel.container.playerConnection.clearAbLoop()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            SingleLineButtonLabel(
                                text = when {
                                    playback.abLoopStartMs == null -> "A-B"
                                    playback.abLoopEndMs == null -> "Set B"
                                    else -> "Clear A-B"
                                },
                                style = MaterialTheme.typography.caption.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (playback.abLoopStartMs != null) accent else MaterialTheme.colors.onSurface
                            )
                        }
                        TextButton(
                            onClick = { showSpeedDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val speedText = if (playback.playbackSpeed != 1.0f || playback.playbackPitch != 1.0f) {
                                String.format("%.2fx", playback.playbackSpeed)
                            } else {
                                "1.0x"
                            }
                            SingleLineButtonLabel(
                                text = speedText,
                                style = MaterialTheme.typography.caption.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (playback.playbackSpeed != 1.0f || playback.playbackPitch != 1.0f) accent else MaterialTheme.colors.onSurface
                            )
                        }
                        IconButton(onClick = { showSleepDialog = true }) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepState.mode != SleepTimerMode.OFF) accent else MaterialTheme.colors.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.container.playerConnection.seekBy(settings.seekIntervalSec * 1000L) }) {
                            Icon(Icons.Default.Forward10, contentDescription = "Seek forward", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Bottom Tool Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PlayerToolButton(Icons.AutoMirrored.Filled.QueueMusic, "Queue", "np_queue_btn", Modifier.weight(1f)) { onOpenQueue() }
                    PlayerToolButton(Icons.Default.Lyrics, "Lyrics", "np_lyrics_btn", Modifier.weight(1f)) { onOpenLyrics(track.id) }
                    PlayerToolButton(Icons.Default.Equalizer, "EQ", "np_eq_btn", Modifier.weight(1f)) { onOpenEqualizer() }
                    PlayerToolButton(Icons.Default.Edit, "Tags", "np_tags_btn", Modifier.weight(1f)) { onOpenTagEditor(track.id) }
                    PlayerToolButton(Icons.Default.Share, "Share", "np_share_btn", Modifier.weight(1f)) { viewModel.shareTracks(listOf(track)) }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingSeekSection(
    viewModel: MainViewModel,
    accent: Color
) {
    val progress by viewModel.playbackProgress.collectAsState()
    val ext = LocalCadenceTheme.current
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val duration = progress.durationMs.coerceAtLeast(1L)
    val sliderPos = if (isDragging) dragFraction else (progress.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val displayPosMs = if (isDragging) (dragFraction * duration).toLong() else progress.positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPos,
            onValueChange = {
                isDragging = true
                dragFraction = it
            },
            onValueChangeFinished = {
                viewModel.container.playerConnection.seekTo((dragFraction * duration).toLong())
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = ext.dividerColor
            ),
            modifier = Modifier.testTag("np_seekbar")
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = TextNormalizer.formatDuration(displayPosMs),
                style = MaterialTheme.typography.overline.tabular(),
                color = ext.subtleText,
                maxLines = 1
            )
            Text(
                text = "-${TextNormalizer.formatDuration((duration - displayPosMs).coerceAtLeast(0L))}",
                style = MaterialTheme.typography.overline.tabular(),
                color = ext.subtleText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PlayerToolButton(
    icon: ImageVector,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = ext.subtleText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BookmarksDialog(
    trackId: Long,
    viewModel: MainViewModel,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val bookmarks by viewModel.observeBookmarks(trackId).collectAsState(initial = emptyList())
    val progress by viewModel.playbackProgress.collectAsState()
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track Bookmarks") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (e.g. Solo)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        viewModel.addBookmark(trackId, label, progress.positionMs)
                        label = ""
                    }) { SingleLineButtonLabel("Add") }
                }
                Spacer(Modifier.height(10.dp))
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks saved for this track yet.", style = MaterialTheme.typography.caption)
                } else {
                    bookmarks.forEach { bm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeek(bm.positionMs); onDismiss() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(TextNormalizer.formatDuration(bm.positionMs), fontFamily = JetBrainsMonoFamily, color = MaterialTheme.colors.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(bm.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { viewModel.deleteBookmark(bm.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete bookmark")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { SingleLineButtonLabel("CLOSE") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAddTracksToPlaylist: (List<Long>) -> Unit,
    onOpenTagEditor: (List<Long>) -> Unit,
    onTrackMore: (TrackEntity) -> Unit,
    onOpenArtist: ((String) -> Unit)? = null,
    onOpenAlbum: ((String, String) -> Unit)? = null
) {
    val queue by viewModel.queueItems.collectAsState()
    val history by viewModel.queueHistory.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("Saved Queue") }
    var selectedQueueIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val ext = LocalCadenceTheme.current

    BackHandler(enabled = selectedQueueIndices.isNotEmpty()) {
        selectedQueueIndices = emptySet()
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Queue as Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveQueueAsPlaylist(playlistName)
                    showSaveDialog = false
                }) { SingleLineButtonLabel("SAVE") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { SingleLineButtonLabel("CANCEL") } }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Active Queue?") },
            text = { Text("Stop playback and remove all ${queue.size} track(s) from the current queue?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.container.playerConnection.clearQueue()
                    selectedQueueIndices = emptySet()
                    showClearConfirm = false
                }) { SingleLineButtonLabel("CLEAR") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { SingleLineButtonLabel("CANCEL") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("queue_screen")
    ) {
        if (selectedQueueIndices.isNotEmpty() && selectedTab == 0) {
            CollectionBatchActionBar(
                selectedCount = selectedQueueIndices.size,
                onCancel = { selectedQueueIndices = emptySet() },
                onSelectAll = { selectedQueueIndices = queue.indices.toSet() },
                onPlayNext = {
                    val chosen = selectedQueueIndices.sorted().mapNotNull { idx ->
                        val q = queue.getOrNull(idx) ?: return@mapNotNull null
                        allTracks.firstOrNull { it.id == q.trackId }
                    }
                    if (chosen.isNotEmpty()) {
                        viewModel.container.playerConnection.addPlayNext(chosen)
                        viewModel.showMessage("Queued ${chosen.size} track(s) next.")
                    }
                    selectedQueueIndices = emptySet()
                },
                onAddToQueue = {
                    val chosen = selectedQueueIndices.sorted().mapNotNull { idx ->
                        val q = queue.getOrNull(idx) ?: return@mapNotNull null
                        allTracks.firstOrNull { it.id == q.trackId }
                    }
                    if (chosen.isNotEmpty()) {
                        viewModel.container.playerConnection.addToEnd(chosen)
                    }
                    selectedQueueIndices = emptySet()
                },
                onAddToPlaylist = {
                    val ids = selectedQueueIndices.sorted().mapNotNull { queue.getOrNull(it)?.trackId }.distinct()
                    if (ids.isNotEmpty()) onAddTracksToPlaylist(ids)
                    selectedQueueIndices = emptySet()
                },
                onBatchTagEdit = {
                    val ids = selectedQueueIndices.sorted().mapNotNull { queue.getOrNull(it)?.trackId }.distinct()
                    if (ids.isNotEmpty()) onOpenTagEditor(ids)
                    selectedQueueIndices = emptySet()
                },
                onRemove = {
                    selectedQueueIndices.sortedDescending().forEach { idx ->
                        viewModel.container.playerConnection.removeQueueItem(idx)
                    }
                    selectedQueueIndices = emptySet()
                },
                removeLabel = "Remove from Queue"
            )
        } else {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                title = { Text("Queue (${queue.size})", style = MaterialTheme.typography.h6.tabular()) },
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (queue.isNotEmpty()) {
                                val tracks = queue.mapNotNull { q -> allTracks.firstOrNull { it.id == q.trackId } }
                                if (tracks.isNotEmpty()) {
                                    viewModel.container.playerConnection.playTracks(tracks.shuffled(), 0, shuffle = true)
                                    viewModel.showMessage("Shuffled queue.")
                                }
                            }
                        },
                        enabled = queue.size > 1
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle Queue")
                    }
                    IconButton(onClick = { showSaveDialog = true }, enabled = queue.isNotEmpty()) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Save Queue as Playlist")
                    }
                    IconButton(onClick = { showClearConfirm = true }, enabled = queue.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Queue")
                    }
                }
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { SingleLineButtonLabel("Up Next (${queue.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; selectedQueueIndices = emptySet() },
                text = { SingleLineButtonLabel("History (${history.size})") }
            )
        }

        if (selectedTab == 0) {
            if (queue.isEmpty()) {
                EmptyStateView("Queue Is Empty", "Play songs or albums to build an active queue.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(queue, key = { _, q -> "qi_${q.queueEntryId}" }, contentType = { _, _ -> "queue_item" }) { idx, item ->
                        val isCurrent = idx == playback.currentMediaIndex
                        val isSelected = idx in selectedQueueIndices
                        val selectionMode = selectedQueueIndices.isNotEmpty()
                        var itemMenuOpen by remember { mutableStateOf(false) }
                        val resolvedTrack = remember(item.trackId, allTracks) {
                            allTracks.firstOrNull { it.id == item.trackId }
                        }

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colors.primary.copy(alpha = 0.14f)
                                            isCurrent -> MaterialTheme.colors.primary.copy(alpha = 0.08f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                selectedQueueIndices = if (isSelected) selectedQueueIndices - idx else selectedQueueIndices + idx
                                            } else {
                                                viewModel.container.playerConnection.playTrackFromQueueIndex(idx)
                                            }
                                        },
                                        onLongClick = {
                                            selectedQueueIndices = if (isSelected) selectedQueueIndices - idx else selectedQueueIndices + idx
                                        }
                                    )
                                    .padding(start = CadenceSpacing.md, end = CadenceSpacing.xs, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedQueueIndices = if (isSelected) selectedQueueIndices - idx else selectedQueueIndices + idx
                                        },
                                        modifier = Modifier.padding(end = CadenceSpacing.xs)
                                    )
                                } else if (isCurrent) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = "Now Playing",
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.width(26.dp).size(18.dp)
                                    )
                                } else {
                                    Text(
                                        "${idx + 1}",
                                        style = MaterialTheme.typography.overline.tabular(),
                                        color = ext.subtleText,
                                        modifier = Modifier.width(26.dp)
                                    )
                                }
                                TrackArtwork(item.artworkUri, item.contentUri, item.title, Modifier.size(44.dp))
                                Spacer(Modifier.width(CadenceSpacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.subtitle2,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isCurrent) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                    )
                                    Text(
                                        "${item.artist} • ${TextNormalizer.formatDuration(item.durationMs)}",
                                        style = MaterialTheme.typography.caption.tabular(),
                                        color = ext.subtleText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box {
                                    IconButton(
                                        onClick = { itemMenuOpen = true },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Queue item actions", tint = ext.subtleText)
                                    }
                                    DropdownMenu(expanded = itemMenuOpen, onDismissRequest = { itemMenuOpen = false }) {
                                        DropdownMenuItem(onClick = {
                                            itemMenuOpen = false
                                            viewModel.container.playerConnection.playTrackFromQueueIndex(idx)
                                        }) { Text("Play Now") }
                                        if (idx > 0) {
                                            DropdownMenuItem(onClick = {
                                                itemMenuOpen = false
                                                viewModel.container.playerConnection.moveQueueItem(idx, 0)
                                            }) { Text("Move to Top") }
                                            DropdownMenuItem(onClick = {
                                                itemMenuOpen = false
                                                viewModel.container.playerConnection.moveQueueItem(idx, idx - 1)
                                            }) { Text("Move Up") }
                                        }
                                        if (idx < queue.lastIndex) {
                                            DropdownMenuItem(onClick = {
                                                itemMenuOpen = false
                                                viewModel.container.playerConnection.moveQueueItem(idx, idx + 1)
                                            }) { Text("Move Down") }
                                        }
                                        val nextTarget = (playback.currentMediaIndex + 1).coerceIn(0, queue.lastIndex)
                                        if (idx != nextTarget) {
                                            DropdownMenuItem(onClick = {
                                                itemMenuOpen = false
                                                viewModel.container.playerConnection.moveQueueItem(idx, nextTarget)
                                            }) { Text("Move to Play Next") }
                                        }
                                        DropdownMenuItem(onClick = {
                                            itemMenuOpen = false
                                            onAddTracksToPlaylist(listOf(item.trackId))
                                        }) { Text("Add to Playlist") }
                                        DropdownMenuItem(onClick = {
                                            itemMenuOpen = false
                                            onOpenTagEditor(listOf(item.trackId))
                                        }) { Text("Edit Tags") }
                                        if (resolvedTrack != null) {
                                            DropdownMenuItem(onClick = {
                                                itemMenuOpen = false
                                                onTrackMore(resolvedTrack)
                                            }) { Text("More Track Actions...") }
                                        }
                                        DropdownMenuItem(onClick = {
                                            itemMenuOpen = false
                                            viewModel.container.playerConnection.removeQueueItem(idx)
                                        }) { Text("Remove from Queue", color = MaterialTheme.colors.error) }
                                    }
                                }
                            }
                            Divider(color = ext.dividerColor, startIndent = 74.dp)
                        }
                    }
                }
            }
        } else {
            if (history.isEmpty()) {
                EmptyStateView("No Queue History", "Tracks played during this or past sessions appear here.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(history, key = { idx, t -> "qh_${idx}_${t.id}" }, contentType = { _, _ -> "track" }) { idx, track ->
                        TrackListItem(
                            track = track,
                            isPlayingCurrent = playback.currentTrack?.id == track.id,
                            onClick = { viewModel.container.playerConnection.playTracks(history, idx) },
                            onLongClick = { onTrackMore(track) },
                            onMoreClick = { onTrackMore(track) },
                            onToggleFavorite = { viewModel.toggleFavorite(track) },
                            onArtistClick = onOpenArtist,
                            onAlbumClick = onOpenAlbum
                        )
                    }
                }
            }
        }
    }
}
