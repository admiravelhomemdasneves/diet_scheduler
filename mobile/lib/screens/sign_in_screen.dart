import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';

class SignInScreen extends StatelessWidget {
  const SignInScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.kitchen, size: 72),
              const SizedBox(height: 16),
              const Text('DietScheduler', style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              const Text('Sign in to manage your household pantry'),
              const SizedBox(height: 32),
              FilledButton.icon(
                onPressed: () => context.read<AppState>().signInWithGoogle(),
                icon: const Icon(Icons.login),
                label: const Text('Sign in with Google'),
              ),
              if (appState.errorMessage != null) ...[
                const SizedBox(height: 16),
                Text(appState.errorMessage!, style: const TextStyle(color: Colors.red)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
