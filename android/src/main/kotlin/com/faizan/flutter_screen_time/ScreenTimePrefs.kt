package com.faizan.flutter_screen_time

import android.content.Context
import android.content.SharedPreferences

/**
 * Small wrapper around [SharedPreferences] that stores everything the Android
 * app-blocker needs to survive process death: which packages are blocked,
 * whether blocking is currently switched on, and the "shield" overlay styling.
 *
 * The Android platform has no system-level Screen Time API, so this data is the
 * source of truth that the [AppBlockerAccessibilityService] reads on every
 * foreground-app change.
 */
internal object ScreenTimePrefs {
    private const val PREFS_NAME = "flutter_screen_time_prefs"

    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_BLOCKING_ENABLED = "blocking_enabled"

    private const val KEY_SHIELD_TITLE = "shield_title"
    private const val KEY_SHIELD_SUBTITLE = "shield_subtitle"
    private const val KEY_SHIELD_PRIMARY_LABEL = "shield_primary_label"
    private const val KEY_SHIELD_SECONDARY_LABEL = "shield_secondary_label"
    private const val KEY_SHIELD_PRIMARY_BG = "shield_primary_bg_hex"
    private const val KEY_SHIELD_BG = "shield_bg_hex"

    // Android-only overlay extras set via `configureAndroidOverlayUI`.
    private const val KEY_OVERLAY_TITLE_COLOR = "overlay_title_color_hex"
    private const val KEY_OVERLAY_MESSAGE_COLOR = "overlay_message_color_hex"
    private const val KEY_OVERLAY_PRIMARY_TEXT_COLOR = "overlay_primary_text_color_hex"
    private const val KEY_OVERLAY_SECONDARY_TEXT_COLOR = "overlay_secondary_text_color_hex"
    private const val KEY_OVERLAY_SECONDARY_BG = "overlay_secondary_bg_hex"
    private const val KEY_OVERLAY_TITLE_SIZE = "overlay_title_size_sp"
    private const val KEY_OVERLAY_MESSAGE_SIZE = "overlay_message_size_sp"
    private const val KEY_OVERLAY_CORNER_RADIUS = "overlay_corner_radius_dp"

