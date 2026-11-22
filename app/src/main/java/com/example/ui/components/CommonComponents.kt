package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.model.CardAppearance
import com.example.core.model.MiniPlayerStyle
import com.example.core.model.NavDestinationKey
import com.example.core.model.RatingSystemMode
import com.example.core.model.SortDirection
import com.example.core.model.SortField
import com.example.core.util.TextNormalizer
import com.example.data.db.AlbumSummary
import com.example.data.db.ArtistSummary
import com.example.data.db.FolderSummary
import com.example.data.db.GenreSummary
import com.example.data.db.PlaylistWithCount
import com.example.data.db.SmartPlaylistEntity
import com.example.data.db.TrackEntity
import com.example.data.db.YearSummary
import com.example.data.preferences.ScreenSortPreference
import com.example.player.PlaybackProgressState
import com.example.player.SleepTimerMode
import com.example.player.SleepTimerState
import com.example.ui.theme.CadenceRadius
import com.example.ui.theme.CadenceSpacing
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.LocalIsCompactWidth
import com.example.ui.theme.LocalRatingSystemMode
import com.example.ui.theme.tabular

sealed interface CollectionSheetTarget {
    data class Album(val album: AlbumSummary) : CollectionSheetTarget
    data class Artist(val artist: ArtistSummary, val isAlbumArtist: Boolean = false) : CollectionSheetTarget
    data class Genre(val genre: GenreSummary) : CollectionSheetTarget
    data class Year(val year: YearSummary) : CollectionSheetTarget
    data class Folder(val folder: FolderSummary) : CollectionSheetTarget
    data class Playlist(val playlist: PlaylistWithCount) : CollectionSheetTarget
    data class SmartPlaylist(val smartPlaylist: SmartPlaylistEntity) : CollectionSheetTarget
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListItem(
    track: TrackEntity,
    isPlayingCurrent: Boolean,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((String, String) -> Unit)? = null,
    leadingNumber: String? = null,
    hideArtistInSubtitle: Boolean = false,
    hideAlbumInSubtitle: Boolean = false,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    val ratingMode = LocalRatingSystemMode.current
    val isCompact = LocalIsCompactWidth.current
    val vPad = ext.config.density.rowVerticalPaddingDp.dp
    val hStartPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    val artSize = if (isCompact) 42.dp else 48.dp
    val leadingWidth = if (leadingNumber != null) 28.dp else artSize
    val artGap = if (isCompact) CadenceSpacing.sm else CadenceSpacing.md
    val bgColor = when {
        isSelected -> MaterialTheme.colors.primary.copy(alpha = 0.14f)
        isPlayingCurrent -> MaterialTheme.colors.primary.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = hStartPad, end = CadenceSpacing.xs, top = vPad, bottom = vPad)
                .testTag("track_row_${track.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = CadenceSpacing.xs)
                )
            }
            if (leadingNumber != null) {
                Box(
                    modifier = Modifier.width(leadingWidth),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlayingCurrent) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Now Playing",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = leadingNumber,
                            style = MaterialTheme.typography.caption.tabular(),
                            color = ext.subtleText,
                            maxLines = 1
                        )
                    }
                }
            } else {
                TrackArtwork(
                    artworkUri = track.artworkUri,
                    contentUri = track.contentUri,
                    contentDescription = track.title,
                    modifier = Modifier.size(artSize)
                )
            }
            Spacer(Modifier.width(artGap))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPlayingCurrent && leadingNumber == null) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Now Playing",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .size(15.dp)
                                .padding(end = CadenceSpacing.xs)
                        )
                    }
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = if (isPlayingCurrent) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isPlayingCurrent) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                InlineArtistAlbumMeta(
                    artist = if (hideArtistInSubtitle) "" else track.artist,
                    album = if (hideAlbumInSubtitle) "" else track.album,
                    albumArtist = track.albumArtist.ifBlank { track.artist },
                    clickableLinks = !selectionMode,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick
                )
            }
            Spacer(Modifier.width(CadenceSpacing.sm))
            if (ratingMode == RatingSystemMode.HEARTS && track.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colors.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            } else if (ratingMode == RatingSystemMode.STARS && track.rating > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "${track.rating} stars",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${track.rating}",
                            style = MaterialTheme.typography.overline.tabular(),
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = remember(track.durationMs) { TextNormalizer.formatDuration(track.durationMs) },
                style = MaterialTheme.typography.caption.tabular(),
                color = ext.subtleText
            )
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("track_more_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Track actions",
                    tint = ext.subtleText
                )
            }
        }
        if (showDivider) {
            val baseIndent = hStartPad + leadingWidth + artGap
            Divider(
                color = ext.dividerColor,
                startIndent = if (selectionMode) baseIndent + 36.dp else baseIndent
            )
        }
    }
}

