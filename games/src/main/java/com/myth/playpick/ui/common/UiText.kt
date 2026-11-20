package com.myth.playpick.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.myth.playpick.data.remote.MissingRawgKeyException
import retrofit2.HttpException
import java.io.IOException

/** RAWG serves resized artwork when "resize/{width}/-/" follows "/media/"; full-size covers are often 1920px. */
object RawgImages {
    fun resized(url: String?, width: Int): String? = when {
        url == null -> null
        "/media/resize/" in url -> url
        else -> url.replaceFirst("/media/", "/media/resize/$width/-/")
    }

    fun cover(url: String?) = resized(url, 640)
    fun hero(url: String?) = resized(url, 1280)
    fun icon(url: String?) = resized(url, 200)
}

fun Throwable.toUserMessage(): String = when (this) {
    is MissingRawgKeyException -> "No RAWG key configured. Add RAWG_API_KEY to local.properties and rebuild."
    is HttpException -> when (code()) {
        401 -> "RAWG rejected the API key. Check RAWG_API_KEY."
        404 -> "That game couldn't be found."
        429 -> "Too many requests to RAWG. Try again in a moment."
        else -> "RAWG returned an error (${code()})."
    }
    is IOException -> "Couldn't reach RAWG. Check your connection."
    else -> "Something went wrong."
}

/** RAWG's terms require an active link back on every screen that shows their data. */
@Composable
fun RawgAttribution(modifier: Modifier = Modifier) {
    val text = buildAnnotatedString {
        append("Game data and images from ")
        withLink(
            LinkAnnotation.Url(
                "https://rawg.io",
                TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)),
            ),
        ) { append("RAWG") }
    }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
