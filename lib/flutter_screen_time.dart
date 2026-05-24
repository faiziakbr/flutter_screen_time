
import 'flutter_screen_time_platform_interface.dart';

enum ShieldBackgroundBlurStyle {
  systemThinMaterial,
  systemMaterial,
  systemChromeMaterial,
  systemUltraThinMaterialDark,
  systemThinMaterialDark,
  systemMaterialDark,
  systemChromeMaterialDark,
}

extension ShieldBackgroundBlurStyleX on ShieldBackgroundBlurStyle {
  String get nativeValue {
    switch (this) {
      case ShieldBackgroundBlurStyle.systemThinMaterial:
        return "systemThinMaterial";
      case ShieldBackgroundBlurStyle.systemMaterial:
        return "systemMaterial";
      case ShieldBackgroundBlurStyle.systemChromeMaterial:
        return "systemChromeMaterial";
      case ShieldBackgroundBlurStyle.systemUltraThinMaterialDark:
        return "systemUltraThinMaterialDark";
      case ShieldBackgroundBlurStyle.systemThinMaterialDark:
        return "systemThinMaterialDark";
      case ShieldBackgroundBlurStyle.systemMaterialDark:
        return "systemMaterialDark";
      case ShieldBackgroundBlurStyle.systemChromeMaterialDark:
        return "systemChromeMaterialDark";
    }
  }
}

class ShieldConfigurationPayload {
  final String title;
  final String? subtitle;
  final String? primaryButtonLabel;
  final String? secondaryButtonLabel;
  final String? primaryButtonBackgroundColorHex;
  final String? backgroundColorHex;
  final ShieldBackgroundBlurStyle? backgroundBlurStyle;

  const ShieldConfigurationPayload({
    required this.title,
    this.subtitle,
    this.primaryButtonLabel,
    this.secondaryButtonLabel,
    this.primaryButtonBackgroundColorHex,
    this.backgroundColorHex,
    this.backgroundBlurStyle,
  });

  Map<String, dynamic> toMap() {
    return {
      "title": title,
      "subtitle": subtitle,
      "primaryButtonLabel": primaryButtonLabel,
      "secondaryButtonLabel": secondaryButtonLabel,
      "primaryButtonBackgroundColorHex": primaryButtonBackgroundColorHex,
      "backgroundColorHex": backgroundColorHex,
      "backgroundBlurStyle": backgroundBlurStyle?.nativeValue,
    };
  }
}

class FlutterScreenTime {

  Future<int> checkAuthorization() async {
    return await FlutterScreenTimePlatform.instance.checkAuthorization();
  }

  void getAuthorization() {
    FlutterScreenTimePlatform.instance.getAuthorization();
  }

  void chooseApps() {
    FlutterScreenTimePlatform.instance.chooseApps();
  }

  void blockApps() {
    FlutterScreenTimePlatform.instance.blockApps();
  }

  void unblockApps() {
    FlutterScreenTimePlatform.instance.unblockApps();
  }

  Future<void> setShieldConfiguration(ShieldConfigurationPayload configuration) async {
    await FlutterScreenTimePlatform.instance.setShieldConfiguration(configuration.toMap());
  }
}