@Composable
fun InlineArtistAlbumMeta(
    artist: String,
    album: String,
    albumArtist: String,
    clickableLinks: Boolean = true,
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((String, String) -> Unit)? = null,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.body2,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    val showArtist = artist.isNotBlank()
    val showAlbum = album.isNotBlank()
    if (!showArtist && !showAlbum) return

    if (showArtist && !showAlbum) {
        Text(
            text = artist,
            style = textStyle,
            color = if (clickableLinks && onArtistClick != null) MaterialTheme.colors.primary else ext.subtleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.then(
                if (clickableLinks && onArtistClick != null) {
                    Modifier.clickable { onArtistClick(artist) }
                } else Modifier
            )
        )
        return
    }
    if (!showArtist && showAlbum) {
        Text(
            text = album,
            style = textStyle,
            color = if (clickableLinks && onAlbumClick != null) MaterialTheme.colors.primary else ext.subtleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.then(
                if (clickableLinks && onAlbumClick != null) {
                    Modifier.clickable { onAlbumClick(album, albumArtist) }
                } else Modifier
            )
        )
        return
    }

    Layout(
        modifier = modifier,
        content = {
            Text(
                text = artist,
                style = textStyle,
                color = ext.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (clickableLinks && onArtistClick != null) {
                    Modifier.clickable { onArtistClick(artist) }
                } else Modifier
            )
            Text(
                text = " • ",
                style = textStyle,
                color = ext.subtleText,
                maxLines = 1
            )
            Text(
                text = album,
                style = textStyle,
                color = ext.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (clickableLinks && onAlbumClick != null) {
                    Modifier.clickable { onAlbumClick(album, albumArtist) }
                } else Modifier
            )
        }
    ) { measurables, constraints ->
        val maxW = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0)
        val sepPlaceable = measurables[1].measure(loose)
        val avail = (maxW - sepPlaceable.width).coerceAtLeast(0)

        val artistNatural = measurables[0].maxIntrinsicWidth(constraints.maxHeight)
        val albumNatural = measurables[2].maxIntrinsicWidth(constraints.maxHeight)

        val (artistMax, albumMax) = if (artistNatural + albumNatural <= avail) {
            artistNatural to albumNatural
        } else {
            val half = avail / 2
            when {
                artistNatural <= half -> artistNatural to (avail - artistNatural)
                albumNatural <= half -> (avail - albumNatural) to albumNatural
                else -> half to (avail - half)
            }
        }

        val artistPlaceable = measurables[0].measure(Constraints(maxWidth = artistMax.coerceAtLeast(0), maxHeight = constraints.maxHeight))
        val albumPlaceable = measurables[2].measure(Constraints(maxWidth = albumMax.coerceAtLeast(0), maxHeight = constraints.maxHeight))

        val totalWidth = (artistPlaceable.width + sepPlaceable.width + albumPlaceable.width).coerceAtMost(maxW)
        val totalHeight = maxOf(artistPlaceable.height, sepPlaceable.height, albumPlaceable.height)
        layout(totalWidth, totalHeight) {
            var x = 0
            artistPlaceable.placeRelative(x, (totalHeight - artistPlaceable.height) / 2)
            x += artistPlaceable.width
            sepPlaceable.placeRelative(x, (totalHeight - sepPlaceable.height) / 2)
            x += sepPlaceable.width
            albumPlaceable.placeRelative(x, (totalHeight - albumPlaceable.height) / 2)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistRowItem(
    artist: ArtistSummary,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val vPad = ext.config.density.rowVerticalPaddingDp.dp
    val hStartPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    val artSize = if (isCompact) 42.dp else 48.dp
    val artGap = if (isCompact) CadenceSpacing.sm else CadenceSpacing.md
    val bgColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.14f) else Color.Transparent

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = hStartPad, end = CadenceSpacing.xs, top = vPad, bottom = vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = CadenceSpacing.xs)
                )
            }
            TrackArtwork(
                artworkUri = artist.artworkUri,
                contentUri = null,
                contentDescription = artist.artist,
                modifier = Modifier.size(artSize),
                isCircular = true
            )
            Spacer(Modifier.width(artGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.artist,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${artist.albumCount} albums • ${artist.trackCount} tracks",
                    style = MaterialTheme.typography.body2,
                    color = ext.subtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(CadenceSpacing.sm))
            Text(
                text = remember(artist.trackCount) { "${artist.trackCount}" },
                style = MaterialTheme.typography.caption.tabular(),
                color = ext.subtleText
            )
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Artist actions for ${artist.artist}",
                    tint = ext.subtleText
                )
            }
        }
        if (showDivider) {
            val baseIndent = hStartPad + artSize + artGap
            Divider(
                color = ext.dividerColor,
                startIndent = if (selectionMode) baseIndent + 36.dp else baseIndent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistCardGridItem(
    artist: ArtistSummary,
    isSelected: Boolean = false,
    selectionActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleCheck: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    CadenceCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(CadenceSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                TrackArtwork(
                    artworkUri = artist.artworkUri,
                    contentUri = null,
                    contentDescription = artist.artist,
                    modifier = Modifier.fillMaxSize(),
                    isCircular = true
                )
                if (selectionActive) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleCheck() },
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
            Divider(color = ext.dividerColor)
            Row(
                modifier = Modifier
                    .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.14f) else Color.Transparent)
                    .padding(start = CadenceSpacing.sm, end = 2.dp, top = CadenceSpacing.xs, bottom = CadenceSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.artist,
                        style = MaterialTheme.typography.subtitle2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.albumCount} albums • ${artist.trackCount} tracks",
                        style = MaterialTheme.typography.caption.tabular(),
                        color = ext.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!selectionActive) {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Artist actions",
                            tint = ext.subtleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionListRow(
    title: String,
    subtitle: String,
    trailingMeta: String? = null,
    artworkUri: String? = null,
    leadingIcon: ImageVector? = null,
    badgeText: String? = null,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onMoreClick: () -> Unit,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val vPad = ext.config.density.rowVerticalPaddingDp.dp
    val hStartPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg
    val bgColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.14f) else Color.Transparent

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = hStartPad, end = CadenceSpacing.xs, top = vPad, bottom = vPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = CadenceSpacing.xs)
                )
            }
            when {
                artworkUri != null -> {
                    TrackArtwork(artworkUri, null, title, Modifier.size(if (isCompact) 42.dp else 48.dp))
                    Spacer(Modifier.width(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md))
                }
                leadingIcon != null -> {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(if (isCompact) CadenceSpacing.md else CadenceSpacing.lg))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.body2,
                        color = ext.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val rightMeta = when {
                !badgeText.isNullOrBlank() && !trailingMeta.isNullOrBlank() -> "$badgeText • $trailingMeta"
                !badgeText.isNullOrBlank() -> badgeText
                else -> trailingMeta
            }
            if (!rightMeta.isNullOrBlank()) {
                Spacer(Modifier.width(CadenceSpacing.sm))
                Text(
                    text = rightMeta,
                    style = MaterialTheme.typography.caption.tabular(),
                    color = ext.subtleText
                )
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions for $title",
                    tint = ext.subtleText
                )
            }
        }
        if (showDivider) {
            val indent = when {
                artworkUri != null -> if (selectionMode) 112.dp else 76.dp
                leadingIcon != null -> if (selectionMode) 96.dp else 56.dp
                else -> 16.dp
            }
            Divider(color = ext.dividerColor, startIndent = indent)
        }
    }
}

