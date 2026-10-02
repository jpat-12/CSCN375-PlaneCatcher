package com.planecatcher.di

import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.planecatcher.BuildConfig
import com.planecatcher.core.quiz.QuizGenerator
import com.planecatcher.data.AdsbRepository
import com.planecatcher.data.PlaneRepository
import com.planecatcher.data.local.AppDatabase
import com.planecatcher.data.local.CaughtPlaneDao
import com.planecatcher.data.local.NearbyCacheDao
import com.planecatcher.data.local.QuizLockDao
import com.planecatcher.data.remote.AdsbFiApi
import com.planecatcher.data.remote.AdsbLolApi
import com.planecatcher.data.remote.PlanespottersApi
import com.planecatcher.data.remote.ServerTimeInterceptor
import com.planecatcher.data.remote.UserAgentInterceptor
import com.planecatcher.data.time.TrustedClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(clock: TrustedClock): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor())
        .addNetworkInterceptor(ServerTimeInterceptor(clock))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAdsbFiApi(client: OkHttpClient, json: Json): AdsbFiApi =
        retrofit(AdsbFiApi.BASE_URL, client, json).create(AdsbFiApi::class.java)

    @Provides
    @Singleton
    fun provideAdsbLolApi(client: OkHttpClient, json: Json): AdsbLolApi =
        retrofit(AdsbLolApi.BASE_URL, client, json).create(AdsbLolApi::class.java)

    @Provides
    @Singleton
    fun providePlanespottersApi(client: OkHttpClient, json: Json): PlanespottersApi =
        retrofit(PlanespottersApi.BASE_URL, client, json).create(PlanespottersApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "planecatcher.db").build()

    @Provides fun provideCaughtDao(db: AppDatabase): CaughtPlaneDao = db.caughtPlaneDao()
    @Provides fun provideNearbyDao(db: AppDatabase): NearbyCacheDao = db.nearbyCacheDao()
    @Provides fun provideLockDao(db: AppDatabase): QuizLockDao = db.quizLockDao()

    @Provides
    @Singleton
    fun provideFusedLocation(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    fun provideQuizGenerator(): QuizGenerator = QuizGenerator()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    abstract fun bindPlaneRepository(impl: AdsbRepository): PlaneRepository
}
