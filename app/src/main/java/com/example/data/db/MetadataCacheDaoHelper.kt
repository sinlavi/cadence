package com.example.data.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataCacheDaoHelper(private val dao: CustomizationAndCacheDao) {
    suspend fun getFreshJson(key: String, maxAgeMs: Long): String? = withContext(Dispatchers.IO) {
        val entry = dao.getCache(key) ?: return@withContext null
        if (System.currentTimeMillis() - entry.cachedAt <= maxAgeMs) entry.payloadJson else null
    }

    suspend fun putJson(key: String, provider: String, json: String) = withContext(Dispatchers.IO) {
        dao.putCache(
            MetadataCacheEntity(
                cacheKey = key,
                provider = provider,
                payloadJson = json,
                cachedAt = System.currentTimeMillis()
            )
        )
    }
}
