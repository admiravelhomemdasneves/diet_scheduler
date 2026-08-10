import 'package:flutter/material.dart';

import 'app_palette.dart';

/// Hand-authored Material 3 ColorSchemes -- NOT ColorScheme.fromSeed, which would desaturate
/// AppPalette.rose to a muddy tone and can only take one seed color, discarding four of the
/// five brand colors. Built in CIE LCh(ab), holding each brand color's hue/chroma constant
/// while sweeping lightness for the tonal roles, gamut-clamped where needed.
///
/// Role mapping (see AppPalette for the source colors):
///  - rose (E0607E)       -> primary family (both themes)
///  - terracotta (C2714F) -> secondary family (both themes)
///  - peach (F6C5AF)      -> secondaryContainer, light theme only (verbatim)
///  - sand (DBD3AD)       -> tertiaryContainer light / tertiary dark (verbatim), surface family
///  - coral (D36060)      -> surfaceTint + branding accents only -- deliberately no text-bearing
///    role: only 3.74:1 with white text (fails the 4.5:1 AA floor), and its hue sits just 16°
///    from primary's, too close to read as a distinct "this is an error" signal at small sizes.
///  - error                -> true Material red (#B3261E / #FFB4AB), independent of the brand
///    palette, for the reasons above.
///
/// Every text-bearing pair below is >=4.5:1 (normal text) or >=3.0:1 (outline/UI-component
/// roles against surface) -- verified by computing WCAG 2.1 relative luminance for all 46
/// role pairs across both brightnesses. `outlineVariant` is the one role Material's own stock
/// tone fails this floor at (M3 treats dividers as decorative, exempt from WCAG 1.4.11); both
/// brightnesses here are pulled several tones further from surface than the M3 default so
/// dividers still clear 3:1 against surface, at the cost of reading a little heavier than stock
/// Material and a narrower gap to `outline`.
abstract final class AppColorSchemes {
  static const ColorScheme light = ColorScheme(
    brightness: Brightness.light,

    primary: Color(0xFFA93A57),
    onPrimary: Color(0xFFFFFFFF),
    primaryContainer: Color(0xFFFFD9DE),
    onPrimaryContainer: Color(0xFF3F0016),

    secondary: Color(0xFF8F4B2E),
    onSecondary: Color(0xFFFFFFFF),
    secondaryContainer: AppPalette.peach,
    onSecondaryContainer: Color(0xFF311300),

    tertiary: Color(0xFF655F3B),
    onTertiary: Color(0xFFFFFFFF),
    tertiaryContainer: AppPalette.sand,
    onTertiaryContainer: Color(0xFF201C00),

    error: Color(0xFFB3261E),
    onError: Color(0xFFFFFFFF),
    errorContainer: Color(0xFFF9DEDC),
    onErrorContainer: Color(0xFF410E0B),

    surface: Color(0xFFFDF9F0),
    onSurface: Color(0xFF1E1B15),
    onSurfaceVariant: Color(0xFF4C4736),
    surfaceDim: Color(0xFFDDDAD0),
    surfaceBright: Color(0xFFFDF9F0),
    surfaceContainerLowest: Color(0xFFFFFFFF),
    surfaceContainerLow: Color(0xFFF7F3EA),
    surfaceContainer: Color(0xFFF1EEE4),
    surfaceContainerHigh: Color(0xFFEBE8DF),
    surfaceContainerHighest: Color(0xFFE6E2D9),

    outline: Color(0xFF7D7764),
    outlineVariant: Color(0xFF928B78),

    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
    inverseSurface: Color(0xFF333029),
    onInverseSurface: Color(0xFFF4F0E7),
    inversePrimary: Color(0xFFFFB1C2),
    surfaceTint: AppPalette.coral,
  );

  static const ColorScheme dark = ColorScheme(
    brightness: Brightness.dark,

    primary: Color(0xFFFFB1C2),
    onPrimary: Color(0xFF660028),
    primaryContainer: Color(0xFF8A143C),
    onPrimaryContainer: Color(0xFFFFD9DE),

    secondary: Color(0xFFFFB596),
    onSecondary: Color(0xFF591D00),
    secondaryContainer: Color(0xFF743418),
    onSecondaryContainer: Color(0xFFFFDBCC),

    tertiary: AppPalette.sand,
    onTertiary: Color(0xFF35310F),
    tertiaryContainer: Color(0xFF4C4724),
    onTertiaryContainer: Color(0xFFECE3B9),

    error: Color(0xFFFFB4AB),
    onError: Color(0xFF690005),
    errorContainer: Color(0xFF93000A),
    onErrorContainer: Color(0xFFFFDAD6),

    surface: Color(0xFF16130A),
    onSurface: Color(0xFFE6E2D9),
    onSurfaceVariant: Color(0xFFCDC6B2),
    surfaceDim: Color(0xFF16130A),
    surfaceBright: Color(0xFF3C3932),
    surfaceContainerLowest: Color(0xFF110E02),
    surfaceContainerLow: Color(0xFF1E1B15),
    surfaceContainer: Color(0xFF221F19),
    surfaceContainerHigh: Color(0xFF2C2A23),
    surfaceContainerHighest: Color(0xFF37352D),

    outline: Color(0xFF97907D),
    outlineVariant: Color(0xFF6E6856),

    shadow: Color(0xFF000000),
    scrim: Color(0xFF000000),
    inverseSurface: Color(0xFFE6E2D9),
    onInverseSurface: Color(0xFF333029),
    inversePrimary: Color(0xFFA93A57),
    surfaceTint: AppPalette.coral,
  );
}
