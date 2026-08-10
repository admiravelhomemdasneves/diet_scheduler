import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../state/app_state.dart';

class ShoppingPreferencesScreen extends StatefulWidget {
  const ShoppingPreferencesScreen({super.key});

  @override
  State<ShoppingPreferencesScreen> createState() => _ShoppingPreferencesScreenState();
}

class _ShoppingPreferencesScreenState extends State<ShoppingPreferencesScreen> {
  bool _loaded = false;
  String _unitSystem = 'METRIC';
  bool _unitPopulated = false;
  final _daysController = TextEditingController();
  bool _daysPopulated = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      Future.microtask(() {
        context.read<AppState>().loadUnitSystem();
        context.read<AppState>().loadShoppingPreferences();
      });
    }
    _maybePopulateUnitSystem(appState.unitSystem);
    _maybePopulateDays(appState.shoppingListLookaheadDays);

    return Scaffold(
      appBar: AppBar(title: const Text('Shopping List')),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          const Text('Measurement units', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const Text('Controls which units appear in the ingredient unit picker.', style: TextStyle(color: Colors.grey, fontSize: 12)),
          const SizedBox(height: 8),
          SegmentedButton<String>(
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(value: 'METRIC', label: Text('Metric')),
              ButtonSegment(value: 'IMPERIAL', label: Text('Imperial')),
              ButtonSegment(value: 'BOTH', label: Text('Both')),
            ],
            selected: {_unitSystem},
            onSelectionChanged: (selection) {
              final v = selection.first;
              setState(() => _unitSystem = v);
              context.read<AppState>().updateUnitSystem(v);
            },
          ),
          const SizedBox(height: 24),
          const Text('Missing ingredients', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const Text(
            'How far ahead the "Missing for future recipes" section on the shopping list looks for upcoming meal plans.',
            style: TextStyle(color: Colors.grey, fontSize: 12),
          ),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(
              child: TextField(
                controller: _daysController,
                decoration: const InputDecoration(labelText: 'Number of days to check missing ingredients', isDense: true),
                keyboardType: TextInputType.number,
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(onPressed: _saveDays, child: const Text('Save')),
          ]),
        ],
      ),
    );
  }

  void _maybePopulateUnitSystem(String? unitSystem) {
    if (_unitPopulated || unitSystem == null) return;
    _unitPopulated = true;
    _unitSystem = unitSystem;
  }

  void _maybePopulateDays(int days) {
    if (_daysPopulated) return;
    _daysPopulated = true;
    _daysController.text = days.toString();
  }

  void _saveDays() {
    final days = int.tryParse(_daysController.text);
    if (days == null || days < 1) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enter a number of days of 1 or more.')));
      return;
    }
    context.read<AppState>().updateShoppingPreferences(days);
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Shopping preferences saved.')));
  }
}
