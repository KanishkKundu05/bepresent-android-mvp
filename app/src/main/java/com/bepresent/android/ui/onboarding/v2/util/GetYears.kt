package com.bepresent.android.ui.onboarding.v2.util

import java.util.Locale

/**
 * Calculates the lifetime estimate shown in onboarding.
 *
 * iOS logic uses the selected daily screen-time bucket, assumes an 80-year
 * lifespan, and truncates via integer division:
 * (hours * 365 * 80) / (24 * 365) == hours * 80 / 24
 */
fun calculateYearsOnPhone(screenTimeAnswer: String): Int =
    (screenTimeAnswerToHours(screenTimeAnswer) * 80) / 24

/**
 * Maps a screen-time bucket to the coarse iOS upper-bound hour estimate.
 *
 * The extra legacy branches preserve compatibility for any answers already
 * saved by earlier Android builds.
 */
fun screenTimeAnswerToHours(answer: String): Int = when (answer.trim()) {
    "1-2 hours",
    "1-2 Hours",
    "Less than 2 hours" -> 2
    "2-3 hours",
    "2-3 Hours" -> 3
    "3-4 hours",
    "3-4 Hours" -> 4
    "4-5 hours",
    "4-5 Hours" -> 5
    "5-6 hours",
    "5-6 Hours" -> 6
    "6-7 hours",
    "6-7 Hours" -> 7
    "7-8 hours",
    "7-8 Hours",
    "6-8 hours" -> 8
    "Over 8 hours",
    "Over 8 Hours",
    "8-10 hours",
    "10+ hours" -> 9
    else -> 5
}

/**
 * Returns the "years back" string shown on shock page 2.
 * Matches iOS by dividing by two, keeping one decimal place, then stripping
 * a trailing ".0".
 */
fun calculateYearsBack(yearsOnPhone: Int): String {
    val formatted = String.format(Locale.US, "%.1f", yearsOnPhone / 2f)
    return formatted.removeSuffix(".0")
}
