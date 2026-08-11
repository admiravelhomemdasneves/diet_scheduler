import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import 'household_preferences_detail_screen.dart';

/// Lists all of the user's households; tapping one opens its members + household-level
/// allergies/tastes.
class HouseholdPreferencesListScreen extends StatefulWidget {
  const HouseholdPreferencesListScreen({super.key});

  @override
  State<HouseholdPreferencesListScreen> createState() => _HouseholdPreferencesListScreenState();
}

class _HouseholdPreferencesListScreenState extends State<HouseholdPreferencesListScreen> {
  bool _loaded = false;
  bool _creating = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      Future.microtask(() {
        if (context.mounted) context.read<AppState>().loadMyHouseholds();
      });
    }
    final activeId = appState.currentHousehold?.id;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Household preferences'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Create household',
            onPressed: _creating ? null : () => _showCreateDialog(context),
          ),
        ],
      ),
      body: appState.myHouseholds.isEmpty
          ? const Padding(
              padding: EdgeInsets.all(32),
              child: Center(child: Text('You are not part of any household yet.')),
            )
          : ListView.builder(
              itemCount: appState.myHouseholds.length,
              itemBuilder: (context, i) {
                final household = appState.myHouseholds[i];
                final isActive = household.id == activeId;
                return ListTile(
                  leading: Icon(isActive ? Icons.check_circle : Icons.home_outlined,
                      color: isActive ? Theme.of(context).colorScheme.primary : null),
                  title: Text(household.name),
                  subtitle: Text(isActive
                      ? 'Active · ${household.members.length} member${household.members.length == 1 ? '' : 's'}'
                      : '${household.members.length} member${household.members.length == 1 ? '' : 's'}'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => HouseholdPreferencesDetailScreen(household: household),
                  )),
                );
              },
            ),
    );
  }

  Future<void> _showCreateDialog(BuildContext context) async {
    final controller = TextEditingController();
    final name = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Create household'),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(labelText: 'Household name', border: OutlineInputBorder()),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(controller.text.trim()),
            child: const Text('Create'),
          ),
        ],
      ),
    );
    if (name == null || name.isEmpty || !context.mounted) return;
    setState(() => _creating = true);
    final appState = context.read<AppState>();
    await appState.createHousehold(name);
    await appState.loadMyHouseholds();
    if (!mounted) return;
    setState(() => _creating = false);
  }
}
