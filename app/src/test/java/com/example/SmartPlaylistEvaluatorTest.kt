package com.example

import com.example.core.model.SmartCombinator
import com.example.core.model.SmartCondition
import com.example.core.model.SmartField
import com.example.core.model.SmartOperator
import com.example.core.model.SmartPlaylistDefinition
import com.example.core.model.SmartRuleGroup
import com.example.core.model.SmartSortSpec
import com.example.core.model.SortDirection
import com.example.core.model.SortField
import com.example.data.smart.SmartPlaylistEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartPlaylistEvaluatorTest {

    @Test
    fun buildSmartPlaylistQuery_compilesNestedGroupsCompletionStatsAndUserTags() {
        val definition = SmartPlaylistDefinition(
            rootGroup = SmartRuleGroup(
                id = "root",
                combinator = SmartCombinator.ALL,
                conditions = listOf(
                    SmartCondition("c1", SmartField.COMPLETION_PCT, SmartOperator.GREATER_THAN, "85"),
                    SmartCondition("c2", SmartField.USER_TAGS, SmartOperator.CONTAINS, "workout"),
                    SmartCondition("c3", SmartField.SKIP_COUNT, SmartOperator.GREATER_THAN, "5", exclude = true)
                ),
                childGroups = listOf(
                    SmartRuleGroup(
                        id = "nested_or",
                        combinator = SmartCombinator.ANY,
                        conditions = listOf(
                            SmartCondition("c4", SmartField.GENRE, SmartOperator.EQUALS, "Electronic"),
                            SmartCondition("c5", SmartField.YEAR, SmartOperator.BETWEEN, "2020", "2025")
                        )
                    )
                )
            ),
            sorts = listOf(SmartSortSpec(SortField.PLAY_COUNT, SortDirection.DESC)),
            maxResults = 75
        )

        val query = SmartPlaylistEvaluator.buildSmartPlaylistQuery(definition, nowMs = 1_700_000_000_000L)
        val sql = query.sql

        assertTrue(sql.contains("isAvailable = 1"))
        assertTrue(sql.contains("(totalListenMs * 100.0) / (durationMs * (playCount + skipCount))"))
        assertTrue(sql.contains("customTags LIKE '%' || ? || '%' COLLATE NOCASE"))
        assertTrue(sql.contains("NOT (skipCount > ?)"))
        assertTrue(sql.contains("genre = ? COLLATE NOCASE) OR (year BETWEEN ? AND ?"))
        assertTrue(sql.contains("ORDER BY playCount DESC"))
        assertTrue(sql.contains("LIMIT 75"))
        assertEquals(6, query.argCount)
    }

    @Test
    fun parseAndEncodeDefinition_roundTripsNestedRules() {
        val builtIns = SmartPlaylistEvaluator.defaultBuiltInSmartPlaylists()
        val highCompletion = builtIns.first { it.name.contains("High Completion") }
        val parsed = SmartPlaylistEvaluator.parseDefinition(highCompletion.definitionJson)

        assertEquals(SmartCombinator.ALL, parsed.rootGroup.combinator)
        assertEquals(SmartField.COMPLETION_PCT, parsed.rootGroup.conditions.first().field)
        assertEquals(1, parsed.rootGroup.childGroups.size)
        assertEquals(SmartCombinator.ANY, parsed.rootGroup.childGroups.first().combinator)

        val reEncoded = SmartPlaylistEvaluator.encodeDefinition(parsed)
        val reparsed = SmartPlaylistEvaluator.parseDefinition(reEncoded)
        assertEquals(parsed, reparsed)
    }
}
