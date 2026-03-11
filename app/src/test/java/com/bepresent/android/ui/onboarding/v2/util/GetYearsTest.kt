package com.bepresent.android.ui.onboarding.v2.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GetYearsTest {

    @Test
    fun `maps iOS screen time buckets to expected year estimates`() {
        assertEquals(6, calculateYearsOnPhone("1-2 hours"))
        assertEquals(13, calculateYearsOnPhone("3-4 hours"))
        assertEquals(16, calculateYearsOnPhone("4-5 hours"))
        assertEquals(30, calculateYearsOnPhone("Over 8 hours"))
    }

    @Test
    fun `uses iOS fallback when bucket is unrecognized`() {
        assertEquals(16, calculateYearsOnPhone("unknown"))
    }

    @Test
    fun `supports legacy android buckets for resumed onboarding`() {
        assertEquals(6, calculateYearsOnPhone("Less than 2 hours"))
        assertEquals(26, calculateYearsOnPhone("6-8 hours"))
        assertEquals(30, calculateYearsOnPhone("10+ hours"))
    }

    @Test
    fun `formats years back like iOS`() {
        assertEquals("8", calculateYearsBack(16))
        assertEquals("6.5", calculateYearsBack(13))
    }
}
