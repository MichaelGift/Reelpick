package com.myth.movieapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.myth.movieapp.BuildConfig
import com.myth.movieapp.data.local.AppDatabase
import com.myth.movieapp.data.local.FeedCacheDao
import com.myth.movieapp.data.local.FeedbackDao
import com.myth.movieapp.data.local.GenreDao
import com.myth.movieapp.data.local.WatchlistDao
import com.myth.movieapp.data.prefs.UserPreferencesRepositoryImpl
import com.myth.movieapp.data.remote.AuthInterceptor
import com.myth.movieapp.data.remote.TmdbApi
import com.myth.movieapp.data.repository.FeedCacheRepositoryImpl
import com.myth.movieapp.data.repository.FeedbackRepositoryImpl
import com.myth.movieapp.data.repository.MovieRepositoryImpl
import com.myth.movieapp.data.repository.WatchlistRepositoryImpl
import com.myth.movieapp.domain.repository.FeedCacheRepository
import com.myth.movieapp.domain.repository.FeedbackRepository
import com.myth.movieapp.domain.repository.MovieRepository
import com.myth.movieapp.domain.repository.UserPreferencesRepository
import com.myth.movieapp.domain.repository.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(BuildConfig.TMDB_ACCESS_TOKEN))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideTmdbApi(client: OkHttpClient, json: Json): TmdbApi = Retrofit.Builder()
        .baseUrl(BuildConfig.TMDB_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TmdbApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "movies.db").build()

    @Provides fun provideFeedbackDao(db: AppDatabase): FeedbackDao = db.feedbackDao()
    @Provides fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
    @Provides fun provideGenreDao(db: AppDatabase): GenreDao = db.genreDao()
    @Provides fun provideFeedCacheDao(db: AppDatabase): FeedCacheDao = db.feedCacheDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.userPrefsDataStore
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository
    @Binds abstract fun bindFeedbackRepository(impl: FeedbackRepositoryImpl): FeedbackRepository
    @Binds abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository
    @Binds abstract fun bindFeedCacheRepository(impl: FeedCacheRepositoryImpl): FeedCacheRepository
    @Binds abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
