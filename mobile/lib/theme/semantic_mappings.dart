import 'package:flutter/material.dart';

import 'app_semantic_colors.dart';

/// Maps a taste-preference string (FAVORITE/LIKED/DISLIKED/FORBIDDEN) to a themed color.
/// Replaces lib/models/taste.dart's old colorForTastePreference, which returned hardcoded
/// Colors.green/blue/orange/red -- a model file importing material.dart for raw colors was
/// a layering smell that also made the mapping untestable independent of a widget tree.
/// FORBIDDEN maps to colorScheme.error rather than a semantic color of its own: "forbidden"
/// already IS what error means (a hard stop), so a separate color would just be a second red
/// with no distinct meaning.
Color colorForTastePreference(BuildContext context, String preference) {
  final semantic = Theme.of(context).extension<AppSemanticColors>()!;
  switch (preference) {
    case 'FAVORITE':
      return semantic.success;
    case 'LIKED':
      return semantic.info;
    case 'DISLIKED':
      return semantic.warning;
    case 'FORBIDDEN':
      return Theme.of(context).colorScheme.error;
    default:
      return Theme.of(context).colorScheme.onSurfaceVariant;
  }
}
