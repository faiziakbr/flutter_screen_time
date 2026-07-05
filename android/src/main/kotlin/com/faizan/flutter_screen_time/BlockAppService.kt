package com.faizan.flutter_screen_time

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Foreground service that enforces app blocking on Android.
 *
 * Android has no first-party Screen Time API, so we poll [UsageStatsManager]
 * for the most recent foreground app and, when it matches the user's blocked
 * set, draw a full-screen "shield" overlay on top of it — the Android analog of
 * the iOS ManagedSettings shield.
 */
class BlockAppService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    private var overlayView: View? = null
    private var isOverlayDisplayed = false

    private val overlayParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
    }

    private val pollingRunnable = object : Runnable {
        override fun run() {
            if (!ScreenTimePrefs.isBlockingEnabled(this@BlockAppService)) {
                Log.d(TAG, "Blocking disabled; stopping service")
                removeOverlay()
                stopSelf()
                return
            }

            if (!hasOverlayPermission() || !hasUsageStatsPermission()) {
                Log.w(TAG, "Missing permissions (overlay/usage); retrying")
                removeOverlay()
                handler.postDelayed(this, 1000)
                return
            }

            if (!isDeviceLocked() && isBlockedPackageInForeground()) {
                showOverlay()
            } else {
                removeOverlay()
            }

            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        handler.removeCallbacksAndMessages(null)
        handler.post(pollingRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        super.onDestroy()
    }

    // region Foreground notification

    private fun startAsForeground() {
        val channelId = "flutter_screen_time.blocking"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen time blocking",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("Screen time blocking")
            .setContentText("App blocking is active.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // endregion

    // region Overlay

    private fun showOverlay() {
        val view = overlayView ?: buildShieldView().also { overlayView = it }
        if (!isOverlayDisplayed && view.windowToken == null) {
            try {
                windowManager.addView(view, overlayParams)
                isOverlayDisplayed = true
                Log.d(TAG, "Shield shown")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add overlay — is 'Display over other apps' granted?", e)
            }
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        if (isOverlayDisplayed && view.windowToken != null) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay", e)
            }
            isOverlayDisplayed = false
        }
    }

    private fun buildShieldView(): View {
        val config = ScreenTimePrefs.getShieldConfiguration(this)
        val backgroundColor = ColorUtils.parse(config.backgroundColorHex, Color.parseColor("#111827"))
        val primaryColor = ColorUtils.parse(config.primaryButtonBackgroundColorHex, Color.parseColor("#2563EB"))
        val titleColor = ColorUtils.parse(config.titleColorHex, Color.WHITE)
        val messageColor = ColorUtils.parse(config.messageColorHex, Color.parseColor("#D1D5DB"))
        val primaryTextColor = ColorUtils.parse(config.primaryButtonTextColorHex, Color.WHITE)
        val secondaryTextColor = ColorUtils.parse(config.secondaryButtonTextColorHex, Color.WHITE)
        val cornerRadiusPx = dp(config.cornerRadiusDp).toFloat()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(backgroundColor)
            setPadding(dp(32), dp(32), dp(32), dp(32))
            isClickable = true
            isFocusable = true
        }

        root.addView(TextView(this).apply {
            text = config.title
            setTextColor(titleColor)
            textSize = config.titleFontSizeSp
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        })

        config.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
            root.addView(TextView(this).apply {
                text = subtitle
                setTextColor(messageColor)
                textSize = config.messageFontSizeSp
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(12) }
            })
        }

        root.addView(Button(this).apply {
            text = config.primaryButtonLabel
            setTextColor(primaryTextColor)
            background = GradientDrawable().apply {
                cornerRadius = cornerRadiusPx
                setColor(primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(28) }
            setOnClickListener { goHome() }
        })

        config.secondaryButtonLabel?.takeIf { it.isNotBlank() }?.let { label ->
            root.addView(Button(this).apply {
                text = label
                setTextColor(secondaryTextColor)
                background = GradientDrawable().apply {
                    cornerRadius = cornerRadiusPx
                    // Filled when a background colour is supplied, otherwise an
                    // outlined button that borrows the text colour for its stroke.
                    val secondaryBg = config.secondaryButtonBackgroundColorHex
                    if (secondaryBg.isNullOrBlank()) {
                        setStroke(dp(1), secondaryTextColor)
                        setColor(Color.TRANSPARENT)
                    } else {
                        setColor(ColorUtils.parse(secondaryBg, Color.TRANSPARENT))
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(12) }
                setOnClickListener { goHome() }
            })
        }

        return root
    }

    /** Sends the user back to the launcher and drops the shield. */
    private fun goHome() {
        removeOverlay()
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // endregion

    // region Foreground detection

    private fun isBlockedPackageInForeground(): Boolean {
        val foreground = lastForegroundPackage() ?: return false
        if (foreground == packageName) return false
        return ScreenTimePrefs.getBlockedPackages(this).contains(foreground)
    }

    private fun lastForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 60_000

        val events = usageStatsManager.queryEvents(beginTime, endTime)
        var lastPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }

    // endregion

    // region Permission helpers

    private fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isDeviceLocked(): Boolean {
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguard.isKeyguardLocked
    }

    // endregion

    private fun dp(value: Int): Int = dp(value.toFloat())

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics,
        ).toInt()

    private companion object {
        const val TAG = "ScreenTimeBlocker"
        const val NOTIFICATION_ID = 1
        const val POLL_INTERVAL_MS = 500L
    }
}
