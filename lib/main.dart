import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'pages/home_page.dart';


void main() async {
  WidgetsFlutterBinding.ensureInitialized(); // Necesario para usar la cámara
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Camera App',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const HomePage(), // Aquí cargas tu pantalla de cámara
      debugShowCheckedModeBanner: false,
    );
  }
}
