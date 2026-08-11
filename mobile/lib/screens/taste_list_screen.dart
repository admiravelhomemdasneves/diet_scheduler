import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/taste.dart';
import '../state/app_state.dart';
import '../theme/semantic_mappings.dart';

/// "See all" page for a taste list (my tastes or household tastes, selected via the [selector]/
/// [onSet]/[onRemove] closures passed in), reached from TasteSearchBox once there are more than 3.
/// Each row lets the user change the taste's preference category or remove it entirely.
class TasteListScreen extends StatelessWidget {
  final String title;
  final List<TastePreferenceEntry> Function(AppState) selector;
  final void Function(AppState appState, String tasteId, String preference) onSet;
  final void Function(AppState appState, String tasteId) onRemove;

  const TasteListScreen({
    super.key,
    required this.title,
    required this.selector,
    required this.onSet,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final entries = selector(appState);
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView.builder(
        itemCount: entries.length,
        itemBuilder: (context, i) {
          final t = entries[i];
          return ListTile(
            leading: CircleAvatar(radius: 6, backgroundColor: colorForTastePreference(context, t.preference)),
            title: Text(t.tasteName),
            subtitle: Text(t.type),
            trailing: DropdownButton<String?>(
              value: t.preference,
              items: [
                ...tastePreferences.map((p) => DropdownMenuItem<String?>(value: p, child: Text(tastePreferenceLabel(p)))),
                const DropdownMenuItem<String?>(value: null, child: Text('Remove')),
              ],
              onChanged: (pref) {
                final s = context.read<AppState>();
                if (pref == null) {
                  onRemove(s, t.tasteId);
                } else {
                  onSet(s, t.tasteId, pref);
                }
              },
            ),
          );
        },
      ),
    );
  }
}
