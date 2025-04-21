import UIKit
import Flutter
import Photos

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
  private let channel = "com.example.fps/video"  // El mismo canal de Flutter

  override func application(
      _ application: UIApplication,
      didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    let controller = window?.rootViewController as! FlutterViewController
    let methodChannel = FlutterMethodChannel(name: channel, binaryMessenger: controller.binaryMessenger)

    methodChannel.setMethodCallHandler { (call, result) in
        if call.method == "saveVideo" {
            if let path = call.arguments as? String {
                self.saveVideoToGallery(path: path)  // Llamamos al método para guardar el video
                result("Video Saved")  // Respuesta de éxito
            } else {
                result(FlutterError(code: "INVALID_ARGUMENT", message: "Path is missing", details: nil))
            }
        } else {
            result(FlutterMethodNotImplemented)
        }
    }

    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  // Método para guardar el video en la galería de iOS
  private func saveVideoToGallery(path: String) {
    let url = URL(fileURLWithPath: path)
    
    PHPhotoLibrary.requestAuthorization { status in
        if status == .authorized {
            PHPhotoLibrary.shared().performChanges({
                PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: url)
            }) { success, error in
                if success {
                    print("Video saved to gallery")
                } else {
                    print("Error saving video: \(error?.localizedDescription ?? "Unknown error")")
                }
            }
        } else {
            print("Permission not granted to access photo library")
        }
    }
  }
}
