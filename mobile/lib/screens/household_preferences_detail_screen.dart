import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../models/household.dart';
import '../state/app_state.dart';
import '../widgets/allergy_search_box.dart';
import '../widgets/error_banner.dart';
import '../widgets/taste_search_box.dart';
import 'allergy_list_screen.dart';
import 'taste_list_screen.dart';

/// Members + household-level allergies/tastes for one household. The household's allergy/taste
/// lists are one-time-inherited from the union of its members' personal preferences (server-side,
/// on first load) and independently editable from there -- editing here never changes any member's
/// personal preferences, and vice versa.
class HouseholdPreferencesDetailScreen extends StatefulWidget {
  final Household household;
  const HouseholdPreferencesDetailScreen({super.key, required this.household});

  @override
  State<HouseholdPreferencesDetailScreen> createState() => _HouseholdPreferencesDetailScreenState();
}

class _HouseholdPreferencesDetailScreenState extends State<HouseholdPreferencesDetailScreen> {
  bool _loaded = false;
  bool _busy = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      final s = context.read<AppState>();
      Future.microtask(() {
        s.loadReferenceData();
        s.loadHouseholdAllergies(widget.household.id);
        s.loadHouseholdTastes(widget.household.id);
      });
    }

    final isActive = widget.household.id == appState.currentHousehold?.id;
    String? myRole;
    for (final m in widget.household.members) {
      if (m.userId == appState.currentUser?.id) {
        myRole = m.role;
        break;
      }
    }
    final isOwner = myRole == 'OWNER';

    return Scaffold(
      appBar: AppBar(title: Text(widget.household.name)),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.vpn_key_outlined),
                  title: const Text('Invite code'),
                  subtitle: Text(widget.household.inviteCode, style: const TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1.5)),
                  trailing: IconButton(
                    icon: const Icon(Icons.copy_outlined),
                    tooltip: 'Copy invite code',
                    onPressed: () {
                      Clipboard.setData(ClipboardData(text: widget.household.inviteCode));
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Invite code copied')));
                    },
                  ),
                ),
                const Divider(height: 1),
                isActive
                    ? ListTile(
                        leading: Icon(Icons.check_circle, color: Theme.of(context).colorScheme.primary),
                        title: const Text('Currently active household'),
                      )
                    : ListTile(
                        leading: const Icon(Icons.radio_button_unchecked),
                        title: const Text('Set as active household'),
                        trailing: _busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)) : null,
                        onTap: _busy ? null : () => _setActive(context),
                      ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          const Text('Members', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 4),
          ...widget.household.members.map((m) => ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: const CircleAvatar(child: Icon(Icons.person, size: 18)),
                title: Text(m.displayName ?? m.email ?? m.userId),
                subtitle: m.displayName != null && m.email != null ? Text(m.email!) : null,
                trailing: Text(m.role, style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12)),
              )),
          const SizedBox(height: 24),
          const Text('Household allergies', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          Text(
            'Inherited once from the union of members\' allergies, then editable just for this household.',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12),
          ),
          const SizedBox(height: 8),
          AllergySearchBox(
            allAllergies: appState.allAllergies,
            selected: appState.householdAllergies,
            onAdd: (id) => context.read<AppState>().addHouseholdAllergy(widget.household.id, id),
            onRemove: (id) => context.read<AppState>().removeHouseholdAllergy(widget.household.id, id),
            onSeeAll: () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => AllergyListScreen(
                title: 'All household allergies',
                selector: (s) => s.householdAllergies,
                onRemove: (s, id) => s.removeHouseholdAllergy(widget.household.id, id),
              ),
            )),
          ),
          const SizedBox(height: 24),
          const Text('Household tastes', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          Text(
            'Inherited once from the union of members\' tastes, then editable just for this household.',
            style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12),
          ),
          const SizedBox(height: 8),
          TasteSearchBox(
            allTastes: appState.allTastes,
            selected: appState.householdTastes,
            onSet: (id, pref) => context.read<AppState>().setHouseholdTaste(widget.household.id, id, pref),
            onRemove: (id) => context.read<AppState>().removeHouseholdTaste(widget.household.id, id),
            onSeeAll: () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => TasteListScreen(
                title: 'All household tastes',
                selector: (s) => s.householdTastes,
                onSet: (s, id, pref) => s.setHouseholdTaste(widget.household.id, id, pref),
                onRemove: (s, id) => s.removeHouseholdTaste(widget.household.id, id),
              ),
            )),
          ),
          const SizedBox(height: 32),
          const Divider(),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            icon: const Icon(Icons.exit_to_app),
            label: const Text('Leave household'),
            style: OutlinedButton.styleFrom(foregroundColor: Theme.of(context).colorScheme.error),
            onPressed: _busy ? null : () => _confirmLeave(context),
          ),
          if (isOwner) ...[
            const SizedBox(height: 8),
            FilledButton.icon(
              icon: const Icon(Icons.delete_forever_outlined),
              label: const Text('Delete household'),
              style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
              onPressed: _busy ? null : () => _confirmDelete(context),
            ),
          ],
          ErrorBanner(message: appState.errorMessage),
        ],
      ),
    );
  }

  Future<void> _setActive(BuildContext context) async {
    setState(() => _busy = true);
    await context.read<AppState>().switchHousehold(widget.household.id);
    if (!mounted) return;
    setState(() => _busy = false);
  }

  Future<void> _confirmLeave(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Leave household?'),
        content: Text('You\'ll lose access to "${widget.household.name}" and its pantry, meal plans, and shopping list. '
            'You can rejoin later with the invite code.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancel')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Theme.of(dialogContext).colorScheme.error),
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Leave'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;
    setState(() => _busy = true);
    final appState = context.read<AppState>();
    await appState.leaveHousehold(widget.household.id);
    if (!context.mounted) return;
    setState(() => _busy = false);
    if (appState.errorMessage == null) Navigator.of(context).pop();
  }

  Future<void> _confirmDelete(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Delete household?'),
        content: Text('This permanently deletes "${widget.household.name}" for every member -- its pantry, meal plans, '
            'shopping list, and household preferences are all erased. This cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancel')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Theme.of(dialogContext).colorScheme.error),
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;
    setState(() => _busy = true);
    final appState = context.read<AppState>();
    await appState.deleteHousehold(widget.household.id);
    if (!context.mounted) return;
    setState(() => _busy = false);
    if (appState.errorMessage == null) Navigator.of(context).pop();
  }
}
