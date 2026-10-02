package com.planecatcher.core

import com.planecatcher.core.rules.GameRules.HOUR_MS
import com.planecatcher.core.rules.JumpDestinations
import com.planecatcher.core.rules.JumpState
import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.core.rules.LocationJump
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationJumpTest {
    private val target = JumpDestinations.all.first()
    private val t0 = 1_000_000_000_000L

    @Test
    fun fresh_state_is_ready() {
        assertEquals(JumpStatus.Ready, LocationJump.status(JumpState(), t0))
    }

    @Test
    fun activation_lasts_three_hours() {
        val s = LocationJump.activate(JumpState(), target, t0).getOrThrow()
        val st = LocationJump.status(s, t0 + 1 * HOUR_MS)
        assertTrue(st is JumpStatus.Active)
        assertEquals(2 * HOUR_MS, (st as JumpStatus.Active).remainingMs)
        assertEquals(target, st.target)
    }

    @Test
    fun timeout_starts_24h_cooldown_from_end() {
        val s = LocationJump.activate(JumpState(), target, t0).getOrThrow()
        val st = LocationJump.status(s, t0 + 4 * HOUR_MS)
        // Ended at 3h; cooldown runs until 27h, so 23h remain at 4h.
        assertEquals(JumpStatus.Cooldown(23 * HOUR_MS), st)
        assertEquals(JumpStatus.Ready, LocationJump.status(s, t0 + 27 * HOUR_MS))
    }

    @Test
    fun catching_a_plane_ends_jump_immediately() {
        val s = LocationJump.activate(JumpState(), target, t0).getOrThrow()
        val caughtAt = t0 + 30 * 60_000L
        val ended = LocationJump.onPlaneCaught(s, caughtAt)
        assertEquals(JumpStatus.Cooldown(24 * HOUR_MS), LocationJump.status(ended, caughtAt))
        assertEquals(JumpStatus.Ready, LocationJump.status(ended, caughtAt + 24 * HOUR_MS))
    }

    @Test
    fun catch_without_active_jump_changes_nothing() {
        val idle = JumpState()
        assertEquals(idle, LocationJump.onPlaneCaught(idle, t0))
        val cooling = JumpState(endedAtMs = t0)
        assertEquals(cooling, LocationJump.onPlaneCaught(cooling, t0 + HOUR_MS))
    }

    @Test
    fun cannot_activate_during_active_or_cooldown() {
        val s = LocationJump.activate(JumpState(), target, t0).getOrThrow()
        assertTrue(LocationJump.activate(s, target, t0 + HOUR_MS).isFailure)
        val ended = LocationJump.end(s, t0 + HOUR_MS)
        assertTrue(LocationJump.activate(ended, target, t0 + 2 * HOUR_MS).isFailure)
        assertTrue(LocationJump.activate(ended, target, t0 + 25 * HOUR_MS).isSuccess)
    }

    @Test
    fun rewinding_clock_does_not_unlock() {
        val ended = JumpState(endedAtMs = t0)
        val st = LocationJump.status(ended, t0 - 48 * HOUR_MS)
        assertTrue(st is JumpStatus.Cooldown)
    }
}
