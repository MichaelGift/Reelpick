package com.myth.movieapp.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.myth.movieapp.domain.model.Title

/** Open a movie's details; [sharedKey] lets the tapped poster morph into the details poster. */
typealias OpenTitle = (movie: Title, sharedKey: String?) -> Unit

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The navigation destination's enter/exit scope, provided per NavHost destination. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Stable list/animation key for a title. Includes the media type because TMDB reuses ids across
 * movies and series, and two items with the same key crash a lazy list.
 */
val Title.stableKey: String get() = "${mediaType.name}-$id"

/** Open a person's page; the headshot morphs into the page header via [sharedKey]. */
typealias OpenPerson = (personId: Int, name: String, profilePath: String?, sharedKey: String?) -> Unit

fun personKey(section: String, personId: Int) = "person-$section-$personId"

/** Unique per on-screen occurrence: the same title can appear in several rows at once. */
fun posterKey(section: String, title: Title) = "poster-$section-${title.stableKey}"

/**
 * Marks a poster as a shared element. A no-op outside a shared transition (previews, tests) or
 * when [key] is null.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPoster(key: String?): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedScope.current
    if (key == null || shared == null || animated == null) return this
    return with(shared) {
        this@sharedPoster.sharedElement(rememberSharedContentState(key), animatedVisibilityScope = animated)
    }
}
