import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_screen_time_api/flutter_screen_time.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _flutterScreenTimePlugin = FlutterScreenTime();
  int authValue = 0;

  @override
  void initState() {
    super.initState();
  }

  Future<int> checkAuthorization() async {
    return await _flutterScreenTimePlugin.checkAuthorization();
  }

  void getAuthorization() {
    _flutterScreenTimePlugin.getAuthorization();
  }

  void chooseApps() {
    _flutterScreenTimePlugin.chooseApps();
  }

  Future<void> blockApps() async {
    await _flutterScreenTimePlugin.setShieldConfiguration(
      const ShieldConfigurationPayload(
        title: "Focus Time Enabled",
        subtitle: "This app is blocked during your focus session.",
        primaryButtonLabel: "Got it",
        // secondaryButtonLabel: "Close",
        primaryButtonBackgroundColorHex: "#2563EB",
        backgroundColorHex: "#111827CC",
        backgroundBlurStyle: ShieldBackgroundBlurStyle.systemMaterialDark,
      ),
    );
    _flutterScreenTimePlugin.blockApps();
  }

  void unblockApps() {
    _flutterScreenTimePlugin.unblockApps();
  }

  void configureAndroidOverlay() async {
    await _flutterScreenTimePlugin.configureAndroidOverlayUI(
      AndroidOverlayConfiguration(title: "App Blocked", secondaryButtonLabel: "Secondary button"),
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Flutter screen time API')),
        body: SizedBox(
          width: double.infinity,
          child: Column(
            spacing: 12,
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                authValue == 1
                    ? "Permission granted"
                    : authValue == 0
                    ? "Not determined"
                    : "Permission denied",
              ),
              TextButton(
                onPressed: () async {
                  authValue = await checkAuthorization();
                  setState(() {});
                },
                child: Text("Check Authorization"),
              ),
              TextButton(
                onPressed: () {
                  getAuthorization();
                },
                child: Text("Get Authorization"),
              ),
              TextButton(
                onPressed: () {
                  chooseApps();
                },
                child: Text("Choose Apps"),
              ),
              TextButton(
                onPressed: () async {
                  await blockApps();
                },
                child: Text("Block Apps"),
              ),
              TextButton(
                onPressed: () {
                  unblockApps();
                },
                child: Text("Unblock Apps"),
              ),
              TextButton(
                onPressed: () {
                  configureAndroidOverlay();
                },
                child: Text("Configure Android Overlay"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
