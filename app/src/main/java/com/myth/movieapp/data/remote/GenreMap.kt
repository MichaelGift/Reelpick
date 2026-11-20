package com.myth.movieapp.data.remote

/**
 * Puts movies and series in one genre space, so a liked series and a liked film teach the same
 * taste profile.
 *
 * TMDB's TV list shares most ids with the movie list but bundles a few into combined genres
 * ("Action & Adventure", "Sci-Fi & Fantasy", ...). Those are split into their movie equivalents
 * whenever a series is converted to a domain Title. TV-only genres with no film counterpart
 * (News, Reality, Soap, Talk) keep their own ids.
 */
object GenreMap {
    private const val ACTION = 28
    private const val ADVENTURE = 12
    private const val SCIENCE_FICTION = 878
    private const val FANTASY = 14
    private const val WAR = 10752
    private const val FAMILY = 10751

    private const val TV_ACTION_ADVENTURE = 10759
    private const val TV_KIDS = 10762
    private const val TV_SCI_FI_FANTASY = 10765
    private const val TV_WAR_POLITICS = 10768

    private val tvToMovie = mapOf(
        TV_ACTION_ADVENTURE to listOf(ACTION, ADVENTURE),
        TV_SCI_FI_FANTASY to listOf(SCIENCE_FICTION, FANTASY),
        TV_WAR_POLITICS to listOf(WAR),
        TV_KIDS to listOf(FAMILY),
    )

    /** Ids TMDB uses for the same genre in both lists. */
    private val shared = setOf(16, 35, 80, 99, 18, FAMILY, 9648, 37)

    private val movieToTv = mapOf(
        ACTION to TV_ACTION_ADVENTURE,
        ADVENTURE to TV_ACTION_ADVENTURE,
        SCIENCE_FICTION to TV_SCI_FI_FANTASY,
        FANTASY to TV_SCI_FI_FANTASY,
        WAR to TV_WAR_POLITICS,
    )

    /** A series' TMDB genre ids, expressed in the shared (movie-based) genre space. */
    fun normalizeTv(tvGenreIds: List<Int>): List<Int> =
        tvGenreIds.flatMap { tvToMovie[it] ?: listOf(it) }.distinct()

    /**
     * The TV genre to query `discover/tv` with for a profile genre, or null when TV has no
     * equivalent (Horror, Thriller, Romance, ...), in which case series simply aren't fetched for it.
     */
    fun tvGenreFor(genreId: Int): Int? = movieToTv[genreId] ?: genreId.takeIf { it in shared || it !in movieGenreIds }

    /** True for TV genres with no movie equivalent; these are added to the shared genre list. */
    fun isTvOnly(tvGenreId: Int): Boolean = tvGenreId !in tvToMovie && tvGenreId !in movieGenreIds

    // Every movie genre id TMDB defines; anything else in the shared space is TV-only.
    private val movieGenreIds = setOf(
        28, 12, 16, 35, 80, 99, 18, 10751, 14, 36, 27, 10402, 9648, 10749, 878, 10770, 53, 10752, 37,
    )
}
