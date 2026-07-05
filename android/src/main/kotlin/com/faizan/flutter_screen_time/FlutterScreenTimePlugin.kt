package com.faizan.flutter_screen_time

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener

/**
 * Android implementation of the screen-time plugin.
 *
 * Android has no first-party Screen Time / FamilyControls API, so app blocking
 * is built from two things the user must grant:
 *   1. "Display over other apps" (overlay) permission — to draw the shield.
 *   2. "Usage access" permission — to detect which app is in the foreground.
 *
 * The [BlockAppService] does the actual detection + shielding; this plugin wires
 * the Flutter method channel to permissions, the app picker, and the blocking
 * on/off flag.
 */
class FlutterScreenTimePlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware,
    ActivityResultListener {

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null
    private var binding: ActivityPluginBinding? = null

    private companion object {
        const val TAG = "ScreenTimeBlocker"
        const val REQ_OVERLAY = 8001
        const val REQ_USAGE = 8002
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_screen_time")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> result.success("Android ${Build.VERSION.RELEASE}")

            "checkAuthorization" -> result.success(checkAuthorization())

            "authorize" -> {
                requestAuthorization()
                result.success(true)
            }

            "chooseApps" -> {
                chooseApps()
                result.success(true)
            }

            "blockApps" -> {
                blockApps()
                result.success(true)
            }

            "unblockApps" -> {
                unblockApps()
                result.success(true)
            }

            "setShieldConfiguration" -> {
                @Suppress("UNCHECKED_CAST")
                val arguments = call.arguments as? Map<String, Any?>
                if (arguments == null) {
                    result.error("INVALID_ARGUMENTS", "Expected a configuration object.", null)
                    return
                }
                ScreenTimePrefs.saveShieldConfiguration(context, arguments)
                result.success(true)
            }

            "configureAndroidOverlayUI" -> {
                @Suppress("UNCHECKED_CAST")
                val arguments = call.arguments as? Map<String, Any?>
                if (arguments == null) {
                    result.error("INVALID_ARGUMENTS", "Expected a configuration object.", null)
                    return
                }
                val config = AndroidOverlayConfiguration.fromMap(arguments)
                ScreenTimePrefs.saveAndroidOverlayConfiguration(context, config)
                result.success(true)
            }

            else -> result.notImplemented()
        }
    }

    /**
     * Returns 1 when both the overlay permission and usage-access permission are
     * granted, otherwise 0 — mirroring the iOS `approved` / `notDetermined`
     * semantics that the example UI expects.
     */
    private fun checkAuthorization(): Int {
        val overlayGranted = Settings.canDrawOverlays(context)
        val usageGranted = hasUsageStatsPermission()
        Log.d(TAG, "checkAuthorization: overlay=$overlayGranted usage=$usageGranted")
        return if (overlayGranted && usageGranted) 1 else 0
    }

    /**
     * Opens the missing special-access screens one at a time. Overlay first;
     * when the user returns, usage access (if still needed). They cannot be
     * requested together — each is a separate full-screen Settings page and
     * neither goes through the runtime permission dialog.
     */
    private fun requestAuthorization() {
        when {
            !Settings.canDrawOverlays(context) -> requestOverlay()
            !hasUsageStatsPermission() -> requestUsageAccess()
            else -> Log.d(TAG, "requestAuthorization: both permissions already granted")
        }
    }

    private fun requestOverlay() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        activity?.startActivityForResult(intent, REQ_OVERLAY)
    }

    private fun requestUsageAccess() {
        // ACTION_USAGE_ACCESS_SETTINGS doesn't reliably honour a package: Uri
        // across OEMs, so it lands on the app list — that's expected.
        activity?.startActivityForResult(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            REQ_USAGE,
        )
    }

    /**
     * These Settings screens return RESULT_CANCELED even on success, so the
     * resultCode is ignored — the actual permission state is re-checked instead.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (requestCode) {
            REQ_OVERLAY -> {
                if (!hasUsageStatsPermission()) requestUsageAccess()
                return true
            }
            REQ_USAGE -> return true
        }
        return false
    }

    private fun chooseApps() {
        startExternalActivity(Intent(context, AppPickerActivity::class.java))
    }

    /** Turns blocking on and starts the foreground service that enforces it. */
    private fun blockApps() {
        ScreenTimePrefs.setBlockingEnabled(context, true)
        Log.d(TAG, "blockApps: packages=${ScreenTimePrefs.getBlockedPackages(context)}")

        val intent = Intent(context, BlockAppService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /** Turns blocking off and stops the enforcing service. */
    private fun unblockApps() {
        ScreenTimePrefs.setBlockingEnabled(context, false)
        context.stopService(Intent(context, BlockAppService::class.java))
        Log.d(TAG, "unblockApps")
    }

    private fun startExternalActivity(intent: Intent) {
        val launcher = activity
        if (launcher != null) {
            launcher.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        attachActivity(binding)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        attachActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachActivity()
    }

    override fun onDetachedFromActivity() {
        detachActivity()
    }

    private fun attachActivity(binding: ActivityPluginBinding) {
        this.binding = binding
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    private fun detachActivity() {
        binding?.removeActivityResultListener(this)
        binding = null
        activity = null
    }
}
