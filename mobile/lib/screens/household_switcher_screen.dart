import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/household.dart';
import '../state/app_state.dart';

class HouseholdSwitcherScreen extends StatefulWidget {
  const HouseholdSwitcherScreen({super.key});

  @override
  State<HouseholdSwitcherScreen> createState() => _HouseholdSwitcherScreenState();
}

class _HouseholdSwitcherScreenState extends State<HouseholdSwitcherScreen> {
  final _createNameController = TextEditingController();
  final _joinCodeController = TextEditingController();
  bool _busy = false;
  String? _switchingId;

  @override
  void initState() {
    super.initState();
    context.read<AppState>().loadMyHouseholds();
  }

  @override
  void dispose() {
    _createNameController.dispose();
    _joinCodeController.dispose();
    super.dispose();
  }

  Future<void> _switchTo(Household household) async {
    setState(() => _switchingId = household.id);
    await context.read<AppState>().switchHousehold(household.id);
    if (!mounted) return;
    setState(() => _switchingId = null);
    if (context.read<AppState>().errorMessage == null) {
      Navigator.popUntil(context, (route) => route.isFirst);
    }
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _busy = true);
    await action();
    if (!mounted) return;
    setState(() => _busy = false);
    if (context.read<AppState>().errorMessage == null) {
      Navigator.popUntil(context, (route) => route.isFirst);
    }
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final currentId = appState.currentHousehold?.id;

    return Scaffold(
      appBar: AppBar(title: const Text('Your households')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (appState.myHouseholds.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(child: CircularProgressIndicator()),
            )
          else
            ...appState.myHouseholds.map((household) {
              final isCurrent = household.id == currentId;
              return Card(
                color: isCurrent ? Theme.of(context).colorScheme.primaryContainer : null,
                child: ListTile(
                  leading: Icon(isCurrent ? Icons.check_circle : Icons.home_outlined),
                  title: Text(household.name),
                  subtitle: Text('Invite code: ${household.inviteCode} · ${household.members.length} member(s)'),
                  trailing: _switchingId == household.id
                      ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                      : (isCurrent ? null : const Icon(Icons.chevron_right)),
                  onTap: isCurrent || _switchingId != null ? null : () => _switchTo(household),
                ),
              );
            }),
          const SizedBox(height: 32),
          const Divider(),
          const SizedBox(height: 16),
          const Text('Create a new household', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          TextField(
            controller: _createNameController,
            decoration: const InputDecoration(labelText: 'Household name', border: OutlineInputBorder()),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: _busy || _createNameController.text.trim().isEmpty
                ? null
                : () => _run(() => context.read<AppState>().createHousehold(_createNameController.text.trim())),
            child: const Text('Create'),
          ),
          const SizedBox(height: 32),
          const Text('Join an existing household', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          TextField(
            controller: _joinCodeController,
            decoration: const InputDecoration(labelText: 'Invite code', border: OutlineInputBorder()),
            textCapitalization: TextCapitalization.characters,
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 8),
          OutlinedButton(
            onPressed: _busy || _joinCodeController.text.trim().isEmpty
                ? null
                : () => _run(() => context.read<AppState>().joinHousehold(_joinCodeController.text.trim())),
            child: const Text('Join'),
          ),
          if (_busy) ...[
            const SizedBox(height: 24),
            const Center(child: CircularProgressIndicator()),
          ],
          if (appState.errorMessage != null) ...[
            const SizedBox(height: 16),
            Text(appState.errorMessage!, style: const TextStyle(color: Colors.red)),
          ],
        ],
      ),
    );
  }
}
