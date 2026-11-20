package com.myth.movieapp.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.myth.movieapp.data.local.AppDatabase
import com.myth.movieapp.data.local.MIGRATION_1_2
import com.myth.movieapp.data.repository.FeedbackRepositoryImpl
import com.myth.movieapp.data.repository.WatchlistRepositoryImpl
import com.myth.movieapp.domain.model.FeedbackType
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.testing.movieKey
import com.myth.movieapp.testing.seriesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun likesAndWatchlistSurviveTheTvMigration() = runTest {
        // A v1 database exactly as the movie-only app wrote it: JSON without a mediaType field.
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO feedback (movieId, type, timestampMillis, movie) VALUES " +
                    "(27205, 'LIKE', 10, '{\"id\":27205,\"title\":\"Inception\",\"genreIds\":[878]}')",
            )
            execSQL(
                "INSERT INTO watchlist (movieId, addedAtMillis, movie) VALUES " +
                    "(155, 20, '{\"id\":155,\"title\":\"The Dark Knight\"}')",
            )
            execSQL("INSERT INTO feed_cache (id, feed) VALUES (0, '{\"stale\":true}')")
            close()
        }

        // Room validates the migrated schema against 2.json here.
        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).close()

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            dbName,
        ).addMigrations(MIGRATION_1_2).build()
        try {
            val feedback = FeedbackRepositoryImpl(db.feedbackDao(), Clock.systemUTC())
            val liked = feedback.feedback.first().single()
            assertEquals("Inception", liked.movie.title)
            assertEquals(MediaType.MOVIE, liked.movie.mediaType)
            assertEquals(listOf(878), liked.movie.genreIds)
            assertEquals(FeedbackType.LIKE, feedback.feedbackFor(movieKey(27205)).first())

            val watchlist = WatchlistRepositoryImpl(db.watchlistDao(), Clock.systemUTC())
            assertTrue(watchlist.isInWatchlist(movieKey(155)).first())
            // The same id as a series is a different title.
            assertFalse(watchlist.isInWatchlist(seriesKey(155)).first())

            assertEquals(null, db.feedCacheDao().observe(0).first())
        } finally {
            db.close()
        }
    }
}
