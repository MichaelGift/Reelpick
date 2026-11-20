package com.myth.playpick.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.myth.playpick.BuildConfig
import com.myth.playpick.data.local.AppDatabase
import com.myth.playpick.data.local.ChecklistDao
import com.myth.playpick.data.local.FeedCacheDao
import com.myth.playpick.data.local.FeedbackDao
import com.myth.playpick.data.local.LibraryDao
import com.myth.playpick.data.prefs.UserPreferencesRepositoryImpl
import com.myth.playpick.data.remote.CacheInterceptor
import com.myth.playpick.data.remote.KeyInterceptor
import com.myth.playpick.data.remote.RawgApi
import com.myth.playpick.data.repository.ChecklistRepositoryImpl
import com.myth.playpick.data.repository.FeedCacheRepositoryImpl
import com.myth.playpick.data.repository.FeedbackRepositoryImpl
import com.myth.playpick.data.repository.GameRepositoryImpl
import com.myth.playpick.data.repository.LibraryRepositoryImpl
import com.myth.playpick.domain.repository.ChecklistRepository
import com.myth.playpick.domain.repository.FeedCacheRepository
import com.myth.playpick.domain.repository.FeedbackRepository
import com.myth.playpick.domain.repository.GameRepository
import com.myth.playpick.domain.repository.LibraryRepository
import com.myth.playpick.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "playpick_prefs")

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
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        // 20 MB of responses keeps repeat visits off the free tier's monthly quota.
        .cache(Cache(File(context.cacheDir, "rawg"), 20L * 1024 * 1024))
        .addInterceptor(KeyInterceptor(BuildConfig.RAWG_API_KEY))
        .addNetworkInterceptor(CacheInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                val logger = HttpLoggingInterceptor { Log.d("RAWG", KeyInterceptor.redact(it)) }
                addInterceptor(logger.apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRawgApi(client: OkHttpClient, json: Json): RawgApi = Retrofit.Builder()
        .baseUrl(BuildConfig.RAWG_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RawgApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "playpick.db").build()

    @Provides fun provideFeedbackDao(db: AppDatabase): FeedbackDao = db.feedbackDao()
    @Provides fun provideLibraryDao(db: AppDatabase): LibraryDao = db.libraryDao()
    @Provides fun provideChecklistDao(db: AppDatabase): ChecklistDao = db.checklistDao()
    @Provides fun provideFeedCacheDao(db: AppDatabase): FeedCacheDao = db.feedCacheDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.userPrefsDataStore
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository
    @Binds abstract fun bindFeedbackRepository(impl: FeedbackRepositoryImpl): FeedbackRepository
    @Binds abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository
    @Binds abstract fun bindChecklistRepository(impl: ChecklistRepositoryImpl): ChecklistRepository
    @Binds abstract fun bindFeedCacheRepository(impl: FeedCacheRepositoryImpl): FeedCacheRepository
    @Binds abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
