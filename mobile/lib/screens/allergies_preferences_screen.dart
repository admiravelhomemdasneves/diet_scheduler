import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../widgets/allergy_search_box.dart';
import '../widgets/taste_search_box.dart';
import 'allergy_list_screen.dart';
import 'taste_list_screen.dart';

/// Personal allergy/taste preferences. Household-level allergies and tastes live on their own
/// "Household preferences" screen (reached from the Preferences menu), since a household's list is
/// independently editable per household, not a per-user setting.
class AllergiesPreferencesScreen extends StatefulWidget {
  const AllergiesPreferencesScreen({super.key});

  @override
  State<AllergiesPreferencesScreen> createState() => _AllergiesPreferencesScreenState();
}

class _AllergiesPreferencesScreenState extends State<AllergiesPreferencesScreen> {
  bool _loaded = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      final s = context.read<AppState>();
      Future.microtask(() {
        s.loadReferenceData();
        s.loadMyPreferences();
      });
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Allergies and Preferences')),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          const Text('My allergies', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          Text('Hard exclude — recipes containing these are never shown.',
              style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12)),
          const SizedBox(height: 8),
          AllergySearchBox(
            allAllergies: appState.allAllergies,
            selected: appState.myAllergies,
            onAdd: (id) => context.read<AppState>().addMyAllergy(id),
            onRemove: (id) => context.read<AppState>().removeMyAllergy(id),
            onSeeAll: () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => AllergyListScreen(
                title: 'All allergies',
                selector: (s) => s.myAllergies,
                onRemove: (s, id) => s.removeMyAllergy(id),
              ),
            )),
          ),
          const SizedBox(height: 24),
          const Text('My tastes', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          Text('Pick a category, search, then tap a match to add it.',
              style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12)),
          const SizedBox(height: 8),
          TasteSearchBox(
            allTastes: appState.allTastes,
            selected: appState.myTastes,
            onSet: (id, pref) => context.read<AppState>().setMyTaste(id, pref),
            onRemove: (id) => context.read<AppState>().removeMyTaste(id),
            onSeeAll: () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => TasteListScreen(
                title: 'All my tastes',
                selector: (s) => s.myTastes,
                onSet: (s, id, pref) => s.setMyTaste(id, pref),
                onRemove: (s, id) => s.removeMyTaste(id),
              ),
            )),
          ),
        ],
      ),
    );
  }
}
