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
