package com.bepresent.android.features.sessions

/**
 * Social media apps that are always blocked during any session by default.
 * Users do not need to manually select these — they are automatically included.
 */
object DefaultBlockedApps {

    val PACKAGES: Set<String> = setOf(
        // Video & streaming
        "com.google.android.youtube",
        "com.google.android.youtube.tv",

        // Meta
        "com.facebook.katana",
        "com.facebook.lite",
        "com.facebook.orca",          // Messenger
        "com.instagram.android",
        "com.instagram.barcelona",    // Threads

        // X / Twitter
        "com.twitter.android",

        // TikTok
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",

        // Snapchat
        "com.snapchat.android",

        // Reddit
        "com.reddit.frontpage",

        // Pinterest
        "com.pinterest",

        // LinkedIn
        "com.linkedin.android",

        // Discord
        "com.discord",

        // Telegram
        "org.telegram.messenger",

        // WhatsApp
        "com.whatsapp",

        // BeReal
        "com.bereal.ft",

        // Tumblr
        "com.tumblr",
    )
}

/**
 * Productivity and utility apps that are always allowed through during sessions.
 * These are non-distracting and should never be blocked.
 */
object DefaultAllowedApps {

    val PACKAGES: Set<String> = setOf(
        // Weather
        "com.google.android.apps.weather",
        "com.samsung.android.weather",
        "com.accuweather.android",
        "com.weather.Weather",

        // Calculator
        "com.google.android.calculator",
        "com.samsung.android.calculator",
        "com.sec.android.app.popupcalculator",

        // Calendar
        "com.google.android.calendar",
        "com.samsung.android.calendar",

        // Clock / Alarm
        "com.google.android.deskclock",
        "com.samsung.android.app.clockpackage",
        "com.sec.android.app.clockpackage",

        // Maps & navigation
        "com.google.android.apps.maps",
        "com.waze",

        // Phone & contacts
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.google.android.contacts",
        "com.samsung.android.contacts",

        // Camera
        "com.google.android.GoogleCamera",
        "com.samsung.android.camera",
        "com.sec.android.app.camera",

        // Files / storage
        "com.google.android.apps.nbu.files",
        "com.sec.android.app.myfiles",

        // Settings
        "com.android.settings",

        // Notes / productivity
        "com.google.android.keep",
        "com.samsung.android.app.notes",
        "com.microsoft.office.onenote",

        // Email
        "com.google.android.gm",
        "com.microsoft.office.outlook",

        // Banking & finance (useful, not distracting)
        "com.google.android.apps.walletnfcrel",

        // Fitness / health
        "com.google.android.apps.fitness",
        "com.samsung.android.shealth",

    )
}
