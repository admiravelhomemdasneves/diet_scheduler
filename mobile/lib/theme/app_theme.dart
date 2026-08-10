import 'package:flutter/material.dart';

import 'app_semantic_colors.dart';

/// Builds the app's ThemeData from a ColorScheme -- shared by light and dark, since every
/// component theme here is expressed in terms of ColorScheme roles rather than literal
/// colors, so it automatically adapts to whichever brightness's scheme is passed in.
ThemeData buildAppTheme(ColorScheme colorScheme) {
  final radius12 = BorderRadius.circular(12);
  final radius16 = BorderRadius.circular(16);

  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    brightness: colorScheme.brightness,
    scaffoldBackgroundColor: colorScheme.surface,
    extensions: [AppSemanticColors.forBrightness(colorScheme.brightness)],

    appBarTheme: AppBarTheme(
      backgroundColor: colorScheme.surface,
      foregroundColor: colorScheme.onSurface,
      surfaceTintColor: colorScheme.surfaceTint,
      elevation: 0,
      scrolledUnderElevation: 2,
    ),

    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: colorScheme.surfaceContainer,
      indicatorColor: colorScheme.secondaryContainer,
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        final selected = states.contains(WidgetState.selected);
        return TextStyle(
          fontSize: 12,
          fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
          color: selected ? colorScheme.onSecondaryContainer : colorScheme.onSurfaceVariant,
        );
      }),
      iconTheme: WidgetStateProperty.resolveWith((states) {
        final selected = states.contains(WidgetState.selected);
        return IconThemeData(color: selected ? colorScheme.onSecondaryContainer : colorScheme.onSurfaceVariant);
      }),
    ),

    cardTheme: CardThemeData(
      color: colorScheme.surfaceContainerLow,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: radius16),
      margin: const EdgeInsets.symmetric(vertical: 4),
    ),

    chipTheme: ChipThemeData(
      backgroundColor: colorScheme.surfaceContainerHighest,
      labelStyle: TextStyle(color: colorScheme.onSurfaceVariant),
      side: BorderSide(color: colorScheme.outlineVariant),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
    ),

    dividerTheme: DividerThemeData(color: colorScheme.outlineVariant, thickness: 1, space: 1),

    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: colorScheme.primary,
        foregroundColor: colorScheme.onPrimary,
        shape: RoundedRectangleBorder(borderRadius: radius12),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
      ),
    ),

    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: colorScheme.secondaryContainer,
        foregroundColor: colorScheme.onSecondaryContainer,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: radius12),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
      ),
    ),

    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: colorScheme.primary,
        side: BorderSide(color: colorScheme.outline),
        shape: RoundedRectangleBorder(borderRadius: radius12),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
      ),
    ),

    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(foregroundColor: colorScheme.primary),
    ),

    floatingActionButtonTheme: FloatingActionButtonThemeData(
      backgroundColor: colorScheme.primaryContainer,
      foregroundColor: colorScheme.onPrimaryContainer,
    ),

    snackBarTheme: SnackBarThemeData(
      backgroundColor: colorScheme.inverseSurface,
      contentTextStyle: TextStyle(color: colorScheme.onInverseSurface),
      actionTextColor: colorScheme.inversePrimary,
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
    ),

    // This app is form-heavy (17 screens use a text field); a filled, rounded field with a
    // clear focus/error state is the single highest-value component theme here -- every
    // field that was previously the bare Material default now gets this for free.
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: colorScheme.surfaceContainerHighest,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      labelStyle: TextStyle(color: colorScheme.onSurfaceVariant),
      hintStyle: TextStyle(color: colorScheme.onSurfaceVariant.withValues(alpha: 0.7)),
      border: OutlineInputBorder(borderRadius: radius12, borderSide: BorderSide.none),
      enabledBorder: OutlineInputBorder(borderRadius: radius12, borderSide: BorderSide(color: colorScheme.outline)),
      focusedBorder: OutlineInputBorder(borderRadius: radius12, borderSide: BorderSide(color: colorScheme.primary, width: 2)),
      errorBorder: OutlineInputBorder(borderRadius: radius12, borderSide: BorderSide(color: colorScheme.error)),
      focusedErrorBorder: OutlineInputBorder(borderRadius: radius12, borderSide: BorderSide(color: colorScheme.error, width: 2)),
      errorStyle: TextStyle(color: colorScheme.error),
    ),

    listTileTheme: ListTileThemeData(
      iconColor: colorScheme.onSurfaceVariant,
      textColor: colorScheme.onSurface,
    ),

    dialogTheme: DialogThemeData(
      backgroundColor: colorScheme.surfaceContainerHigh,
      shape: RoundedRectangleBorder(borderRadius: radius16),
    ),

    tooltipTheme: TooltipThemeData(
      decoration: BoxDecoration(
        color: colorScheme.inverseSurface,
        borderRadius: BorderRadius.circular(6),
      ),
      textStyle: TextStyle(color: colorScheme.onInverseSurface),
    ),

    progressIndicatorTheme: ProgressIndicatorThemeData(color: colorScheme.primary),
  );
}
