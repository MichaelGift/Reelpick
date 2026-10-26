package com.myth.movieapp.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.myth.movieapp.domain.model.Movie

/** Open a movie's details; [sharedKey] lets the tapped poster morph into the details poster. */
typealias OpenMovie = (movie: Movie, sharedKey: String?) -> Unit

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The navigation destination's enter/exit scope, provided per NavHost destination. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Unique per on-screen occurrence: the same movie can appear in several rows at once. */
fun posterKey(section: String, movieId: Int) = "poster-$section-$movieId"

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
