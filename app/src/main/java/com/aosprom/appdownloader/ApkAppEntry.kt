package com.aosprom.appdownloader

/**
 * Una app instalable por APK (fuera de Play Store).
 * @param packageName ID del paquete (ej: org.schabi.newpipe). Si la app usa spoofing y cambia de package en cada instalación, usa un nombre fijo solo para el archivo de descarga (ej: "kernelsu.next") y pon identifyByDisplayName = true.
 * @param displayName Nombre para mostrar (y, si identifyByDisplayName, nombre con el que se detecta si está instalada).
 * @param downloadUrl URL directa del APK (GitHub releases, etc.)
 * @param identifyByDisplayName Si true, se detecta si está instalada por el nombre de la app (label) en lugar del package. Útil para apps con package spoofed que cambia en cada instalación.
 */
data class ApkAppEntry(
    val packageName: String,
    val displayName: String,
    val downloadUrl: String,
    val identifyByDisplayName: Boolean = false,
    val githubRepo: String? = null // Format: "owner/repo"
)

/**
 * Lista de apps instalables por APK.
 * Añade aquí tus apps: la mayoría pueden ser enlaces a GitHub Releases u otras URLs directas.
 */
object ApkApps {
    val list = listOf(
        ApkAppEntry(
            packageName = "app.revanced.android.gms",
            displayName = "Servicios de microG",
            downloadUrl = "https://github.com/ReVanced/GmsCore/releases/download/v0.3.1.4.240913/app.revanced.android.gms-240913008-signed.apk",
            githubRepo = "ReVanced/GmsCore"
        ),
        ApkAppEntry(
            packageName = "org.frknkrc44.hma_oss",
            displayName = "HMA-OSS",
            downloadUrl = "https://github.com/frknkrc44/HMA-OSS/releases/download/oss-154/HMA-OSS-oss-154-release.apk",
            githubRepo = "frknkrc44/HMA-OSS"
        ),
        ApkAppEntry(
            packageName = "kernelsu.next",
            displayName = "KernelSU Next",
            downloadUrl = "https://github.com/KernelSU-Next/KernelSU-Next/releases/download/v1.0.3/KernelSU_Next_v1.0.3-spoofed_32967-release.apk",
            identifyByDisplayName = true,
            githubRepo = "KernelSU-Next/KernelSU-Next"
        ),
        ApkAppEntry(
            packageName = "app.lawnchair",
            displayName = "Lawnchair",
            downloadUrl = "https://github.com/LawnchairLauncher/lawnchair/releases/download/v14.0.0-beta2/Lawnchair.14.0.0.Beta.2.apk",
            githubRepo = "LawnchairLauncher/lawnchair"
        ),
        ApkAppEntry(
            packageName = "app.lawnchair.lawnicons",
            displayName = "Lawnicons",
            downloadUrl = "https://github.com/LawnchairLauncher/lawnicons/releases/download/v2.10.0/Lawnicons.2.10.0.apk",
            githubRepo = "LawnchairLauncher/lawnicons"
        ),
        ApkAppEntry(
            packageName = "com.reveny.nativecheck",
            displayName = "Native Detector",
            downloadUrl = "https://github.com/reveny/Android-Native-Root-Detector/releases/download/v1.2.0/native-root-detector-v1.2.0.apk",
            githubRepo = "reveny/Android-Native-Root-Detector"
        ),
        ApkAppEntry(
            packageName = "com.theveloper.pixelplay",
            displayName = "PixelPlayer",
            downloadUrl = "https://github.com/theovilardo/PixelPlayer/releases/download/0.5.0-beta/PixelPlayer-0.5.0-beta.apk",
            githubRepo = "theovilardo/PixelPlayer"
        ),
        ApkAppEntry(
            packageName = "app.revanced.manager.flutter",
            displayName = "ReVanced Manager",
            downloadUrl = "https://github.com/ReVanced/revanced-manager/releases/download/v1.23.0/revanced-manager-1.23.0.apk",
            githubRepo = "ReVanced/revanced-manager"
        ),
        ApkAppEntry(
            packageName = "com.aurora.store",
            displayName = "Aurora Store",
            downloadUrl = "https://auroraoss.com/downloads/AuroraStore/Release/AuroraStore-4.5.1.apk",
            // Aurora Store isn't strictly a GitHub release in the same direct way sometimes, but they have a repo.
            // Keeping it null if uncertain or using direct GitLab/Website.
            githubRepo = "AuroraOSS/AuroraStore" 
        ),
    )
}
