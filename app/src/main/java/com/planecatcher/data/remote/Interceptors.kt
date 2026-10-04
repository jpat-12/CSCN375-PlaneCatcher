package com.planecatcher.data.remote

import com.planecatcher.BuildConfig
import com.planecatcher.data.time.TrustedClock
import okhttp3.Interceptor
import okhttp3.Response

/** Feeds server `Date` headers into the [TrustedClock] so cooldowns use server time. */
class ServerTimeInterceptor(private val clock: TrustedClock) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        response.headers.getDate("Date")?.let { clock.onServerTime(it.time) }
        return response
    }
}

/** Identifies the app to the APIs. Planespotters requires a contact URL in the User-Agent. */
class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder()
            .header("User-Agent", "Blip/${BuildConfig.VERSION_NAME} (+$CONTACT_URL)")
            .build(),
    )
}

const val CONTACT_URL = "https://github.com/jpat-12/CSCN375-PlaneCatcher"
