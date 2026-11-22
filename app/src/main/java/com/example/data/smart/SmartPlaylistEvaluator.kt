package com.example.data.smart

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.core.model.LibraryFilter
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
import com.example.data.db.SmartPlaylistEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object SmartPlaylistEvaluator {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun parseDefinition(definitionJson: String): SmartPlaylistDefinition {
        return runCatching { json.decodeFromString<SmartPlaylistDefinition>(definitionJson) }
            .getOrDefault(SmartPlaylistDefinition())
    }

    fun encodeDefinition(def: SmartPlaylistDefinition): String = json.encodeToString(def)

    fun buildSmartPlaylistQuery(definition: SmartPlaylistDefinition, nowMs: Long = System.currentTimeMillis()): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val whereClause = compileGroup(definition.rootGroup, args, nowMs)
        val fullWhere = if (whereClause.isBlank()) {
            "isAvailable = 1"
        } else {
            "isAvailable = 1 AND ($whereClause)"
        }
        val orderBy = buildOrderByClause(definition.sorts)
        val limit = definition.maxResults.coerceIn(1, 10_000)
        val sql = "SELECT * FROM tracks WHERE $fullWhere ORDER BY $orderBy LIMIT $limit"
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    fun buildLibraryQuery(
        sortField: SortField,
        sortDirection: SortDirection,
        filter: LibraryFilter,
        extraWhere: String = "",
        extraArgs: List<Any> = emptyList()
    ): SupportSQLiteQuery {
        val clauses = mutableListOf("isAvailable = 1")
        val args = mutableListOf<Any>()

        if (extraWhere.isNotBlank()) {
            clauses.add("($extraWhere)")
            args.addAll(extraArgs)
        }
        filter.minDurationSec?.let {
            clauses.add("durationMs >= ?")
            args.add(it * 1000L)
        }
        filter.maxDurationSec?.let {
            clauses.add("durationMs <= ?")
            args.add(it * 1000L)
        }
        filter.artist?.takeIf { it.isNotBlank() }?.let {
            clauses.add("artist = ?")
            args.add(it)
        }
        filter.album?.takeIf { it.isNotBlank() }?.let {
            clauses.add("album = ?")
            args.add(it)
        }
        filter.genre?.takeIf { it.isNotBlank() }?.let {
            clauses.add("genre = ?")
            args.add(it)
        }
        filter.year?.let {
            clauses.add("year = ?")
            args.add(it)
        }
        filter.minRating?.let {
            clauses.add("rating >= ?")
            args.add(it)
        }
        filter.folderPath?.takeIf { it.isNotBlank() }?.let {
            clauses.add("folderPath = ?")
            args.add(it)
        }
        filter.hasLyrics?.let { has ->
            clauses.add(if (has) "hasLyrics = 1" else "hasLyrics = 0")
        }
        filter.hasArtwork?.let { has ->
            clauses.add(if (has) "(hasEmbeddedArtwork = 1 OR artworkUri IS NOT NULL)" else "(hasEmbeddedArtwork = 0 AND artworkUri IS NULL)")
        }
        if (filter.formats.isNotEmpty()) {
            val placeholders = filter.formats.joinToString(",") { "?" }
            clauses.add("UPPER(format) IN ($placeholders)")
            args.addAll(filter.formats.map { it.uppercase() })
        }

        val orderBy = buildOrderByClause(listOf(SmartSortSpec(sortField, sortDirection)))
        val sql = "SELECT * FROM tracks WHERE ${clauses.joinToString(" AND ")} ORDER BY $orderBy"
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }

    private fun compileGroup(group: SmartRuleGroup, args: MutableList<Any>, nowMs: Long): String {
        val parts = mutableListOf<String>()
        for (cond in group.conditions) {
            val sql = compileCondition(cond, args, nowMs)
            if (sql.isNotBlank()) {
                parts.add(if (cond.exclude) "NOT ($sql)" else "($sql)")
            }
        }
        for (child in group.childGroups) {
            val childSql = compileGroup(child, args, nowMs)
            if (childSql.isNotBlank()) {
                parts.add("($childSql)")
            }
        }
        if (parts.isEmpty()) return ""
        val joiner = if (group.combinator == SmartCombinator.ALL) " AND " else " OR "
        return parts.joinToString(joiner)
    }

    private fun compileCondition(cond: SmartCondition, args: MutableList<Any>, nowMs: Long): String {
        val col = fieldToSqlExpr(cond.field, nowMs)
        return when (cond.field.kind) {
            SmartFieldKind.PRESENCE_BOOL -> when (cond.operator) {
                SmartOperator.IS_TRUE, SmartOperator.IS_NOT_EMPTY -> "$col = 1"
                SmartOperator.IS_FALSE, SmartOperator.IS_EMPTY -> "$col = 0"
                else -> "$col = 1"
            }
            SmartFieldKind.BOOL -> when (cond.operator) {
                SmartOperator.IS_TRUE -> "$col = 1"
                SmartOperator.IS_FALSE -> "$col = 0"
                else -> "$col = 1"
            }
            SmartFieldKind.TEXT -> when (cond.operator) {
                SmartOperator.CONTAINS -> {
                    args.add(cond.value)
                    "$col LIKE '%' || ? || '%' COLLATE NOCASE"
                }
                SmartOperator.NOT_CONTAINS -> {
                    args.add(cond.value)
                    "$col NOT LIKE '%' || ? || '%' COLLATE NOCASE"
                }
                SmartOperator.EQUALS -> {
                    args.add(cond.value)
                    "$col = ? COLLATE NOCASE"
                }
                SmartOperator.NOT_EQUALS -> {
                    args.add(cond.value)
                    "$col != ? COLLATE NOCASE"
                }
                SmartOperator.STARTS_WITH -> {
                    args.add(cond.value)
                    "$col LIKE ? || '%' COLLATE NOCASE"
                }
                SmartOperator.IS_EMPTY -> "($col IS NULL OR TRIM($col) = '')"
                SmartOperator.IS_NOT_EMPTY -> "($col IS NOT NULL AND TRIM($col) != '')"
                else -> {
                    args.add(cond.value)
                    "$col LIKE '%' || ? || '%' COLLATE NOCASE"
                }
            }
            SmartFieldKind.NUMBER -> {
                val n1 = cond.value.toDoubleOrNull() ?: 0.0
                val n2 = cond.secondValue.toDoubleOrNull() ?: n1
                when (cond.operator) {
                    SmartOperator.EQUALS -> {
                        args.add(n1)
                        "$col = ?"
                    }
                    SmartOperator.NOT_EQUALS -> {
                        args.add(n1)
                        "$col != ?"
                    }
                    SmartOperator.GREATER_THAN -> {
                        args.add(n1)
                        "$col > ?"
                    }
                    SmartOperator.LESS_THAN -> {
                        args.add(n1)
                        if (cond.field == SmartField.LAST_PLAYED_DAYS_AGO) {
                            "(lastPlayedAt > 0 AND $col < ?)"
                        } else {
                            "$col < ?"
                        }
                    }
                    SmartOperator.BETWEEN -> {
                        val low = minOf(n1, n2)
                        val high = maxOf(n1, n2)
                        args.add(low)
                        args.add(high)
                        "$col BETWEEN ? AND ?"
                    }
                    SmartOperator.IS_EMPTY -> "($col = 0)"
                    SmartOperator.IS_NOT_EMPTY -> "($col > 0)"
                    else -> {
                        args.add(n1)
                        "$col >= ?"
                    }
                }
            }
        }
    }

    private fun fieldToSqlExpr(field: SmartField, nowMs: Long): String {
        val nowSec = nowMs / 1000L
        return when (field) {
            SmartField.TITLE -> "title"
            SmartField.ARTIST -> "artist"
            SmartField.ALBUM -> "album"
            SmartField.ALBUM_ARTIST -> "albumArtist"
            SmartField.GENRE -> "genre"
            SmartField.COMPOSER -> "composer"
            SmartField.FOLDER_PATH -> "folderPath"
            SmartField.FORMAT -> "format"
            SmartField.LYRICS -> "hasLyrics"
            SmartField.ARTWORK -> "(CASE WHEN hasEmbeddedArtwork = 1 OR artworkUri IS NOT NULL THEN 1 ELSE 0 END)"
            SmartField.FAVORITE -> "isFavorite"
            SmartField.PLAY_COUNT -> "playCount"
            SmartField.SKIP_COUNT -> "skipCount"
            SmartField.COMPLETION_PCT -> "(CASE WHEN durationMs <= 0 OR (playCount + skipCount) = 0 THEN 0.0 ELSE MIN(100.0, (totalListenMs * 100.0) / (durationMs * (playCount + skipCount))) END)"
            SmartField.RATING -> "rating"
            SmartField.DURATION_SEC -> "(durationMs / 1000.0)"
            SmartField.YEAR -> "year"
            SmartField.BPM -> "bpm"
            SmartField.BITRATE -> "bitrateKbps"
            SmartField.USER_TAGS -> "customTags"
            SmartField.DATE_ADDED_DAYS_AGO -> "(($nowSec - dateAddedSec) / 86400.0)"
            SmartField.LAST_PLAYED_DAYS_AGO -> "(CASE WHEN lastPlayedAt = 0 THEN 999999.0 ELSE (($nowMs - lastPlayedAt) / 86400000.0) END)"
        }
    }

    private fun buildOrderByClause(sorts: List<SmartSortSpec>): String {
        if (sorts.isEmpty()) return "title COLLATE NOCASE ASC"
        return sorts.joinToString(", ") { spec ->
            val dir = if (spec.direction == SortDirection.ASC) "ASC" else "DESC"
            val col = when (spec.field) {
                SortField.TITLE -> "title COLLATE NOCASE"
                SortField.ARTIST -> "artist COLLATE NOCASE"
                SortField.ALBUM -> "album COLLATE NOCASE"
                SortField.ALBUM_ARTIST -> "COALESCE(NULLIF(albumArtist, ''), artist) COLLATE NOCASE"
                SortField.DURATION -> "durationMs"
                SortField.FILE_SIZE -> "fileSizeBytes"
                SortField.DATE_ADDED -> "dateAddedSec"
                SortField.DATE_MODIFIED -> "dateModifiedSec"
                SortField.TRACK_NUMBER -> "discNumber ASC, trackNumber"
                SortField.DISC_NUMBER -> "discNumber"
                SortField.YEAR -> "year"
                SortField.GENRE -> "genre COLLATE NOCASE"
                SortField.PLAY_COUNT -> "playCount"
                SortField.LAST_PLAYED -> "lastPlayedAt"
                SortField.RATING -> "rating"
                SortField.BPM -> "bpm"
            }
            "$col $dir"
        }
    }

    fun defaultBuiltInSmartPlaylists(): List<SmartPlaylistEntity> {
        val presets = listOf(
            Triple(
                "Heavy Rotation (20+ Plays)",
                "Tracks played more than 20 times",
                SmartPlaylistDefinition(
                    rootGroup = SmartRuleGroup(
                        id = "g1",
                        conditions = listOf(
                            SmartCondition(UUID.randomUUID().toString(), SmartField.PLAY_COUNT, SmartOperator.GREATER_THAN, "20")
                        )
                    ),
                    sorts = listOf(SmartSortSpec(SortField.PLAY_COUNT, SortDirection.DESC)),
                    maxResults = 100
                )
            ),
            Triple(
                "Unheard Gems (Never Played)",
                "Tracks in your library you haven't played yet",
                SmartPlaylistDefinition(
                    rootGroup = SmartRuleGroup(
                        id = "g2",
                        conditions = listOf(
                            SmartCondition(UUID.randomUUID().toString(), SmartField.PLAY_COUNT, SmartOperator.EQUALS, "0")
                        )
                    ),
                    sorts = listOf(SmartSortSpec(SortField.DATE_ADDED, SortDirection.DESC)),
                    maxResults = 150
                )
            ),
            Triple(
                "Added Last 30 Days",
                "Fresh arrivals from the past month",
                SmartPlaylistDefinition(
                    rootGroup = SmartRuleGroup(
                        id = "g3",
                        conditions = listOf(
                            SmartCondition(UUID.randomUUID().toString(), SmartField.DATE_ADDED_DAYS_AGO, SmartOperator.LESS_THAN, "30")
                        )
                    ),
                    sorts = listOf(SmartSortSpec(SortField.DATE_ADDED, SortDirection.DESC)),
                    maxResults = 200
                )
            ),
            Triple(
                "Lossless FLAC Collection",
                "High-resolution FLAC and ALAC tracks",
                SmartPlaylistDefinition(
                    rootGroup = SmartRuleGroup(
                        id = "g4",
                        combinator = SmartCombinator.ANY,
                        conditions = listOf(
                            SmartCondition(UUID.randomUUID().toString(), SmartField.FORMAT, SmartOperator.EQUALS, "FLAC"),
                            SmartCondition(UUID.randomUUID().toString(), SmartField.FORMAT, SmartOperator.EQUALS, "ALAC")
                        )
                    ),
                    sorts = listOf(SmartSortSpec(SortField.ARTIST, SortDirection.ASC), SmartSortSpec(SortField.ALBUM, SortDirection.ASC)),
                    maxResults = 500
                )
            ),
            Triple(
                "Top Rated (4+ Stars)",
                "Your highest-rated favorites",
                SmartPlaylistDefinition(
                    rootGroup = SmartRuleGroup(
                        id = "g5",
                        conditions = listOf(
                            SmartCondition(UUID.randomUUID().toString(), SmartField.RATING, SmartOperator.GREATER_THAN, "3")
                        )
                    ),
                    sorts = listOf(SmartSortSpec(SortField.RATING, SortDirection.DESC), SmartSortSpec(SortField.PLAY_COUNT, SortDirection.DESC)),
                    maxResults = 150
                )
            ),
            Triple(
                "Needs Lyrics or Artwork",
                "Tracks missing embedded/cached cover art or lyrics",
                SmartPlaylistDefinition(
                    rootGroup = SmartRuleGroup(
                        id = "g6",
                        combinator = SmartCombinator.ANY,
                        conditions = listOf(
                            SmartCondition(UUID.randomUUID().toString(), SmartField.LYRICS, SmartOperator.IS_FALSE),
                            SmartCondition(UUID.randomUUID().toString(), SmartField.ARTWORK, SmartOperator.IS_FALSE)
                        )
                    ),
                    sorts = listOf(SmartSortSpec(SortField.TITLE, SortDirection.ASC)),
                    maxResults = 300
                )
            ),
            Triple(
                "High Completion Favorites (85%+)",
                "Tracks listened to >= 85% completion AND loved or 4+ stars",
                SmartPlaylistDefinition(
                    rootGroup = SmartRuleGroup(
                        id = "g7",
                        combinator = SmartCombinator.ALL,
                        conditions = listOf(
                            SmartCondition(UUID.randomUUID().toString(), SmartField.COMPLETION_PCT, SmartOperator.GREATER_THAN, "85")
                        ),
                        childGroups = listOf(
                            SmartRuleGroup(
                                id = "g7_nested",
                                combinator = SmartCombinator.ANY,
                                conditions = listOf(
                                    SmartCondition(UUID.randomUUID().toString(), SmartField.FAVORITE, SmartOperator.IS_TRUE),
                                    SmartCondition(UUID.randomUUID().toString(), SmartField.RATING, SmartOperator.GREATER_THAN, "3")
                                )
                            )
                        )
                    ),
                    sorts = listOf(SmartSortSpec(SortField.PLAY_COUNT, SortDirection.DESC)),
                    maxResults = 150
                )
            )
        )
        return presets.map { (name, desc, def) ->
            SmartPlaylistEntity(
                name = name,
                description = desc,
                definitionJson = encodeDefinition(def),
                isBuiltIn = true
            )
        }
    }
}
