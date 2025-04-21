# fps_camera

FPS CAMERA to interact with FAN to modify FPS

## Packages
https://pub.dev/packages/
```bash
camera: ^0.11.1
path_provider: ^2.1.5

```

## MainActivity.kt
MainActivity.kt, you define the channel and the native code that runs when Flutter calls a method, like saving a file to the gallery.
```bash
android/app/src/main/kotlin/com/example/video_recorder_app/MainActivity.kt
com.example.fps/video
```
## Method Channel
MethodChannel is used to communicate between Flutter and native code (Android/iOS). For example, from Dart you can say "save this video" and Android (Kotlin) will do it.

## Add some permissions

### Android
Path: android/app/src/main/AndroidManifest.xml

```bash

 <!-- Permissions -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Requisitos de hardware -->
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.autofocus" />

```

#### Application
```bash

android:requestLegacyExternalStorage="true"

```


# Package Modify according to IOS System or Android
```bash
IOS --> 
Info.plist

Android -->
AndroidManifest.xml
```