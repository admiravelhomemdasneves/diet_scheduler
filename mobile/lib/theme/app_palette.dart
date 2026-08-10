import 'package:flutter/material.dart';

/// The five raw brand colors the app's palette is derived from. None of these are used
/// directly as widget colors -- see app_color_schemes.dart for the actual Material 3
/// ColorScheme roles derived from them (several needed darkening/lightening to carry
/// text at an accessible contrast ratio; see that file's doc comment for the mapping
/// and the contrast ratios verified for each role).
abstract final class AppPalette {
  /// "Main" in the source palette -- warm sand/tan, ~70% of pixels. Maps to the surface
  /// family (light theme) and to tertiary (dark theme).
  static const Color sand = Color(0xFFDBD3AD);

  /// "Secondary" in the source palette -- the only high-chroma color of the five.
  /// Maps to the primary family (both themes).
  static const Color rose = Color(0xFFE0607E);

  /// Maps to surfaceTint and branding/logo accents only -- deliberately given no
  /// text-bearing role (fails 4.5:1 with white text; too close in hue to the error red
  /// to be visually distinct at small sizes).
  static const Color coral = Color(0xFFD36060);

  /// Maps to the secondary family (both themes).
  static const Color terracotta = Color(0xFFC2714F);

  /// Maps to secondaryContainer (light theme only; darkened for dark-theme use elsewhere).
  static const Color peach = Color(0xFFF6C5AF);
}
