import 'package:flutter/services.dart';

class NativeSaver {
  static const _channel = MethodChannel('native_video_saver');

  static Future<void> saveVideo(String path) async {
    await _channel.invokeMethod('saveVideoToGallery', {'path': path});
  }
}
