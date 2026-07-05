package com.faizan.flutter_screen_time

/**
 * Typed representation of the payload sent by `configureAndroidOverlayUI` from
 * Dart. Every field is nullable: an absent value means "keep the default", which
 * is applied later when the overlay is read back via
 * [ScreenTimePrefs.getShieldConfiguration].
 *
 * Mirrors the Dart `AndroidOverlayConfiguration` class.
 */
internal data class AndroidOverlayConfiguration(
    val title: String?,
    val message: String?,
    val backgroundColorHex: String?,
    val titleColorHex: String?,
    val messageColorHex: String?,
    val primaryButtonLabel: String?,
    val primaryButtonBackgroundColorHex: String?,
    val primaryButtonTextColorHex: String?,
    val secondaryButtonLabel: String?,
    val secondaryButtonBackgroundColorHex: String?,
    val secondaryButtonTextColorHex: String?,
    val titleFontSize: Float?,
    val messageFontSize: Float?,
    val cornerRadius: Float?,
) {
    companion object {
        /** Builds a configuration from the raw method-channel argument map. */
        fun fromMap(map: Map<String, Any?>): AndroidOverlayConfiguration {
            fun string(key: String) = map[key] as? String
            fun float(key: String) = (map[key] as? Number)?.toFloat()
            return AndroidOverlayConfiguration(
                title = string("title"),
                message = string("message"),
                backgroundColorHex = string("backgroundColorHex"),
                titleColorHex = string("titleColorHex"),
                messageColorHex = string("messageColorHex"),
                primaryButtonLabel = string("primaryButtonLabel"),
                primaryButtonBackgroundColorHex = string("primaryButtonBackgroundColorHex"),
                primaryButtonTextColorHex = string("primaryButtonTextColorHex"),
                secondaryButtonLabel = string("secondaryButtonLabel"),
                secondaryButtonBackgroundColorHex = string("secondaryButtonBackgroundColorHex"),
                secondaryButtonTextColorHex = string("secondaryButtonTextColorHex"),
                titleFontSize = float("titleFontSize"),
                messageFontSize = float("messageFontSize"),
                cornerRadius = float("cornerRadius"),
            )
        }
    }
}