@Composable
fun CollectionBatchActionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onBatchTagEdit: (() -> Unit)? = null,
    onManageUserTags: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    removeLabel: String = "Remove",
    onDelete: (() -> Unit)? = null
) {
    var overflowOpen by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colors.primary,
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CadenceSpacing.xs, vertical = CadenceSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.Close, contentDescription = "Exit Selection", tint = MaterialTheme.colors.onPrimary)
            }
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.subtitle1.tabular(),
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSelectAll, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = MaterialTheme.colors.onPrimary)
            }
            IconButton(onClick = onAddToQueue, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add to Queue", tint = MaterialTheme.colors.onPrimary)
            }
            if (onAddToPlaylist != null) {
                IconButton(onClick = onAddToPlaylist, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to Playlist", tint = MaterialTheme.colors.onPrimary)
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.PlaylistRemove, contentDescription = removeLabel, tint = MaterialTheme.colors.onPrimary)
                }
            }
            Box {
                IconButton(onClick = { overflowOpen = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More batch actions", tint = MaterialTheme.colors.onPrimary)
                }
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    DropdownMenuItem(onClick = { overflowOpen = false; onPlayNext() }) {
                        Text("Play Next")
                    }
                    if (onManageUserTags != null) {
                        DropdownMenuItem(onClick = { overflowOpen = false; onManageUserTags() }) {
                            Text("User Tags")
                        }
                    }
                    if (onBatchTagEdit != null) {
                        DropdownMenuItem(onClick = { overflowOpen = false; onBatchTagEdit() }) {
                            Text("Batch Edit Tags")
                        }
                    }
                    if (onShare != null) {
                        DropdownMenuItem(onClick = { overflowOpen = false; onShare() }) {
                            Text("Share Audio")
                        }
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(onClick = { overflowOpen = false; onDelete() }) {
                            Text("Delete", color = MaterialTheme.colors.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    progressFlow: StateFlow<PlaybackProgressState>,
    dividerColor: Color
) {
    val progress by progressFlow.collectAsState()
    val frac = if (progress.durationMs > 0L) {
        (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    LinearProgressIndicator(
        progress = frac,
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
        color = MaterialTheme.colors.primary,
        backgroundColor = dividerColor
    )
}

@Composable
fun SingleLineButtonLabel(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.button,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun CadenceInlineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {}
) {
    val ext = LocalCadenceTheme.current
    val isCompact = LocalIsCompactWidth.current
    val hPad = if (isCompact) CadenceSpacing.md else CadenceSpacing.lg

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = ext.elevatedSurface,
            shape = RoundedCornerShape(CadenceRadius.md),
            border = BorderStroke(1.dp, if (query.isNotEmpty()) MaterialTheme.colors.primary.copy(alpha = 0.45f) else ext.subtleBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPad, vertical = CadenceSpacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CadenceSpacing.md, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (query.isNotEmpty()) MaterialTheme.colors.primary else ext.subtleText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(CadenceSpacing.sm))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.body2,
                            color = ext.subtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.body2.copy(
                            color = MaterialTheme.colors.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inline_search_bar")
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = ext.subtleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        Divider(color = ext.dividerColor)
    }
}

@Composable
fun PersistentMiniPlayer(
    track: TrackEntity,
    isPlaying: Boolean,
    progressFlow: StateFlow<PlaybackProgressState>,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onSetRating: ((Int) -> Unit)? = null,
    onOpenQueue: (() -> Unit)? = null,
    showArtwork: Boolean = true,
    showFavorite: Boolean = true,
    showPrevious: Boolean = false,
    showPlayPause: Boolean = true,
    showNext: Boolean = true,
    showQueue: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    val style = ext.config.miniPlayerStyle
    val ratingMode = LocalRatingSystemMode.current
    var dragAccumulated by remember { mutableFloatStateOf(0f) }
    var starMenuOpen by remember { mutableStateOf(false) }

    val containerModifier = when (style) {
        MiniPlayerStyle.ELEVATED_CARD -> modifier
            .fillMaxWidth()
            .padding(horizontal = CadenceSpacing.md, vertical = CadenceSpacing.xs)
        else -> modifier.fillMaxWidth()
    }

    val shape = when (style) {
        MiniPlayerStyle.ELEVATED_CARD -> RoundedCornerShape(CadenceRadius.md)
        else -> RoundedCornerShape(0.dp)
    }

    Surface(
        modifier = containerModifier
            .pointerInput(track.id, onNext, onPrevious) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccumulated += dragAmount },
                    onDragEnd = {
                        if (dragAccumulated < -96f) {
                            onNext()
                        } else if (dragAccumulated > 96f) {
                            onPrevious?.invoke()
                        }
                        dragAccumulated = 0f
                    }
                )
            }
            .clickable(onClick = onOpenNowPlaying)
            .testTag("mini_player_bar"),
        shape = shape,
        color = ext.elevatedSurface,
        elevation = if (style == MiniPlayerStyle.ELEVATED_CARD) 6.dp else 2.dp,
        border = if (style == MiniPlayerStyle.ELEVATED_CARD) BorderStroke(1.dp, ext.subtleBorder) else BorderStroke(0.5.dp, ext.dividerColor)
    ) {
        Column {
            MiniPlayerProgressBar(
                progressFlow = progressFlow,
                dividerColor = ext.dividerColor
            )
            val isCompact = LocalIsCompactWidth.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isCompact) CadenceSpacing.sm else CadenceSpacing.md,
                        end = CadenceSpacing.xs,
                        top = if (style == MiniPlayerStyle.MINIMAL) 4.dp else 6.dp,
                        bottom = if (style == MiniPlayerStyle.MINIMAL) 4.dp else 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showArtwork && style != MiniPlayerStyle.MINIMAL) {
                    TrackArtwork(
                        artworkUri = track.artworkUri,
                        contentUri = track.contentUri,
                        contentDescription = track.title,
                        cornerRadius = 8.dp,
                        modifier = Modifier.size(if (isCompact) 38.dp else 42.dp)
                    )
                    Spacer(Modifier.width(if (isCompact) CadenceSpacing.sm else CadenceSpacing.md))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.caption,
                        color = ext.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showFavorite && (onToggleFavorite != null || onSetRating != null)) {
                    if (ratingMode == RatingSystemMode.HEARTS && onToggleFavorite != null) {
                        val favTint by animateColorAsState(
                            targetValue = if (track.isFavorite) MaterialTheme.colors.secondary else ext.subtleText,
                            animationSpec = tween(if (ext.config.reducedMotion) 0 else 180),
                            label = "mini_fav_tint"
                        )
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mini_favorite")
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (track.isFavorite) "Remove favorite" else "Favorite",
                                tint = favTint,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else if (ratingMode == RatingSystemMode.STARS && onSetRating != null) {
                        Box {
                            IconButton(
                                onClick = { starMenuOpen = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("mini_rating")
                            ) {
                                Icon(
                                    imageVector = if (track.rating > 0) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Track rating ${track.rating}",
                                    tint = if (track.rating > 0) MaterialTheme.colors.primary else ext.subtleText,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            DropdownMenu(expanded = starMenuOpen, onDismissRequest = { starMenuOpen = false }) {
                                (0..5).forEach { stars ->
                                    DropdownMenuItem(onClick = {
                                        starMenuOpen = false
                                        onSetRating(stars)
                                    }) {
                                        Text(if (stars == 0) "Clear Rating" else "$stars Stars")
                                    }
                                }
                            }
                        }
                    }
                }
                if (showPrevious && onPrevious != null) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                if (showPlayPause) {
                    val playBg by animateColorAsState(
                        targetValue = if (isPlaying) MaterialTheme.colors.primary.copy(alpha = 0.20f) else MaterialTheme.colors.primary.copy(alpha = 0.12f),
                        animationSpec = tween(if (ext.config.reducedMotion) 0 else 180),
                        label = "mini_play_bg"
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = playBg,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mini_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or Pause",
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                if (showNext) {
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                if (showQueue && onOpenQueue != null) {
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mini_queue")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Open Queue",
                            tint = ext.subtleText,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CadenceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val ext = LocalCadenceTheme.current
    val appearance = ext.config.cardAppearance
    val elevation = when (appearance) {
        CardAppearance.ELEVATED -> 2.dp
        CardAppearance.FLAT -> 0.dp
        CardAppearance.OUTLINED -> 0.dp
    }
    val border = when (appearance) {
        CardAppearance.OUTLINED -> BorderStroke(1.dp, ext.subtleBorder)
        CardAppearance.ELEVATED -> BorderStroke(0.75.dp, ext.subtleBorder)
        CardAppearance.FLAT -> BorderStroke(0.5.dp, ext.dividerColor)
    }
    val bgColor = if (appearance == CardAppearance.FLAT) ext.elevatedSurface else MaterialTheme.colors.surface
    val cardMod = if (onClick != null) modifier.clickable(onClick = onClick) else modifier

    Card(
        modifier = cardMod,
        shape = MaterialTheme.shapes.medium,
        backgroundColor = bgColor,
        elevation = elevation,
        border = border,
        content = content
    )
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.MusicNote,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(CadenceSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.elevatedSurface,
            border = BorderStroke(1.dp, ext.subtleBorder)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.padding(14.dp).size(32.dp)
            )
        }
        Spacer(Modifier.height(CadenceSpacing.md))
        Text(title, style = MaterialTheme.typography.h6, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.body2,
            color = ext.subtleText,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(CadenceSpacing.lg))
            Button(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SingleLineButtonLabel(actionLabel)
            }
        }
    }
}

@Composable
fun TrackActionsSheet(
    track: TrackEntity,
    onDismiss: () -> Unit,
    onPlayNow: (() -> Unit)? = null,
    onPlayNext: () -> Unit,
    onAddToEnd: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onSetRating: ((Int) -> Unit)? = null,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onOpenTagEditor: () -> Unit,
    onManageUserTags: () -> Unit,
    onOpenLyrics: () -> Unit,
    onShowFileInfo: () -> Unit,
    onShare: () -> Unit,
    onExcludeFolder: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    val ratingMode = LocalRatingSystemMode.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = CadenceSpacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(track.artworkUri, track.contentUri, track.title, Modifier.size(48.dp))
            Spacer(Modifier.width(CadenceSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.subtitle1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${track.artist} • ${track.album}", style = MaterialTheme.typography.caption, color = ext.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
        if (onPlayNow != null) {
            SheetActionRow(Icons.Default.PlayArrow, "Play Now") { onPlayNow(); onDismiss() }
        }
        SheetActionRow(Icons.Default.PlaylistPlay, "Play Next") { onPlayNext(); onDismiss() }
        SheetActionRow(Icons.AutoMirrored.Filled.QueueMusic, "Add to Queue") { onAddToEnd(); onDismiss() }
        SheetActionRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to Playlist") { onAddToPlaylist(); onDismiss() }
        if (onRemoveFromPlaylist != null) {
            SheetActionRow(Icons.Default.PlaylistRemove, "Remove from Playlist") { onRemoveFromPlaylist(); onDismiss() }
        }
        if (ratingMode == RatingSystemMode.HEARTS && onToggleFavorite != null) {
            SheetActionRow(
                icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (track.isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (track.isFavorite) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface
            ) {
                onToggleFavorite()
                onDismiss()
            }
        } else if (ratingMode == RatingSystemMode.STARS && onSetRating != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CadenceSpacing.lg, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(CadenceSpacing.lg))
                Text("Rate Track", style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                for (star in 1..5) {
                    IconButton(
                        onClick = {
                            val next = if (track.rating == star) 0 else star
                            onSetRating(next)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("sheet_rate_star_$star")
                    ) {
                        Icon(
                            imageVector = if (star <= track.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Rate $star stars",
                            tint = if (star <= track.rating) MaterialTheme.colors.primary else ext.subtleText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
        SheetActionRow(Icons.Default.Album, "Go to Album") { onGoToAlbum(); onDismiss() }
        SheetActionRow(Icons.Default.Person, "Go to Artist") { onGoToArtist(); onDismiss() }
        SheetActionRow(Icons.Default.Lyrics, "Lyrics & Sync Studio") { onOpenLyrics(); onDismiss() }
        SheetActionRow(Icons.Default.Edit, "Edit Tags & Artwork") { onOpenTagEditor(); onDismiss() }
        SheetActionRow(Icons.Default.Label, "User-Defined Tags") { onManageUserTags(); onDismiss() }
        Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
        SheetActionRow(Icons.Default.Info, "File & Codec Details") { onShowFileInfo(); onDismiss() }
        SheetActionRow(Icons.Default.Share, "Share Audio File") { onShare(); onDismiss() }
        if (track.folderPath.isNotBlank()) {
            SheetActionRow(Icons.Default.FolderOff, "Exclude Folder") { onExcludeFolder(); onDismiss() }
        }
        SheetActionRow(Icons.Default.Delete, "Delete File from Device", tint = MaterialTheme.colors.error) {
            onDelete()
            onDismiss()
        }
    }
}

@Composable
fun CollectionActionsSheet(
    target: CollectionSheetTarget,
    onDismiss: () -> Unit,
    onPlayTarget: (CollectionSheetTarget, Boolean) -> Unit,
    onPlayNextTarget: (CollectionSheetTarget) -> Unit,
    onAddToQueueTarget: (CollectionSheetTarget) -> Unit,
    onAddToPlaylistTarget: (CollectionSheetTarget) -> Unit,
    onBatchEditTagsTarget: (CollectionSheetTarget) -> Unit,
    onOpenTarget: (CollectionSheetTarget) -> Unit,
    onRenamePlaylist: (PlaylistWithCount) -> Unit,
    onDuplicatePlaylist: (PlaylistWithCount) -> Unit,
    onExportPlaylist: (PlaylistWithCount) -> Unit,
    onDeletePlaylist: (PlaylistWithCount) -> Unit,
    onEditSmartPlaylist: (SmartPlaylistEntity) -> Unit,
    onDuplicateSmartPlaylist: (SmartPlaylistEntity) -> Unit,
    onDeleteSmartPlaylist: (SmartPlaylistEntity) -> Unit,
    onRescanLibrary: () -> Unit,
    onExcludeFolder: (FolderSummary) -> Unit
) {
    val ext = LocalCadenceTheme.current
    val (title, subtitle, icon) = when (target) {
        is CollectionSheetTarget.Album -> Triple(target.album.album, "${target.album.albumArtist} • ${target.album.trackCount} tracks", Icons.Default.Album)
        is CollectionSheetTarget.Artist -> Triple(target.artist.artist, "${target.artist.albumCount} albums • ${target.artist.trackCount} tracks", Icons.Default.Person)
        is CollectionSheetTarget.Genre -> Triple(target.genre.genre, "${target.genre.trackCount} tracks • ${TextNormalizer.formatDuration(target.genre.totalDurationMs)}", Icons.Default.MusicNote)
        is CollectionSheetTarget.Year -> Triple(target.year.year.toString(), "${target.year.trackCount} tracks", Icons.Default.MusicNote)
        is CollectionSheetTarget.Folder -> Triple(target.folder.folderName, "${target.folder.trackCount} tracks • ${target.folder.folderPath}", Icons.Default.Folder)
        is CollectionSheetTarget.Playlist -> Triple(target.playlist.name, "${target.playlist.trackCount} tracks • ${TextNormalizer.formatDuration(target.playlist.totalDurationMs)}", Icons.Default.PlaylistPlay)
        is CollectionSheetTarget.SmartPlaylist -> Triple(target.smartPlaylist.name, target.smartPlaylist.description.ifBlank { "Dynamic smart rules" }, Icons.Default.AutoAwesome)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = CadenceSpacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (target) {
                is CollectionSheetTarget.Album -> TrackArtwork(target.album.artworkUri, null, title, Modifier.size(48.dp))
                is CollectionSheetTarget.Artist -> TrackArtwork(target.artist.artworkUri, null, title, Modifier.size(48.dp))
                else -> Icon(
                    icon,
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(CadenceSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.subtitle1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.caption, color = ext.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))

        SheetActionRow(Icons.Default.PlayArrow, "Play") { onPlayTarget(target, false); onDismiss() }
        SheetActionRow(Icons.Default.Shuffle, "Shuffle") { onPlayTarget(target, true); onDismiss() }
        SheetActionRow(Icons.Default.PlaylistPlay, "Play Next") { onPlayNextTarget(target); onDismiss() }
        SheetActionRow(Icons.AutoMirrored.Filled.QueueMusic, "Add to Queue") { onAddToQueueTarget(target); onDismiss() }
        if (target !is CollectionSheetTarget.Playlist) {
            SheetActionRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to Playlist") { onAddToPlaylistTarget(target); onDismiss() }
        }
        SheetActionRow(Icons.Default.FolderOpen, "Open") { onOpenTarget(target); onDismiss() }

        when (target) {
            is CollectionSheetTarget.Playlist -> {
                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                SheetActionRow(Icons.Default.Edit, "Rename Playlist") { onRenamePlaylist(target.playlist); onDismiss() }
                SheetActionRow(Icons.Default.ContentCopy, "Duplicate Playlist") { onDuplicatePlaylist(target.playlist); onDismiss() }
                SheetActionRow(Icons.Default.FileDownload, "Export .M3U8") { onExportPlaylist(target.playlist); onDismiss() }
                SheetActionRow(Icons.Default.Delete, "Delete Playlist", tint = MaterialTheme.colors.error) {
                    onDeletePlaylist(target.playlist); onDismiss()
                }
            }
            is CollectionSheetTarget.SmartPlaylist -> {
                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                SheetActionRow(Icons.Default.Edit, "Edit Smart Rules") { onEditSmartPlaylist(target.smartPlaylist); onDismiss() }
                SheetActionRow(Icons.Default.ContentCopy, "Duplicate Rules") { onDuplicateSmartPlaylist(target.smartPlaylist); onDismiss() }
                if (!target.smartPlaylist.isBuiltIn) {
                    SheetActionRow(Icons.Default.Delete, "Delete Smart Playlist", tint = MaterialTheme.colors.error) {
                        onDeleteSmartPlaylist(target.smartPlaylist); onDismiss()
                    }
                }
            }
            is CollectionSheetTarget.Folder -> {
                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                SheetActionRow(Icons.Default.Edit, "Batch Edit Track Tags") { onBatchEditTagsTarget(target); onDismiss() }
                SheetActionRow(Icons.Default.Refresh, "Rescan Library") { onRescanLibrary(); onDismiss() }
                SheetActionRow(Icons.Default.FolderOff, "Exclude Folder from Library", tint = MaterialTheme.colors.error) {
                    onExcludeFolder(target.folder); onDismiss()
                }
            }
            else -> {
                Divider(color = ext.dividerColor, modifier = Modifier.padding(vertical = CadenceSpacing.xs))
                SheetActionRow(Icons.Default.Edit, "Batch Edit Track Tags") { onBatchEditTagsTarget(target); onDismiss() }
            }
        }
    }
}

@Composable
fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = CadenceSpacing.lg, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(CadenceSpacing.lg))
        Text(label, style = MaterialTheme.typography.body1, color = tint)
    }
}

@Composable
fun CodecInfoDialog(track: TrackEntity, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio File Details") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("Title", track.title)
                InfoRow("Artist", track.artist)
                InfoRow("Album", track.album)
                InfoRow("Format / Codec", "${track.format} (${track.codec})")
                InfoRow("Bitrate", if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "Variable / Unknown")
                InfoRow("Sample Rate", if (track.sampleRateHz > 0) "${track.sampleRateHz} Hz" else "Unknown")
                InfoRow("Bit Depth", if (track.bitDepth > 0) "${track.bitDepth}-bit" else "Standard / Lossy")
                InfoRow("Channels", "${track.channels}")
                InfoRow("Duration", TextNormalizer.formatDuration(track.durationMs))
                InfoRow("File Size", TextNormalizer.formatFileSize(track.fileSizeBytes))
                InfoRow("ReplayGain", track.replayGainTrackDb?.let { "$it dB" } ?: "Not present")
                InfoRow("Plays / Skips", "${track.playCount} plays • ${track.skipCount} skips")
                InfoRow("User Tags", track.customTags.ifBlank { "None" })
                InfoRow("Location", track.filePath.ifBlank { track.contentUri })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    val ext = LocalCadenceTheme.current
    Column(modifier = Modifier.padding(vertical = CadenceSpacing.xs)) {
        Text(label, style = MaterialTheme.typography.overline, color = ext.subtleText)
        Text(value, style = MaterialTheme.typography.body2)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun SortAndFilterDialog(
    current: ScreenSortPreference,
    onApply: (ScreenSortPreference) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember { mutableStateOf(current.field) }
    var direction by remember { mutableStateOf(current.direction) }
    var filter by remember { mutableStateOf(current.filter) }
    var minDurText by remember { mutableStateOf(current.filter.minDurationSec?.toString().orEmpty()) }
    var maxDurText by remember { mutableStateOf(current.filter.maxDurationSec?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort & Filter Library") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Direction", style = MaterialTheme.typography.subtitle2)
                Row(horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm), modifier = Modifier.padding(vertical = 6.dp)) {
                    FilterChip(
                        selected = direction == SortDirection.ASC,
                        onClick = { direction = SortDirection.ASC }
                    ) { Text("Ascending") }
                    FilterChip(
                        selected = direction == SortDirection.DESC,
                        onClick = { direction = SortDirection.DESC }
                    ) { Text("Descending") }
                }

                Text("Sort By", style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SortField.entries.forEach { sf ->
                        FilterChip(
                            selected = field == sf,
                            onClick = { field = sf }
                        ) { Text(sf.label) }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))
                Text("Quick Filters", style = MaterialTheme.typography.subtitle2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    FilterChip(
                        selected = filter.hasLyrics == true,
                        onClick = {
                            filter = filter.copy(hasLyrics = if (filter.hasLyrics == true) null else true)
                        }
                    ) { Text("Has Lyrics") }
                    FilterChip(
                        selected = filter.hasArtwork == true,
                        onClick = {
                            filter = filter.copy(hasArtwork = if (filter.hasArtwork == true) null else true)
                        }
                    ) { Text("Has Art") }
                }

                Text("Formats", style = MaterialTheme.typography.caption, modifier = Modifier.padding(top = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MP3", "FLAC", "M4A", "OGG", "OPUS", "WAV", "ALAC").forEach { fmt ->
                        val selected = fmt in filter.formats
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) filter.formats - fmt else filter.formats + fmt
                                filter = filter.copy(formats = next)
                            }
                        ) { Text(fmt) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = minDurText,
                        onValueChange = { minDurText = it.filter { c -> c.isDigit() } },
                        label = { Text("Min sec") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxDurText,
                        onValueChange = { maxDurText = it.filter { c -> c.isDigit() } },
                        label = { Text("Max sec") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedFilter = filter.copy(
                    minDurationSec = minDurText.toIntOrNull(),
                    maxDurationSec = maxDurText.toIntOrNull()
                )
                onApply(ScreenSortPreference(field, direction, updatedFilter))
                onDismiss()
            }) { Text("APPLY") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onApply(ScreenSortPreference())
                    onDismiss()
                }) { Text("RESET") }
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistWithCount>,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNew: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New playlist name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreateNew(newName)
                                onDismiss()
                            }
                        },
                        enabled = newName.isNotBlank()
                    ) { Text("Create") }
                }
                Spacer(Modifier.height(10.dp))
                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectPlaylist(pl.id)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colors.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, style = MaterialTheme.typography.subtitle1)
                            Text("${pl.trackCount} tracks", style = MaterialTheme.typography.caption)
                        }
                    }
                    Divider(color = LocalCadenceTheme.current.dividerColor)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun SleepTimerDialog(
    state: SleepTimerState,
    onStartMinutes: (Int, Boolean) -> Unit,
    onStopAfterCurrent: (Boolean) -> Unit,
    onStartAfterTracks: (Int, Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var minutes by remember { mutableFloatStateOf(20f) }
    var trackCount by remember { mutableIntStateOf(3) }
    var fadeOut by remember { mutableStateOf(state.fadeOutEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colors.primary)
                Spacer(Modifier.width(8.dp))
                Text("Sleep Timer")
            }
        },
        text = {
            Column {
                if (state.mode != SleepTimerMode.OFF) {
                    Surface(
                        color = MaterialTheme.colors.primary.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val status = when (state.mode) {
                                SleepTimerMode.END_OF_TRACK -> "Stopping after current track"
                                SleepTimerMode.AFTER_TRACK_COUNT -> "Stopping after ${state.remainingTracks} track(s)"
                                else -> "Remaining: ${TextNormalizer.formatDuration(state.remainingMs)}"
                            }
                            Text(status, style = MaterialTheme.typography.subtitle2.tabular(), color = MaterialTheme.colors.primary)
                            TextButton(onClick = { onCancelTimer(); onDismiss() }) {
                                Text("CANCEL ACTIVE TIMER")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text("Stop after ${minutes.toInt()} minutes", style = MaterialTheme.typography.subtitle2.tabular())
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = 5f..120f
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = fadeOut, onCheckedChange = { fadeOut = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Gradually fade out volume", style = MaterialTheme.typography.body2)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onStartMinutes(minutes.toInt(), fadeOut); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) { SingleLineButtonLabel("START ${minutes.toInt()} MIN TIMER") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { onStopAfterCurrent(fadeOut); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) { SingleLineButtonLabel("STOP AFTER CURRENT TRACK") }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onStartAfterTracks(trackCount, fadeOut); onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) { SingleLineButtonLabel("AFTER $trackCount TRACKS") }
                    IconButton(onClick = { if (trackCount > 1) trackCount-- }) { Text("-") }
                    IconButton(onClick = { if (trackCount < 25) trackCount++ }) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { SingleLineButtonLabel("CLOSE") }
        }
    )
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    currentPitch: Float,
    onApply: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed) }
    var pitch by remember { mutableFloatStateOf(currentPitch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speed & Pitch") },
        text = {
            Column {
                Text("Speed: ${String.format("%.2fx", speed)}", fontFamily = JetBrainsMonoFamily)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2.0f)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { preset ->
                        OutlinedButton(
                            onClick = { speed = preset },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            SingleLineButtonLabel("${preset}x", style = MaterialTheme.typography.caption)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Pitch: ${String.format("%.2fx", pitch)}", fontFamily = JetBrainsMonoFamily)
                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.6f..1.5f)
            }
        },
        confirmButton = {
            Button(onClick = { onApply(speed, pitch); onDismiss() }) { SingleLineButtonLabel("APPLY") }
        },
        dismissButton = {
            TextButton(onClick = { onApply(1.0f, 1.0f); onDismiss() }) { SingleLineButtonLabel("RESET 1.0x") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun UserTagsDialog(
    initialTagsCsv: String,
    trackCount: Int,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagsText by remember { mutableStateOf(initialTagsCsv) }
    val activeSet = remember(tagsText) {
        tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val suggestedTags = listOf("Workout", "Chill", "Focus", "Drive", "Late Night", "Study", "Acoustic", "Live", "Party")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (trackCount == 1) "User-Defined Track Tags" else "User Tags ($trackCount Tracks)") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Tag tracks with custom labels to use in dynamic Smart Playlist rules.",
                    style = MaterialTheme.typography.caption
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Comma-separated tags") },
                    placeholder = { Text("e.g. Workout, High Energy") },
                    modifier = Modifier.fillMaxWidth().testTag("user_tags_input")
                )
                Spacer(Modifier.height(10.dp))
                Text("Quick Toggle Tags", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    suggestedTags.forEach { tag ->
                        val selected = activeSet.any { it.equals(tag, ignoreCase = true) }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) {
                                    activeSet.filterNot { it.equals(tag, ignoreCase = true) }
                                } else {
                                    activeSet + tag
                                }
                                tagsText = next.joinToString(", ")
                            }
                        ) { Text(tag) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(tagsText)
                onDismiss()
            }) { Text("SAVE TAGS") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun FastScrollLazyListOverlay(
    listState: LazyListState,
    itemCount: Int,
    labelForIndex: (Int) -> String,
    modifier: Modifier = Modifier
) {
    if (itemCount < 20) return
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val listFraction by remember(listState, itemCount) {
        derivedStateOf {
            if (itemCount <= 1) 0f
            else (listState.firstVisibleItemIndex.toFloat() / (itemCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
    }
    val activeFraction = if (isDragging) dragFraction else listFraction
    val showThumb = isDragging || listState.isScrollInProgress
    val currentIndex = ((activeFraction * (itemCount - 1)).roundToInt()).coerceIn(0, itemCount - 1)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val thumbHeightPx = with(density) { 44.dp.toPx() }
        val bubbleOffsetX = -with(density) { 38.dp.roundToPx() }
        val thumbOffsetX = -with(density) { 4.dp.roundToPx() }
        val usablePx = (maxHeightPx - thumbHeightPx).coerceAtLeast(1f)
        val thumbY = (activeFraction * usablePx).roundToInt()

        if (isDragging) {
            val bubbleLabel = labelForIndex(currentIndex)
            if (bubbleLabel.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colors.primary,
                    elevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(bubbleOffsetX, thumbY) }
                ) {
                    Text(
                        text = bubbleLabel,
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Right-edge fast-scroll touch rail
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(24.dp)
                .pointerInput(itemCount) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val f = (offset.y / maxHeightPx).coerceIn(0f, 1f)
                            dragFraction = f
                            val idx = (f * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
                            scope.launch { listState.scrollToItem(idx) }
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val f = (change.position.y / maxHeightPx).coerceIn(0f, 1f)
                            dragFraction = f
                            val idx = (f * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
                            scope.launch { listState.scrollToItem(idx) }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    )
                }
        ) {
            if (showThumb) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (isDragging) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(thumbOffsetX, thumbY) }
                        .width(6.dp)
                        .height(44.dp)
                ) {}
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.CadenceBottomTabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    canHide: Boolean,
    onClick: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onHideTab: () -> Unit,
    onOpenTabEditor: () -> Unit,
    onResetTabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalCadenceTheme.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colors.primary else ext.subtleText,
        animationSpec = tween(if (ext.config.reducedMotion) 0 else 160),
        label = "bottom_tab_color"
    )

    Box(
        modifier = modifier
            .weight(1f)
            .height(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClickLabel = "Edit navigation tabs",
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuExpanded = true
                },
                role = Role.Tab
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(2.dp))
            SingleLineButtonLabel(
                text = label,
                style = MaterialTheme.typography.caption,
                color = contentColor
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                enabled = false,
                onClick = {}
            ) {
                Text(
                    text = "Tab: $label",
                    style = MaterialTheme.typography.overline,
                    color = MaterialTheme.colors.primary
                )
            }
            Divider(color = ext.dividerColor)
            DropdownMenuItem(
                enabled = canMoveLeft,
                onClick = {
                    menuExpanded = false
                    onMoveLeft()
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Move Tab Left")
            }
            DropdownMenuItem(
                enabled = canMoveRight,
                onClick = {
                    menuExpanded = false
                    onMoveRight()
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Move Tab Right")
            }
            DropdownMenuItem(
                enabled = canHide,
                onClick = {
                    menuExpanded = false
                    onHideTab()
                }
            ) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Hide \"$label\"")
            }
            Divider(color = ext.dividerColor)
            DropdownMenuItem(
                onClick = {
                    menuExpanded = false
                    onOpenTabEditor()
                }
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Customize All Tabs…", color = MaterialTheme.colors.primary, fontWeight = FontWeight.SemiBold)
            }
            DropdownMenuItem(
                onClick = {
                    menuExpanded = false
                    onResetTabs()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Reset Default Tabs")
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TabBarEditorDialog(
    navOrder: List<NavDestinationKey>,
    hiddenNavItems: Set<NavDestinationKey>,
    iconForKey: (NavDestinationKey) -> ImageVector,
    onMoveUp: (NavDestinationKey) -> Unit,
    onMoveDown: (NavDestinationKey) -> Unit,
    onToggleVisibility: (NavDestinationKey, Boolean) -> Unit,
    onApplyPreset: (List<NavDestinationKey>, Set<NavDestinationKey>) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalCadenceTheme.current
    val configurable = remember(navOrder) {
        navOrder.filter { it != NavDestinationKey.SETTINGS }
    }
    val visibleTabs = remember(configurable, hiddenNavItems) {
        configurable.filter { it !in hiddenNavItems }
    }
    val phonePreviewTabs = remember(visibleTabs) {
        visibleTabs.take(5).ifEmpty { listOf(NavDestinationKey.HOME, NavDestinationKey.PLAYLISTS) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.xl)
                .testTag("tab_bar_editor_dialog"),
            shape = RoundedCornerShape(CadenceRadius.md),
            color = MaterialTheme.colors.surface,
            border = BorderStroke(1.dp, ext.subtleBorder),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = CadenceSpacing.lg, end = CadenceSpacing.xs, top = CadenceSpacing.md, bottom = CadenceSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(CadenceSpacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Customize Bottom Tab Bar", style = MaterialTheme.typography.h6)
                        Text(
                            "Long-press any bottom tab anytime to reorder, hide, or swap tabs.",
                            style = MaterialTheme.typography.caption,
                            color = ext.subtleText
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Divider(color = ext.dividerColor)

                // Live Preview of Phone Bottom Bar (max 5 slots)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ext.elevatedSurface)
                        .padding(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.sm)
                ) {
                    Text(
                        text = "LIVE PHONE BAR PREVIEW (${phonePreviewTabs.size}/5 SLOTS)",
                        style = MaterialTheme.typography.overline.tabular(),
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(CadenceRadius.sm),
                        color = MaterialTheme.colors.surface,
                        border = BorderStroke(1.dp, ext.subtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            phonePreviewTabs.forEachIndexed { idx, key ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = iconForKey(key),
                                        contentDescription = key.label,
                                        tint = if (idx == 0) MaterialTheme.colors.primary else ext.subtleText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    SingleLineButtonLabel(
                                        text = key.label,
                                        style = MaterialTheme.typography.caption,
                                        color = if (idx == 0) MaterialTheme.colors.primary else ext.subtleText
                                    )
                                }
                            }
                        }
                    }
                }

                // One-tap Tab Bar Presets
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(CadenceSpacing.sm)
                ) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { onResetDefaults() }
                        ) { SingleLineButtonLabel("Classic 5", style = MaterialTheme.typography.caption) }
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val order = listOf(
                                    NavDestinationKey.HOME,
                                    NavDestinationKey.SONGS,
                                    NavDestinationKey.PLAYLISTS,
                                    NavDestinationKey.SEARCH,
                                    NavDestinationKey.ARTISTS,
                                    NavDestinationKey.ALBUMS,
                                    NavDestinationKey.FILES,
                                    NavDestinationKey.GENRES,
                                    NavDestinationKey.YEARS,
                                    NavDestinationKey.COMPOSERS,
                                    NavDestinationKey.SMART_VIEWS,
                                    NavDestinationKey.LIBRARY,
                                    NavDestinationKey.SETTINGS
                                )
                                val hidden = NavDestinationKey.entries.toSet() - setOf(
                                    NavDestinationKey.HOME,
                                    NavDestinationKey.SONGS,
                                    NavDestinationKey.PLAYLISTS,
                                    NavDestinationKey.SEARCH
                                )
                                onApplyPreset(order, hidden)
                            }
                        ) { SingleLineButtonLabel("Focused 4", style = MaterialTheme.typography.caption) }
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                val order = listOf(
                                    NavDestinationKey.HOME,
                                    NavDestinationKey.ALBUMS,
                                    NavDestinationKey.ARTISTS,
                                    NavDestinationKey.FILES,
                                    NavDestinationKey.GENRES,
                                    NavDestinationKey.PLAYLISTS,
                                    NavDestinationKey.SONGS,
                                    NavDestinationKey.YEARS,
                                    NavDestinationKey.COMPOSERS,
                                    NavDestinationKey.SMART_VIEWS,
                                    NavDestinationKey.LIBRARY,
                                    NavDestinationKey.SEARCH,
                                    NavDestinationKey.SETTINGS
                                )
                                val hidden = NavDestinationKey.entries.toSet() - setOf(
                                    NavDestinationKey.HOME,
                                    NavDestinationKey.ALBUMS,
                                    NavDestinationKey.ARTISTS,
                                    NavDestinationKey.FILES,
                                    NavDestinationKey.GENRES
                                )
                                onApplyPreset(order, hidden)
                            }
                        ) { SingleLineButtonLabel("Collector", style = MaterialTheme.typography.caption) }
                    }
                }
                Divider(color = ext.dividerColor)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(configurable, key = { it.name }) { key ->
                        val idx = configurable.indexOf(key)
                        val isVisible = key !in hiddenNavItems
                        val barSlot = if (isVisible) visibleTabs.indexOf(key) + 1 else -1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = CadenceSpacing.lg, end = CadenceSpacing.sm, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconForKey(key),
                                contentDescription = null,
                                tint = if (isVisible) MaterialTheme.colors.primary else ext.subtleText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(CadenceSpacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = key.label,
                                    style = MaterialTheme.typography.subtitle2,
                                    color = if (isVisible) MaterialTheme.colors.onSurface else ext.subtleText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        !isVisible -> "Hidden from bar"
                                        barSlot in 1..5 -> "Bottom Bar Slot #$barSlot"
                                        else -> "Shown on tablet rail only (bar capped at 5)"
                                    },
                                    style = MaterialTheme.typography.caption.tabular(),
                                    color = if (barSlot in 1..5) MaterialTheme.colors.primary else ext.subtleText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onMoveUp(key) },
                                enabled = idx > 0,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move ${key.label} up", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onMoveDown(key) },
                                enabled = idx < configurable.lastIndex,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move ${key.label} down", modifier = Modifier.size(18.dp))
                            }
                            Switch(
                                checked = isVisible,
                                enabled = !isVisible || visibleTabs.size > 2,
                                onCheckedChange = { show -> onToggleVisibility(key, show) }
                            )
                        }
                        Divider(color = ext.dividerColor, startIndent = 48.dp)
                    }
                }

                Divider(color = ext.dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CadenceSpacing.lg, vertical = CadenceSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onResetDefaults,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        SingleLineButtonLabel("RESET DEFAULT")
                    }
                    Button(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        SingleLineButtonLabel("DONE")
                    }
                }
            }
        }
    }
}


