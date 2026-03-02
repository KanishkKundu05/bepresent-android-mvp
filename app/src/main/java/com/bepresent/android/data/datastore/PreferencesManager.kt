package com.bepresent.android.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bepresent_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TOTAL_XP = intPreferencesKey("total_xp")
        val TOTAL_COINS = intPreferencesKey("total_coins")
        val STREAK_FREEZE_AVAILABLE = booleanPreferencesKey("streak_freeze_available")
        val LAST_FREEZE_GRANT_DATE = stringPreferencesKey("last_freeze_grant_date")
        val ACTIVE_SESSION_ID = stringPreferencesKey("active_session_id")

        // Onboarding V2
        val ONBOARDING_V2_PROGRESS = intPreferencesKey("onboarding_v2_progress")
        val ONBOARDING_V2_ANSWERS = stringPreferencesKey("onboarding_v2_answers")
        val ONBOARDING_V2_USERNAME = stringPreferencesKey("onboarding_v2_username")

        // Subscription
        val SUBSCRIPTION_ACTIVE = booleanPreferencesKey("subscription_active")
        val SUBSCRIPTION_EXPIRY = stringPreferencesKey("subscription_expiry")
        val STRIPE_CUSTOMER_ID = stringPreferencesKey("stripe_customer_id")
        val DEVICE_UUID = stringPreferencesKey("device_uuid")
    }

    // Flows
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val totalXp: Flow<Int> = dataStore.data.map { it[Keys.TOTAL_XP] ?: 0 }
    val totalCoins: Flow<Int> = dataStore.data.map { it[Keys.TOTAL_COINS] ?: 0 }
    val streakFreezeAvailable: Flow<Boolean> = dataStore.data.map { it[Keys.STREAK_FREEZE_AVAILABLE] ?: true }
    val lastFreezeGrantDate: Flow<String> = dataStore.data.map { it[Keys.LAST_FREEZE_GRANT_DATE] ?: "" }
    val activeSessionId: Flow<String?> = dataStore.data.map { it[Keys.ACTIVE_SESSION_ID] }

    // Setters
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun addXpAndCoins(xp: Int, coins: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.TOTAL_XP] = (prefs[Keys.TOTAL_XP] ?: 0) + xp
            prefs[Keys.TOTAL_COINS] = (prefs[Keys.TOTAL_COINS] ?: 0) + coins
        }
    }

    suspend fun setStreakFreezeAvailable(available: Boolean) {
        dataStore.edit { it[Keys.STREAK_FREEZE_AVAILABLE] = available }
    }

    suspend fun setLastFreezeGrantDate(date: String) {
        dataStore.edit { it[Keys.LAST_FREEZE_GRANT_DATE] = date }
    }

    suspend fun setActiveSessionId(sessionId: String?) {
        dataStore.edit { prefs ->
            if (sessionId != null) {
                prefs[Keys.ACTIVE_SESSION_ID] = sessionId
            } else {
                prefs.remove(Keys.ACTIVE_SESSION_ID)
            }
        }
    }

    suspend fun getStreakFreezeAvailableOnce(): Boolean {
        return dataStore.data.first()[Keys.STREAK_FREEZE_AVAILABLE] ?: true
    }

    suspend fun getLastFreezeGrantDateOnce(): String {
        return dataStore.data.first()[Keys.LAST_FREEZE_GRANT_DATE] ?: ""
    }

    // ── Onboarding V2 ──

    suspend fun setOnboardingV2Progress(index: Int) {
        dataStore.edit { it[Keys.ONBOARDING_V2_PROGRESS] = index }
    }

    suspend fun getOnboardingV2ProgressOnce(): Int {
        return dataStore.data.first()[Keys.ONBOARDING_V2_PROGRESS] ?: 0
    }

    suspend fun setOnboardingV2Answers(encoded: String) {
        dataStore.edit { it[Keys.ONBOARDING_V2_ANSWERS] = encoded }
    }

    suspend fun getOnboardingV2AnswersOnce(): String {
        return dataStore.data.first()[Keys.ONBOARDING_V2_ANSWERS] ?: ""
    }

    suspend fun setOnboardingV2Username(username: String) {
        dataStore.edit { it[Keys.ONBOARDING_V2_USERNAME] = username }
    }

    suspend fun getOnboardingV2UsernameOnce(): String {
        return dataStore.data.first()[Keys.ONBOARDING_V2_USERNAME] ?: ""
    }

    suspend fun clearOnboardingV2Progress() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ONBOARDING_V2_PROGRESS)
            prefs.remove(Keys.ONBOARDING_V2_ANSWERS)
            prefs.remove(Keys.ONBOARDING_V2_USERNAME)
        }
    }

    // ── Subscription ──

    suspend fun setSubscriptionActive(active: Boolean) {
        dataStore.edit { it[Keys.SUBSCRIPTION_ACTIVE] = active }
    }

    suspend fun getSubscriptionActiveOnce(): Boolean {
        return dataStore.data.first()[Keys.SUBSCRIPTION_ACTIVE] ?: false
    }

    suspend fun setSubscriptionExpiry(expiry: String) {
        dataStore.edit { it[Keys.SUBSCRIPTION_EXPIRY] = expiry }
    }

    suspend fun setStripeCustomerId(customerId: String) {
        dataStore.edit { it[Keys.STRIPE_CUSTOMER_ID] = customerId }
    }

    suspend fun getStripeCustomerIdOnce(): String? {
        return dataStore.data.first()[Keys.STRIPE_CUSTOMER_ID]
    }

    suspend fun getOrCreateDeviceUuid(): String {
        val existing = dataStore.data.first()[Keys.DEVICE_UUID]
        if (existing != null) return existing
        val uuid = UUID.randomUUID().toString()
        dataStore.edit { it[Keys.DEVICE_UUID] = uuid }
        return uuid
    }
}
