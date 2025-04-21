import 'dart:async';
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'native_saver.dart';

List<CameraDescription> cameras = [];

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  cameras = await availableCameras();
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: CameraApp(),
    );
  }
}

class CameraApp extends StatefulWidget {
  @override
  _CameraAppState createState() => _CameraAppState();
}

class _CameraAppState extends State<CameraApp> {
  CameraController? controller;
  bool isRecording = false;
  Timer? _timer;
  int _recordDuration = 0;

  @override
  void initState() {
    super.initState();
    _initCamera();
  }

  Future<void> _initCamera() async {
    final newController = CameraController(
      cameras[0],
      ResolutionPreset.high,
      enableAudio: true,
    );

    try {
      await newController.initialize();
    } catch (e) {
      print("Error al inicializar cámara: $e");
      return;
    }

    if (!mounted) return;

    setState(() {
      controller = newController;
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    controller?.dispose();
    super.dispose();
  }

  void _startTimer() {
    _recordDuration = 0;
    _timer = Timer.periodic(Duration(seconds: 1), (Timer t) {
      setState(() => _recordDuration++);
    });
  }

  void _stopTimer() {
    _timer?.cancel();
  }

  Future<void> toggleRecording() async {
    if (controller == null || !controller!.value.isInitialized) return;

    if (isRecording) {
      XFile file = await controller!.stopVideoRecording();
      _stopTimer();
      setState(() => isRecording = false);
      await NativeSaver.saveVideo(file.path);
    } else {
      await controller!.startVideoRecording();
      _startTimer();
      setState(() => isRecording = true);
    }
  }

  String _formatDuration(int seconds) {
    final minutes = (seconds ~/ 60).toString().padLeft(2, '0');
    final secs = (seconds % 60).toString().padLeft(2, '0');
    return "$minutes:$secs";
  }

  @override
  Widget build(BuildContext context) {
    if (controller == null || !controller!.value.isInitialized) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      body: Stack(
        children: [
          Positioned.fill(child: CameraPreview(controller!)),

          if (isRecording)
            Positioned(
              top: 40,
              left: 20,
              child: Container(
                padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.black54,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  _formatDuration(_recordDuration),
                  style: TextStyle(
                    color: Colors.redAccent,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),

          // UI del selector de FPS (solo visual)
          Positioned(
            top: 40,
            right: 20,
            child: Container(
              padding: EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: Colors.black45,
                borderRadius: BorderRadius.circular(8),
              ),
              child: DropdownButton<int>(
                value: 30,
                icon: Icon(Icons.arrow_drop_down, color: Colors.white),
                dropdownColor: Colors.black87,
                underline: Container(),
                onChanged: null, // desactivado, solo UI
                items: [15, 24, 30, 60].map((fps) {
                  return DropdownMenuItem<int>(
                    value: fps,
                    child: Text("$fps FPS", style: TextStyle(color: Colors.white)),
                  );
                }).toList(),
              ),
            ),
          ),

          Positioned(
            bottom: 40,
            left: 0,
            right: 0,
            child: Center(
              child: GestureDetector(
                onTap: toggleRecording,
                child: AnimatedContainer(
                  duration: Duration(milliseconds: 300),
                  width: 70,
                  height: 70,
                  decoration: BoxDecoration(
                    color: isRecording ? Colors.red : Colors.white,
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.2),
                        blurRadius: 10,
                        offset: Offset(0, 5),
                      ),
                    ],
                    border: Border.all(
                      color: Colors.grey.shade800,
                      width: 2,
                    ),
                  ),
                  child: Center(
                    child: Icon(
                      isRecording ? Icons.stop : Icons.fiber_manual_record,
                      color: isRecording ? Colors.white : Colors.red,
                      size: 30,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
