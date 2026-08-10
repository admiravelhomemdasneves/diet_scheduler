import 'package:google_sign_in/google_sign_in.dart';
import '../config.dart';

/// Wraps Google Sign-In requesting only basic profile/email scope (per M0 scope).
class AuthService {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    scopes: ['email'],
    serverClientId: googleServerClientId,
  );

  /// Returns the Google ID token to exchange with the backend, or null if the user cancelled.
  Future<String?> signInAndGetIdToken() async {
    final account = await _googleSignIn.signIn();
    if (account == null) {
      return null;
    }
    final auth = await account.authentication;
    return auth.idToken;
  }

  Future<void> signOut() => _googleSignIn.signOut();
}
