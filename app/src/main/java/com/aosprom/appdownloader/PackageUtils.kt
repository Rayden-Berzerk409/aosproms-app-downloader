package com.aosprom.appdownloader

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

/**
 * Resultado de la query launcher: packages visibles, iconos y labels.
 * Usado por Preset Apps y Standalone APK para detectar apps instaladas y mostrar iconos.
 * labelsByPackage permite identificar apps por nombre (ej. apps con package spoofed).
 */
data class LauncherInfo(
    val packageNames: Set<String>,
    val iconsByPackage: Map<String, Drawable>,
    val labelsByPackage: Map<String, String>
)

fun getLauncherInfo(pm: PackageManager): LauncherInfo {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return try {
        val resolveList = pm.queryIntentActivities(intent, 0)
        val names = resolveList.map { it.activityInfo.packageName }.toSet()
        val icons = mutableMapOf<String, Drawable>()
        val labels = mutableMapOf<String, String>()
        for (info in resolveList) {
            val pkg = info.activityInfo.packageName
            if (!icons.containsKey(pkg)) {
                try {
                    icons[pkg] = pm.getApplicationIcon(info.activityInfo.applicationInfo)
                } catch (_: Exception) { }
            }
            if (!labels.containsKey(pkg)) {
                try {
                    labels[pkg] = pm.getApplicationLabel(info.activityInfo.applicationInfo).toString()
                } catch (_: Exception) { }
            }
        }
        LauncherInfo(names, icons, labels)
    } catch (_: Exception) {
        LauncherInfo(emptySet(), emptyMap(), emptyMap())
    }
}

/**
 * Para apps que se identifican por nombre (identifyByDisplayName): devuelve el package
 * instalado cuyo label coincide con displayName, o null si no hay ninguno.
 */
fun findPackageByDisplayName(launcherInfo: LauncherInfo, displayName: String): String? {
    return launcherInfo.labelsByPackage.entries
        .firstOrNull { (_, label) -> label.equals(displayName, ignoreCase = true) }
        ?.key
}

fun isAppInstalled(
    pm: PackageManager,
    packageName: String,
    launcherPackageNames: Set<String>
): Boolean {
    if (launcherPackageNames.contains(packageName)) return true
    return try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        pm.getLaunchIntentForPackage(packageName) != null
    }
}

fun getAppIcon(pm: PackageManager, packageName: String): Drawable? {
    return try {
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationIcon(appInfo)
    } catch (_: Exception) {
        null
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    val bitmap = if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
        Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    } else {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
