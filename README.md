![Android](https://img.shields.io/badge/Platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange)
![License](https://img.shields.io/badge/License-MIT-blue)
[![GitHub Stars](https://img.shields.io/github/stars/Rayden-Berzerk409/aosproms-app-downloader)](https://github.com/Rayden-Berzerk409/aosproms-app-downloader/stargazers)

# 📦 App Downloader for AOSP ROMs Users

A simple Android app designed to quickly reinstall your essential apps after flashing or reinstalling AOSP-based custom ROMs — without manually browsing the Google Play Store or searching for APKs one by one. This app automates the tedious process of reinstalling your favorite applications after a clean flash or a /data partition format, eliminating the need to manually search the Play Store or hunt for APKs in Chrome.

Built with ❤️ using Cursor AI and Google Antigravity (Gemini 3 Pro).

# 🚀 Why This App Exists

If you frequently flash custom ROMs (especially AOSP-based ones), you already know the struggle:

- 🔄 Format /data

- 📱 Boot fresh system

- 🛍️ Open Play Store

- 🔍 Search each app manually

- ⏳ Repeat... again... and again...

Or worse:

- 🌐 Open Chrome

- 🔎 Search for APKs

- 📥 Download

- 📂 Install manually

This app solves that.

It gives you a centralized app list so you can reinstall your commonly used applications quickly and efficiently after a clean flash.

# ✨ Features

- 📋 Predefined list of commonly used apps

- 🛒 Direct Play Store redirection for store apps

- 📦 Direct APK download support for apps distributed outside the Play Store

- 🔧 Easily customizable app list

- ⚡ Lightweight and minimal

- 🎯 Designed specifically for AOSP custom ROM users

# 🛠️ Built With

- Kotlin

- Android Studio

- Cursor AI

- Google Antigravity (Gemini 3 Pro)

# 👨‍💻 How to Customize the App With Your Own Apps

The main goal of this project is to be a template. If you want to create your own version of the app with your specific list of apps, follow these steps:

## 1️⃣ Clone the Repository
```
git clone https://github.com/Rayden-Berzerk409/aosproms-app-downloader.git
```
Open the project in Android Studio.

## 2️⃣ Edit Only These Two Files

You must modify:

`AppEntry.kt`

`ApkAppEntry.kt`

**⚠️ Important:
Only delete or replace the apps inside your cloned repository. Do not modify the original repository unless you intend to contribute.**

### 📝 Editing AppEntry.kt

This file is for apps that are available on the Google Play Store.

Use this structure:

```
AppEntry("[package name]", "[display name]"),
```

Example:
```
AppEntry("com.twofasapp", "2FAS Auth"),
```

### 📦 Editing ApkAppEntry.kt

This file is for apps that are installed via APK (not from Play Store).

Use this structure:
```
ApkAppEntry(
    packageName = "[package name]",
    displayName = "[display name]",
    downloadUrl = "[direct APK download URL]",
    githubRepo = "[owner/repository-name]"
),
```
Example:
```
ApkAppEntry(
    packageName = "app.lawnchair",
    displayName = "Lawnchair",
    downloadUrl = "https://github.com/LawnchairLauncher/lawnchair/releases/download/vX.X.X/app-release.apk",
    githubRepo = "LawnchairLauncher/lawnchair"
),
```

### 🧪 3️⃣ Build and Test

After editing only those two files:

- Sync the project (Sync Project with Gradle Files)

- Build and test the APK in Android Studio

- If works correctly and as you expect, build the APK and install it on your device

- Test that downloads and redirects work correctly

That’s it 🎉

# ⚠️ Disclaimer

- This app **does not** host APK files.

- All APK download links must point to official or trusted sources.

- The developer is not responsible for third-party content.

- Always verify APK sources before installation.

# 🎯 Target Users

- AOSP custom ROM users

- Users who frequently flash or switch ROMs

- Power users who want faster post-flash setup

- Anyone who wants a simple personal app installer list

# 🤝 Contributions

Pull requests are welcome!

If you’d like to improve UI, performance, or add features — feel free to fork and contribute.

# ⭐ Support the Project

If this project helps you save time after flashing ROMs:

- ⭐ Star the repository

- 🛠️ Fork it and customize it

- 🤝 Share it with your custom ROM community
