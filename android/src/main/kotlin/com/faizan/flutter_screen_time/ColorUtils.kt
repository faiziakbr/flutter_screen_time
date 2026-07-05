package com.faizan.flutter_screen_time

import android.graphics.Color

/**
 * Parses the hex colours coming from Dart. The Dart/iOS side uses `#RRGGBB` and
 * `#RRGGBBAA` (alpha last), whereas Android's [Color.parseColor] expects
 * `#RRGGBB` or `#AARRGGBB` (alpha first). This normalises both forms.
 */
internal object ColorUtils {
    fun parse(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        val cleaned = hex.trim().removePrefix("#")
        return try {
            when (cleaned.length) {
                3 -> {
                    // #RGB -> #RRGGBB
                    val r = cleaned[0]
                    val g = cleaned[1]
                    val b = cleaned[2]
                    Color.parseColor("#$r$r$g$g$b$b")
                }
                6 -> Color.parseColor("#$cleaned")
                8 -> {
                    // Incoming is RRGGBBAA (alpha last); Android wants AARRGGBB.
                    val rrggbb = cleaned.substring(0, 6)
                    val aa = cleaned.substring(6, 8)
                    Color.parseColor("#$aa$rrggbb")
                }
                else -> fallback
            }
        } catch (e: IllegalArgumentException) {
            fallback
        }
    }
}
