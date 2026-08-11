import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';
import '../widgets/error_banner.dart';

class HouseholdScreen extends StatefulWidget {
  const HouseholdScreen({super.key});

  @override
  State<HouseholdScreen> createState() => _HouseholdScreenState();
}

class _HouseholdScreenState extends State<HouseholdScreen> {
  final _createNameController = TextEditingController();
  final _joinCodeController = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _createNameController.dispose();
    _joinCodeController.dispose();
    super.dispose();
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _busy = true);
    await action();
    if (mounted) setState(() => _busy = false);
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Household'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => context.read<AppState>().signOut(),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text('Signed in as ${appState.currentUser?.email ?? ''}'),
          const SizedBox(height: 32),
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
          const SizedBox(height: 40),
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
          ErrorBanner(message: appState.errorMessage),
        ],
      ),
    );
  }
}
