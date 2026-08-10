import 'package:flutter/material.dart';

/// Success/info/warning colors, parallel to Material 3's error family but not part of
/// ColorScheme itself -- exposed as a ThemeExtension instead. Replaces the app's previous
/// ad-hoc mix of Colors.green/blue/orange (three different oranges, inconsistently) with one
/// set derived the same way as AppColorSchemes: CIE LCh(ab), hue/chroma held constant per
/// family while lightness is swept per tonal role, gamut-clamped, WCAG-verified (every
/// base/onBase and container/onContainer pair here is >=6.4:1, well past the 4.5:1 AA floor).
///
/// Hues were chosen for separation from both each other and from AppColorSchemes' primary/
/// secondary/tertiary/error hues, so a user can tell them apart by hue alone at small sizes
/// (icons, thin borders) -- not just lightness/saturation. `warning` in particular was moved
/// from its first-draft hue (40 degrees) to 70 degrees after verification found it only 8.7
/// degrees from `secondary` and 3.6 degrees from the Material baseline `error` red -- close
/// enough to be visually indistinguishable in exactly the contexts (validation states, status
/// banners) where warning and error appear side by side. Final hues: success 150 degrees,
/// info 205 degrees, warning 70 degrees -- each at least ~21 degrees from every other role.
@immutable
class AppSemanticColors extends ThemeExtension<AppSemanticColors> {
  final Color success;
  final Color onSuccess;
  final Color successContainer;
  final Color onSuccessContainer;

  final Color info;
  final Color onInfo;
  final Color infoContainer;
  final Color onInfoContainer;

  final Color warning;
  final Color onWarning;
  final Color warningContainer;
  final Color onWarningContainer;

  const AppSemanticColors({
    required this.success,
    required this.onSuccess,
    required this.successContainer,
    required this.onSuccessContainer,
    required this.info,
    required this.onInfo,
    required this.infoContainer,
    required this.onInfoContainer,
    required this.warning,
    required this.onWarning,
    required this.warningContainer,
    required this.onWarningContainer,
  });

  static const AppSemanticColors light = AppSemanticColors(
    success: Color(0xFF126D38),
    onSuccess: Color(0xFFFFFFFF),
    successContainer: Color(0xFFA1F5B7),
    onSuccessContainer: Color(0xFF00210A),

    info: Color(0xFF11696E),
    onInfo: Color(0xFFFFFFFF),
    infoContainer: Color(0xFFA4EFF5),
    onInfoContainer: Color(0xFF002022),

    warning: Color(0xFF885201),
    onWarning: Color(0xFFFFFFFF),
    warningContainer: Color(0xFFFFDCBC),
    onWarningContainer: Color(0xFF291800),
  );

  static const AppSemanticColors dark = AppSemanticColors(
    success: Color(0xFF85D89C),
    onSuccess: Color(0xFF003919),
    successContainer: Color(0xFF005227),
    onSuccessContainer: Color(0xFFA1F5B7),

    info: Color(0xFF88D3D8),
    onInfo: Color(0xFF00363A),
    infoContainer: Color(0xFF004F54),
    onInfoContainer: Color(0xFFA4EFF5),

    warning: Color(0xFFFFB86B),
    onWarning: Color(0xFF482A00),
    warningContainer: Color(0xFF683D00),
    onWarningContainer: Color(0xFFFFDCBC),
  );

  static AppSemanticColors forBrightness(Brightness brightness) =>
      brightness == Brightness.dark ? dark : light;

  @override
  AppSemanticColors copyWith({
    Color? success,
    Color? onSuccess,
    Color? successContainer,
    Color? onSuccessContainer,
    Color? info,
    Color? onInfo,
    Color? infoContainer,
    Color? onInfoContainer,
    Color? warning,
    Color? onWarning,
    Color? warningContainer,
    Color? onWarningContainer,
  }) {
    return AppSemanticColors(
      success: success ?? this.success,
      onSuccess: onSuccess ?? this.onSuccess,
      successContainer: successContainer ?? this.successContainer,
      onSuccessContainer: onSuccessContainer ?? this.onSuccessContainer,
      info: info ?? this.info,
      onInfo: onInfo ?? this.onInfo,
      infoContainer: infoContainer ?? this.infoContainer,
      onInfoContainer: onInfoContainer ?? this.onInfoContainer,
      warning: warning ?? this.warning,
      onWarning: onWarning ?? this.onWarning,
      warningContainer: warningContainer ?? this.warningContainer,
      onWarningContainer: onWarningContainer ?? this.onWarningContainer,
    );
  }

  @override
  AppSemanticColors lerp(ThemeExtension<AppSemanticColors>? other, double t) {
    if (other is! AppSemanticColors) return this;
    return AppSemanticColors(
      success: Color.lerp(success, other.success, t)!,
      onSuccess: Color.lerp(onSuccess, other.onSuccess, t)!,
      successContainer: Color.lerp(successContainer, other.successContainer, t)!,
      onSuccessContainer: Color.lerp(onSuccessContainer, other.onSuccessContainer, t)!,
      info: Color.lerp(info, other.info, t)!,
      onInfo: Color.lerp(onInfo, other.onInfo, t)!,
      infoContainer: Color.lerp(infoContainer, other.infoContainer, t)!,
      onInfoContainer: Color.lerp(onInfoContainer, other.onInfoContainer, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      onWarning: Color.lerp(onWarning, other.onWarning, t)!,
      warningContainer: Color.lerp(warningContainer, other.warningContainer, t)!,
      onWarningContainer: Color.lerp(onWarningContainer, other.onWarningContainer, t)!,
    );
  }
}
