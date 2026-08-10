/// The "server client ID" (a Web application OAuth client, not the Android one) that
/// google_sign_in needs to actually return an ID token our backend can verify. See
/// https://pub.dev/packages/google_sign_in#id-token -- the Android client only
/// authorizes the app by package name + signing certificate; the ID token audience
/// is always this server/web client.
const String googleServerClientId =
    '337611843888-aj9o1a0s9ls2bue3jlqcq9pcd5kqqnua.apps.googleusercontent.com';

class ApiConfig {
  /// Single source of truth for where the backend lives, set at build/run time via
  /// `--dart-define=DS_API_ORIGIN=http://<host>:8080` (see mobile/dart_define/*.json).
  /// Defaults to the Android emulator's host-loopback alias, so `flutter run` with no
  /// flags at all still works exactly as before for emulator development. A physical
  /// device needs the dev machine's real LAN IP instead -- see dart_define/lan.example.json.
  static const String _origin = String.fromEnvironment(
    'DS_API_ORIGIN',
    defaultValue: 'http://10.0.2.2:8080',
  );

  static String get baseHttpUrl => _origin;

  static String get baseWsUrl =>
      _origin.replaceFirst('https://', 'wss://').replaceFirst('http://', 'ws://');

  /// External thumbnails (e.g. an imported recipe's source image) are already absolute URLs;
  /// our own uploaded recipe photos come back as a relative /recipe-images/... path.
  static String? resolveImageUrl(String? imageUrl) {
    if (imageUrl == null) return null;
    if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) return imageUrl;
    return '$baseHttpUrl$imageUrl';
  }
}
