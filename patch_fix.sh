sed -i 's/<\/manifest>/    <uses-permission android:name="android.permission.CAMERA" \/>\n<\/manifest>/g' app/src/main/AndroidManifest.xml
sed -i '/fun fetchOverlayApps() {/i \
    init {\
        fetchOverlayApps()\
    }\
' app/src/main/java/com/example/secscanner/MainViewModel.kt
