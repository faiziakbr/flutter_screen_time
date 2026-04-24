
import 'flutter_screen_time_platform_interface.dart';

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
}
