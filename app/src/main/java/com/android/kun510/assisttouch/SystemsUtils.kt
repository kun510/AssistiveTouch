package com.android.kun510.assisttouch

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SystemsUtils {

    fun getScreenSize(context: Context): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun shutDown(context: Context) {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        devicePolicyManager.lockNow()
    }

    fun goHome(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        context.startActivity(intent)
    }

    fun isRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() != null }
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    fun takeScreenShot(): String {
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val filename = format.format(Date())
        try {
            val sh = Runtime.getRuntime().exec("su", null, null)
            sh.outputStream.use { os ->
                os.write(("/system/bin/screencap -p /sdcard/Pictures/$filename.png").toByteArray(Charsets.US_ASCII))
                os.flush()
            }
            sh.waitFor()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return filename
    }
}
