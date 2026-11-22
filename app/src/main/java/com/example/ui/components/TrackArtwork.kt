package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.SubcomposeAsyncImage
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.Options
import com.example.ui.theme.ArtworkPaletteColors
import com.example.ui.theme.LocalCadenceTheme
import com.example.ui.theme.extractArtworkPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteMemoryCache {
    private val cache = LruCache<String, ArtworkPaletteColors>(64)

    fun get(key: String): ArtworkPaletteColors? = cache.get(key)
    fun put(key: String, value: ArtworkPaletteColors) {
        cache.put(key, value)
    }

    fun removeByContentUri(contentUri: String) {
        cache.snapshot().keys
            .filter { it.contains(contentUri) }
            .forEach { cache.remove(it) }
    }

    fun clear() {
        cache.evictAll()
    }
}

@OptIn(ExperimentalCoilApi::class)
fun invalidateTrackArtworkCache(context: Context, contentUri: String, artworkUri: String?) {
    PaletteMemoryCache.removeByContentUri(contentUri)
    runCatching {
        val loader = Coil.imageLoader(context)
        listOf("lo", "hi").forEach { bucket ->
            val key = "embedded_art:$contentUri:$bucket"
            loader.memoryCache?.remove(MemoryCache.Key(key))
            loader.diskCache?.remove(key)
        }
        if (!artworkUri.isNullOrBlank()) {
            loader.memoryCache?.remove(MemoryCache.Key(artworkUri))
        }
    }
}

data class EmbeddedAudioArtworkRequest(
    val contentUri: String,
    val maxDimPx: Int
)

class EmbeddedAudioArtworkKeyer : Keyer<EmbeddedAudioArtworkRequest> {
    override fun key(data: EmbeddedAudioArtworkRequest, options: Options): String {
        val bucket = if (data.maxDimPx > 256) "hi" else "lo"
        return "embedded_art:${data.contentUri}:$bucket"
    }
}

class EmbeddedAudioArtworkFetcher(
    private val context: Context,
    private val data: EmbeddedAudioArtworkRequest
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val mmr = MediaMetadataRetriever()
        val bytes = try {
            mmr.setDataSource(context, Uri.parse(data.contentUri))
            mmr.embeddedPicture
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { mmr.release() }
        } ?: return@withContext null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        var sample = 1
        while (bounds.outWidth / sample > data.maxDimPx * 2 || bounds.outHeight / sample > data.maxDimPx * 2) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: return@withContext null

        DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = sample > 1,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<EmbeddedAudioArtworkRequest> {
        override fun create(
            data: EmbeddedAudioArtworkRequest,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = EmbeddedAudioArtworkFetcher(context, data)
    }
}

suspend fun loadTrackArtworkBitmap(
    context: Context,
    artworkUri: String?,
    contentUri: String?,
    maxDimPx: Int = 256
): Bitmap? = withContext(Dispatchers.IO) {
    if (!artworkUri.isNullOrBlank()) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(artworkUri))?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()?.let { return@withContext it }
    }
    if (!contentUri.isNullOrBlank()) {
        val mmr = MediaMetadataRetriever()
        val bytes = try {
            mmr.setDataSource(context, Uri.parse(contentUri))
            mmr.embeddedPicture
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { mmr.release() }
        }
        if (bytes != null) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxDimPx * 2 || bounds.outHeight / sample > maxDimPx * 2) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        }
    }
    null
}

@Composable
private fun ArtworkSolidPlaceholder(
    contentDescription: String?,
    isCircular: Boolean
) {
    val bgTint = MaterialTheme.colors.primary.copy(alpha = if (isCircular) 0.14f else 0.11f)
    val iconTint = MaterialTheme.colors.primary.copy(alpha = 0.78f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgTint),
        contentAlignment = Alignment.Center
    ) {
        if (isCircular) {
            val letter = contentDescription?.trim()?.firstOrNull()?.takeIf { it.isLetterOrDigit() }?.uppercaseChar()
            if (letter != null) {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TrackArtwork(
    artworkUri: String?,
    contentUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    highRes: Boolean = false,
    isCircular: Boolean = false
) {
    val ext = LocalCadenceTheme.current
    val radius = cornerRadius ?: ext.config.artworkCornerRadiusDp.dp
    val shape = if (isCircular) CircleShape else remember(radius) { RoundedCornerShape(radius) }
    val context = LocalContext.current

    val requestModel: Any? = remember(artworkUri, contentUri, highRes) {
        when {
            !contentUri.isNullOrBlank() -> EmbeddedAudioArtworkRequest(
                contentUri = contentUri,
                maxDimPx = if (highRes) 720 else 192
            )
            !artworkUri.isNullOrBlank() -> Uri.parse(artworkUri)
            else -> null
        }
    }

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
        border = BorderStroke(if (isCircular) 1.dp else 0.5.dp, ext.dividerColor),
        elevation = 0.dp
    ) {
        if (requestModel != null) {
            val targetPx = if (highRes) 900 else 240
            val imageRequest = remember(requestModel, targetPx, context) {
                ImageRequest.Builder(context)
                    .data(requestModel)
                    .size(targetPx)
                    .crossfade(false)
                    .build()
            }
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    ArtworkSolidPlaceholder(contentDescription = contentDescription, isCircular = isCircular)
                },
                error = {
                    ArtworkSolidPlaceholder(contentDescription = contentDescription, isCircular = isCircular)
                }
            )
        } else {
            ArtworkSolidPlaceholder(contentDescription = contentDescription, isCircular = isCircular)
        }
    }
}

suspend fun computeArtworkPaletteForTrack(
    context: Context,
    trackKey: String,
    artworkUri: String?,
    contentUri: String?
): ArtworkPaletteColors? = withContext(Dispatchers.Default) {
    PaletteMemoryCache.get(trackKey)?.let { return@withContext it }
    val bmp = loadTrackArtworkBitmap(context, artworkUri, contentUri, maxDimPx = 160) ?: return@withContext null
    val extracted = extractArtworkPalette(bmp)
    PaletteMemoryCache.put(trackKey, extracted)
    extracted
}
