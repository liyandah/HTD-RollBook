# HT-E ChatBot Android App

Native Android WebView app that connects to the live Salvation Army chat at `http://4.189.69.11/chat`.

## Build APK

```bat
cd mobile-android
gradlew.bat assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Install on phone

1. Copy `app-debug.apk` to your Android device
2. Enable "Install unknown apps" for your file manager
3. Open the APK and install
4. Launch **HT-E ChatBot**

## Live server

- Chat UI: `http://4.189.69.11/chat`
- API: `http://4.189.69.11/api/bot/message`

Change the URL in `app/src/main/java/org/salvationarmy/hte/chat/MainActivity.java` if the server IP changes.
