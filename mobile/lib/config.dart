import 'dart:io' show Platform;

/// Dev-only base URLs. The Android emulator reaches the host machine via the
/// special alias 10.0.2.2; the iOS simulator can use localhost directly.
/// A physical device needs the host machine's real LAN IP instead.
/// The "server client ID" (a Web application OAuth client, not the Android one) that
/// google_sign_in needs to actually return an ID token our backend can verify. See
/// https://pub.dev/packages/google_sign_in#id-token -- the Android client only
/// authorizes the app by package name + signing certificate; the ID token audience
/// is always this server/web client.
const String googleServerClientId =
    '337611843888-aj9o1a0s9ls2bue3jlqcq9pcd5kqqnua.apps.googleusercontent.com';

class ApiConfig {
  static String get baseHttpUrl {
    if (Platform.isAndroid) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static String get baseWsUrl {
    if (Platform.isAndroid) {
      return 'ws://10.0.2.2:8080';
    }
    return 'ws://localhost:8080';
  }

  /// External thumbnails (e.g. an imported recipe's source image) are already absolute URLs;
  /// our own uploaded recipe photos come back as a relative /recipe-images/... path.
  static String? resolveImageUrl(String? imageUrl) {
    if (imageUrl == null) return null;
    if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) return imageUrl;
    return '$baseHttpUrl$imageUrl';
  }
}
