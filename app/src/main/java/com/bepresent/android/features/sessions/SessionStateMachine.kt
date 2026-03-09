package com.bepresent.android.features.sessions

import com.bepresent.android.data.db.PresentSession
import com.bepresent.android.data.db.PresentSessionAction

object SessionStateMachine {

    data class Transition(
        val newState: String,
        val action: String,
        val cancelAlarm: Boolean = false,
        val clearActiveSession: Boolean = false,
        val syncAfter: Boolean = false,
        val rewardsEligible: Boolean = false,
        val setEndedAt: Boolean = false,
        val setGoalReachedAt: Boolean = false
    )

    sealed class TransitionResult {
        data class Success(val transition: Transition) : TransitionResult()
        data class Error(val message: String) : TransitionResult()
    }

    fun start(session: PresentSession): TransitionResult {
        return if (session.state == PresentSession.STATE_IDLE) {
            TransitionResult.Success(
                Transition(
                    newState = PresentSession.STATE_ACTIVE,
                    action = PresentSessionAction.ACTION_START
                )
            )
        } else {
            TransitionResult.Error("Cannot start session in state: ${session.state}")
        }
    }

    fun cancel(session: PresentSession): TransitionResult {
        if (session.state != PresentSession.STATE_ACTIVE) {
            return TransitionResult.Error("Cannot cancel session in state: ${session.state}")
        }
        val elapsed = System.currentTimeMillis() - (session.startedAt ?: 0)
        return if (elapsed <= 10_000) {
            TransitionResult.Success(
                Transition(
                    newState = PresentSession.STATE_CANCELED,
                    action = PresentSessionAction.ACTION_CANCEL,
                    cancelAlarm = true,
                    clearActiveSession = true,
                    setEndedAt = true
                )
            )
        } else {
            TransitionResult.Error("Cannot cancel after 10 seconds — use Give Up instead")
        }
    }

    fun giveUp(session: PresentSession): TransitionResult {
        if (session.state != PresentSession.STATE_ACTIVE) {
            return TransitionResult.Error("Cannot give up session in state: ${session.state}")
        }
        return if (!session.beastMode) {
            TransitionResult.Success(
                Transition(
                    newState = PresentSession.STATE_GAVE_UP,
                    action = PresentSessionAction.ACTION_GIVE_UP,
                    cancelAlarm = true,
                    clearActiveSession = true,
                    syncAfter = true,
                    setEndedAt = true
                )
            )
        } else {
            TransitionResult.Error("Beast Mode is enabled — cannot give up")
        }
    }

    fun goalReached(session: PresentSession): TransitionResult {
        return if (session.state == PresentSession.STATE_ACTIVE) {
            TransitionResult.Success(
                Transition(
                    newState = PresentSession.STATE_GOAL_REACHED,
                    action = PresentSessionAction.ACTION_GOAL_REACHED,
                    setGoalReachedAt = true
                )
            )
        } else {
            TransitionResult.Error("Cannot reach goal in state: ${session.state}")
        }
    }

    fun complete(session: PresentSession): TransitionResult {
        return if (session.state == PresentSession.STATE_GOAL_REACHED) {
            TransitionResult.Success(
                Transition(
                    newState = PresentSession.STATE_COMPLETED,
                    action = PresentSessionAction.ACTION_COMPLETE,
                    clearActiveSession = true,
                    syncAfter = true,
                    rewardsEligible = true,
                    setEndedAt = true
                )
            )
        } else {
            TransitionResult.Error("Cannot complete session in state: ${session.state}")
        }
    }

    fun calculateRewards(goalDurationMinutes: Int): Pair<Int, Int> = when {
        goalDurationMinutes <= 15  -> 3 to 1
        goalDurationMinutes <= 30  -> 5 to 2
        goalDurationMinutes <= 45  -> 8 to 4
        goalDurationMinutes <= 60  -> 10 to 5
        goalDurationMinutes <= 75  -> 13 to 7
        goalDurationMinutes <= 90  -> 15 to 8
        goalDurationMinutes <= 105 -> 20 to 9
        else                       -> 25 to 10
    }
}
