import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'android_overlay_configuration.dart';
import 'flutter_screen_time_method_channel.dart';

abstract class FlutterScreenTimePlatform extends PlatformInterface {
  /// Constructs a FlutterScreenTimePlatform.
  FlutterScreenTimePlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterScreenTimePlatform _instance = MethodChannelFlutterScreenTime();

  /// The default instance of [FlutterScreenTimePlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterScreenTime].
  static FlutterScreenTimePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterScreenTimePlatform] when
  /// they register themselves.
  static set instance(FlutterScreenTimePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<int> checkAuthorization() {
    throw UnimplementedError('checkAuthorization() has not been implemented.');
  }

  void getAuthorization() {
    throw UnimplementedError('getAuthorization() has not been implemented.');
  }

  void chooseApps() {
    throw UnimplementedError('chooseApps() has not been implemented.');
  }

  void blockApps() {
    throw UnimplementedError('blockApps() has not been implemented.');
  }

  void unblockApps() {
    throw UnimplementedError('unblockApps() has not been implemented.');
  }

  Future<void> setShieldConfiguration(Map<String, dynamic> configuration) {
    throw UnimplementedError('setShieldConfiguration() has not been implemented.');
  }

  Future<void> configureAndroidOverlayUI(AndroidOverlayConfiguration configuration) {
    throw UnimplementedError('configureAndroidOverlayUI() has not been implemented.');
  }
}
