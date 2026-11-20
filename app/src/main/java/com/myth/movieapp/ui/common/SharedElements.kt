package com.myth.movieapp.ui.common

import com.myth.movieapp.domain.model.Title

/** Open a movie's details; [sharedKey] lets the tapped poster morph into the details poster. */
typealias OpenTitle = (movie: Title, sharedKey: String?) -> Unit

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
