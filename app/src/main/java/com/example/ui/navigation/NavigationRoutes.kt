package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface CadenceRoute {
    @Serializable data object Home : CadenceRoute
    @Serializable data object Artists : CadenceRoute
    @Serializable data object Albums : CadenceRoute
    @Serializable data object Files : CadenceRoute
    @Serializable data object Songs : CadenceRoute
    @Serializable data object Genres : CadenceRoute
    @Serializable data object Years : CadenceRoute
    @Serializable data object Composers : CadenceRoute
    @Serializable data object SmartViews : CadenceRoute
    @Serializable data class Library(val initialTab: String = "SONGS") : CadenceRoute
    @Serializable data object Search : CadenceRoute
    @Serializable data object Playlists : CadenceRoute
    @Serializable data object Settings : CadenceRoute
    @Serializable data object NowPlaying : CadenceRoute
    @Serializable data object Queue : CadenceRoute
    @Serializable data class Lyrics(val trackId: Long, val startInEditor: Boolean = false) : CadenceRoute
    @Serializable data class TagEditor(val trackIdsCsv: String) : CadenceRoute
    @Serializable data object Equalizer : CadenceRoute
    @Serializable data object ThemeGenerator : CadenceRoute
    @Serializable data class AlbumDetail(val albumName: String, val albumArtist: String) : CadenceRoute
    @Serializable data class ArtistDetail(val artistName: String, val isAlbumArtist: Boolean = false) : CadenceRoute
    @Serializable data class PlaylistDetail(val playlistId: Long) : CadenceRoute
    @Serializable data class SmartPlaylistDetail(val smartPlaylistId: Long) : CadenceRoute
    @Serializable data class GeneratedMixDetail(val mixId: String) : CadenceRoute
    @Serializable data class FolderDetail(val folderPath: String, val folderName: String) : CadenceRoute
    @Serializable data class GenreDetail(val genreName: String) : CadenceRoute
    @Serializable data class YearDetail(val year: Int) : CadenceRoute
}
