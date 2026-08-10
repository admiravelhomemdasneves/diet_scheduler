import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/allergy.dart';
import '../state/app_state.dart';

/// "See all" page for an allergy list (my allergies or a household's allergies, selected via the
/// [selector]/[onRemove] closures passed in), reached from AllergySearchBox once there are more than 3.
class AllergyListScreen extends StatelessWidget {
  final String title;
  final List<Allergy> Function(AppState) selector;
  final void Function(AppState appState, String allergyId) onRemove;

  const AllergyListScreen({super.key, required this.title, required this.selector, required this.onRemove});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final allergies = selector(appState);
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView.builder(
        itemCount: allergies.length,
        itemBuilder: (context, i) {
          final a = allergies[i];
          return ListTile(
            title: Text(a.name),
            trailing: IconButton(
              icon: const Icon(Icons.delete_outline),
              onPressed: () => onRemove(context.read<AppState>(), a.id),
            ),
          );
        },
      ),
    );
  }
}
