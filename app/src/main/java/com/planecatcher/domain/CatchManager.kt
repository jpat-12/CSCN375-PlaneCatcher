package com.planecatcher.domain

import com.planecatcher.core.catalog.AircraftCatalog
import com.planecatcher.core.model.GeoPoint
import com.planecatcher.core.quiz.QuizQuestion
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.core.rules.QuizLock
import com.planecatcher.data.PhotoRepository
import com.planecatcher.data.local.CaughtPlaneDao
import com.planecatcher.data.local.CaughtPlaneEntity
import com.planecatcher.data.local.QuizLockDao
import com.planecatcher.data.local.QuizLockEntity
import com.planecatcher.data.time.TrustedClock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CatchResult {
    data class Caught(val plane: CaughtPlaneEntity) : CatchResult
    data class Missed(val correctAnswer: String, val retryAtMs: Long) : CatchResult
}

/** Applies a quiz answer: a right answer catches the plane, a wrong one locks it for an hour. */
@Singleton
class CatchManager @Inject constructor(
    private val caughtDao: CaughtPlaneDao,
    private val lockDao: QuizLockDao,
    private val photos: PhotoRepository,
    private val jump: JumpManager,
    private val clock: TrustedClock,
) {
    suspend fun lockFor(hex: String): QuizLock? =
        lockDao.get(hex)?.let { QuizLock(it.planeHex, it.retryAt, it.failedKind) }

    suspend fun submit(
        plane: NearbyPlane,
        question: QuizQuestion,
        choice: Int,
        catchCenter: GeoPoint,
        jumpCode: String?,
    ): CatchResult {
        val now = clock.now()
        if (!question.isCorrect(choice)) {
            val lock = QuizLock.afterWrongAnswer(plane.hex, now, question.kind)
            lockDao.upsert(QuizLockEntity(lock.planeHex, lock.retryAtMs, lock.failedKind))
            return CatchResult.Missed(question.correctAnswer, lock.retryAtMs)
        }

        val a = plane.aircraft
        val photo = photos.cached(a.hex) ?: photos.photoFor(a.hex)
        val entity = CaughtPlaneEntity(
            hex = a.hex,
            callsign = a.callsign,
            registration = a.registration,
            typeCode = a.typeCode,
            typeName = AircraftCatalog.lookup(a.typeCode)?.fullName,
            tier = plane.tier,
            points = plane.tier.points,
            caughtAt = now,
            lat = catchCenter.lat,
            lon = catchCenter.lon,
            altitudeFt = a.altitudeFt,
            distanceMiles = plane.distanceMiles,
            isMilitary = a.isMilitary,
            jumpCode = jumpCode,
            photoUrl = photo?.url,
            photographer = photo?.photographer,
            photoLink = photo?.link,
        )
        caughtDao.insert(entity)
        lockDao.delete(a.hex)
        // Only a catch made *with* the jump ends it.
        if (jumpCode != null) jump.onPlaneCaught()
        return CatchResult.Caught(entity)
    }
}
