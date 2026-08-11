import 'package:flutter/material.dart';

/// Persistent inline error display for screens where a transient SnackBar isn't enough --
/// typically because the error means the screen's own action failed and the user needs to see
/// why while they decide what to do next, not just get a toast that's gone in a few seconds.
/// Renders nothing when [message] is null, so it's safe to place unconditionally in a screen's
/// widget tree (e.g. `ErrorBanner(message: appState.errorMessage)`).
class ErrorBanner extends StatelessWidget {
  final String? message;
  const ErrorBanner({super.key, required this.message});

  @override
  Widget build(BuildContext context) {
    if (message == null) return const SizedBox.shrink();
    final colorScheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colorScheme.errorContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.error_outline, size: 20, color: colorScheme.onErrorContainer),
          const SizedBox(width: 8),
          Expanded(
            child: Text(message!, style: TextStyle(color: colorScheme.onErrorContainer)),
          ),
        ],
      ),
    );
  }
}