    // Defaults mirror the values that used to be hard-coded in BlockAppService.
    private const val DEFAULT_BG = "#111827"
    private const val DEFAULT_PRIMARY_BG = "#2563EB"
    private const val DEFAULT_TITLE_COLOR = "#FFFFFF"
    private const val DEFAULT_MESSAGE_COLOR = "#D1D5DB"
    private const val DEFAULT_BUTTON_TEXT_COLOR = "#FFFFFF"
    private const val DEFAULT_TITLE_SIZE_SP = 24f
    private const val DEFAULT_MESSAGE_SIZE_SP = 16f
    private const val DEFAULT_CORNER_RADIUS_DP = 12f

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBlockedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
    }

    fun isBlockingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCKING_ENABLED, false)

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCKING_ENABLED, enabled).apply()
    }

    /** Persists the shield styling coming from `setShieldConfiguration`. */
    fun saveShieldConfiguration(context: Context, config: Map<String, Any?>) {
        prefs(context).edit().apply {
            putString(KEY_SHIELD_TITLE, config["title"] as? String)
            putString(KEY_SHIELD_SUBTITLE, config["subtitle"] as? String)
            putString(KEY_SHIELD_PRIMARY_LABEL, config["primaryButtonLabel"] as? String)
            putString(KEY_SHIELD_SECONDARY_LABEL, config["secondaryButtonLabel"] as? String)
            putString(KEY_SHIELD_PRIMARY_BG, config["primaryButtonBackgroundColorHex"] as? String)
            putString(KEY_SHIELD_BG, config["backgroundColorHex"] as? String)
        }.apply()
    }

    /**
     * Persists the Android-only overlay styling coming from
     * `configureAndroidOverlayUI`. Text/label fields overlap with the shared
     * shield config (same keys), while the colour, text-size and corner-radius
     * fields are Android-specific extras.
     *
     * Every key is (re)written from the incoming map: a missing/absent field is
     * cleared so it falls back to its default when read — the caller supplies a
     * complete configuration each time.
     */
    fun saveAndroidOverlayConfiguration(context: Context, config: AndroidOverlayConfiguration) {
        prefs(context).edit().apply {
            putString(KEY_SHIELD_TITLE, config.title)
            putString(KEY_SHIELD_SUBTITLE, config.message)
            putString(KEY_SHIELD_PRIMARY_LABEL, config.primaryButtonLabel)
            putString(KEY_SHIELD_SECONDARY_LABEL, config.secondaryButtonLabel)
            putString(KEY_SHIELD_BG, config.backgroundColorHex)
            putString(KEY_SHIELD_PRIMARY_BG, config.primaryButtonBackgroundColorHex)
            putString(KEY_OVERLAY_TITLE_COLOR, config.titleColorHex)
            putString(KEY_OVERLAY_MESSAGE_COLOR, config.messageColorHex)
            putString(KEY_OVERLAY_PRIMARY_TEXT_COLOR, config.primaryButtonTextColorHex)
            putString(KEY_OVERLAY_SECONDARY_TEXT_COLOR, config.secondaryButtonTextColorHex)
            putString(KEY_OVERLAY_SECONDARY_BG, config.secondaryButtonBackgroundColorHex)
            putFloatOrRemove(KEY_OVERLAY_TITLE_SIZE, config.titleFontSize)
            putFloatOrRemove(KEY_OVERLAY_MESSAGE_SIZE, config.messageFontSize)
            putFloatOrRemove(KEY_OVERLAY_CORNER_RADIUS, config.cornerRadius)
        }.apply()
    }

    private fun SharedPreferences.Editor.putFloatOrRemove(key: String, value: Float?) {
        if (value == null) remove(key) else putFloat(key, value)
    }

    fun getShieldConfiguration(context: Context): ShieldConfiguration {
        val p = prefs(context)
        return ShieldConfiguration(
            title = p.getString(KEY_SHIELD_TITLE, null) ?: "App blocked",
            subtitle = p.getString(KEY_SHIELD_SUBTITLE, null)
                ?: "This app is blocked right now.",
            primaryButtonLabel = p.getString(KEY_SHIELD_PRIMARY_LABEL, null) ?: "OK",
            secondaryButtonLabel = p.getString(KEY_SHIELD_SECONDARY_LABEL, null),
            primaryButtonBackgroundColorHex = p.getString(KEY_SHIELD_PRIMARY_BG, null) ?: DEFAULT_PRIMARY_BG,
            backgroundColorHex = p.getString(KEY_SHIELD_BG, null) ?: DEFAULT_BG,
            titleColorHex = p.getString(KEY_OVERLAY_TITLE_COLOR, null) ?: DEFAULT_TITLE_COLOR,
            messageColorHex = p.getString(KEY_OVERLAY_MESSAGE_COLOR, null) ?: DEFAULT_MESSAGE_COLOR,
            primaryButtonTextColorHex = p.getString(KEY_OVERLAY_PRIMARY_TEXT_COLOR, null)
                ?: DEFAULT_BUTTON_TEXT_COLOR,
            secondaryButtonTextColorHex = p.getString(KEY_OVERLAY_SECONDARY_TEXT_COLOR, null)
                ?: DEFAULT_BUTTON_TEXT_COLOR,
            secondaryButtonBackgroundColorHex = p.getString(KEY_OVERLAY_SECONDARY_BG, null),
            titleFontSizeSp = p.getFloat(KEY_OVERLAY_TITLE_SIZE, DEFAULT_TITLE_SIZE_SP),
            messageFontSizeSp = p.getFloat(KEY_OVERLAY_MESSAGE_SIZE, DEFAULT_MESSAGE_SIZE_SP),
            cornerRadiusDp = p.getFloat(KEY_OVERLAY_CORNER_RADIUS, DEFAULT_CORNER_RADIUS_DP),
        )
    }
}

/** Plain-data holder for the overlay styling. Mirrors the Dart `ShieldConfigurationPayload`. */
internal data class ShieldConfiguration(
    val title: String,
    val subtitle: String?,
    val primaryButtonLabel: String,
    val secondaryButtonLabel: String?,
    val primaryButtonBackgroundColorHex: String,
    val backgroundColorHex: String,
    // Android-only extras (configureAndroidOverlayUI).
    val titleColorHex: String,
    val messageColorHex: String,
    val primaryButtonTextColorHex: String,
    val secondaryButtonTextColorHex: String,
    val secondaryButtonBackgroundColorHex: String?,
    val titleFontSizeSp: Float,
    val messageFontSizeSp: Float,
    val cornerRadiusDp: Float,
)
