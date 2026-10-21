package vn.dsai.mockgps

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

object Permissions {
    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun hasLocation(ctx: Context) =
        granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION) || granted(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasNotifications(ctx: Context) =
        Build.VERSION.SDK_INT < 33 || granted(ctx, Manifest.permission.POST_NOTIFICATIONS)

    fun canOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    /** True once this app is chosen under Developer options → "Select mock location app". */
    fun isMockApp(ctx: Context): Boolean = runCatching {
        val aom = ctx.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            aom.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            aom.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), ctx.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun runtimeRequests(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
}
