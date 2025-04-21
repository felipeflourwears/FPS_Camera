import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:gal/gal.dart'; // Importa el paquete gal
import 'package:path_provider/path_provider.dart';  // Para obtener la ruta donde guardar el video
import 'dart:io';  // Para trabajar con archivos
import 'package:permission_handler/permission_handler.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with WidgetsBindingObserver {
  List<CameraDescription> cameras = [];
  CameraController? cameraController;
  bool isRecording = false;

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    if (cameraController == null || cameraController?.value.isInitialized == false) {
      return;
    }

    if (state == AppLifecycleState.inactive) {
      cameraController?.dispose();
    } else if (state == AppLifecycleState.resumed) {
      _setupCameraController();
    }
  }

  @override
  void initState() {
    super.initState();
    _setupCameraController();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _buildUI(),
    );
  }

  Widget _buildUI() {
    if (cameraController == null || cameraController?.value.isInitialized == false) {
      return const Center(child: CircularProgressIndicator());
    }
    return SafeArea(
      child: SizedBox.expand(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            SizedBox(
              height: MediaQuery.sizeOf(context).height * 0.30,
              width: MediaQuery.sizeOf(context).width * 0.80,
              child: CameraPreview(cameraController!),
            ),
            IconButton(
              onPressed: () async {
                if (isRecording) {
                  // Detener la grabación
                  await stopRecording();
                } else {
                  // Iniciar la grabación
                  await startRecording();
                }
              },
              iconSize: 100,
              icon: Icon(
                isRecording ? Icons.stop : Icons.videocam,
                color: Colors.red,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _setupCameraController() async {
    List<CameraDescription> _cameras = await availableCameras();
    if (_cameras.isNotEmpty) {
      setState(() {
        cameras = _cameras;
        cameraController = CameraController(_cameras.first, ResolutionPreset.high);
      });
      cameraController?.initialize().then((_) {
        if (!mounted) {
          return;
        }
        setState(() {});
      });
    }
  }

  Future<void> startRecording() async {
    if (!cameraController!.value.isInitialized) {
      return;
    }

    final directory = await getApplicationDocumentsDirectory();  // Obtener el directorio de documentos
    final videoPath = '${directory.path}/video_${DateTime.now().millisecondsSinceEpoch}.mp4';

    try {
      await cameraController!.startVideoRecording();
      setState(() {
        isRecording = true;
      });
    } catch (e) {
      print("Error al iniciar grabación: $e");
    }
  }

 Future<void> stopRecording() async {
  if (!cameraController!.value.isInitialized || !isRecording) {
    return;
  }

  try {
    XFile videoFile = await cameraController!.stopVideoRecording();
    setState(() {
      isRecording = false;
    });

    // Pedir permiso para acceder a almacenamiento
    final videosPermission = await Permission.videos.request();
    final storagePermission = await Permission.storage.request();

    if (videosPermission.isGranted || storagePermission.isGranted) {
    await Gal.putVideo(videoFile.path);
    } else {
    print("❌ Permisos no concedidos");
    }

  } catch (e) {
    print("Error al detener la grabación: $e");
  }
}
}
