# flutter_screen_time

Flutter Screen Time is an **iOS-only** project.

![Demo](./readme_images/demo.gif)

## Features

- Get permission status
- Select apps to block
- Block/unblock apps

## Installation

Add the plugin to your `pubspec.yaml` file:

```yaml
dependencies:
  flutter_screen_time: ^0.0.1
```

## Prerequisite

Before running this project, make sure your Xcode target has the **Family Controls** capability enabled.

![FlutterScreenTimeScreenshot](./readme_images/image1.png)

## Setup

### iOS

### Create Object

```dart
final _flutterScreenTimePlugin = FlutterScreenTime();
```

#### 1. Permission Information

```dart
Future<int> checkAuthorization() async {
  return await _flutterScreenTimePlugin.checkAuthorization();
}
```

```
Statuses: 
-1 -> Permission denied
1 -> Permission granted
0 -? Not determined
```

#### 2. Get Permission

```dart
void getAuthorization() {
  _flutterScreenTimePlugin.getAuthorization();
}
```

#### 3. Choose Apps to block

```dart
void chooseApps() {
  _flutterScreenTimePlugin.chooseApps();
}
```

#### 4. Block apps

```dart
void blockApps() {
  _flutterScreenTimePlugin.blockApps();
}
```

#### 5. Unblock apps

```dart
void unblockApps() {
  _flutterScreenTimePlugin.unblockApps();
}
```

