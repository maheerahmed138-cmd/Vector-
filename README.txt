VECTOR ANDROID PROJECT

This project wraps the latest Vector V8 offline HTML tracker in an Android WebView app.

BUILD FROM YOUR PHONE USING GITHUB:
1. Download and extract this ZIP.
2. Create a new GitHub repository named Vector.
3. Upload all extracted files and folders to the repository root.
   IMPORTANT: The .github folder must also be uploaded.
4. Open the repository's Actions tab.
5. Select "Build Vector APK" and tap "Run workflow".
6. Wait for the workflow to finish successfully.
7. Open the completed workflow run, scroll to Artifacts, and download "Vector-APK".
8. Extract the downloaded artifact ZIP and install app-debug.apk.

The APK is a debug APK for personal installation/testing. It is not a Play Store signed release.

DATA:
Vector stores its tracker data locally in the app's WebView storage. Export a backup from Vector > Progress regularly.
Uninstalling the app or clearing its storage can erase local data. Keep exported JSON backups.
