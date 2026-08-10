import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/nutrition_profile.dart';
import '../state/app_state.dart';

class NutritionProfileScreen extends StatefulWidget {
  const NutritionProfileScreen({super.key});

  @override
  State<NutritionProfileScreen> createState() => _NutritionProfileScreenState();
}

class _NutritionProfileScreenState extends State<NutritionProfileScreen> {
  bool _loaded = false;
  final _ageController = TextEditingController();
  final _weightController = TextEditingController();
  final _heightController = TextEditingController();
  final _calorieController = TextEditingController();
  final _proteinController = TextEditingController();
  final _carbsController = TextEditingController();
  final _fatController = TextEditingController();
  final _weightGoalController = TextEditingController();
  String? _gender;
  bool _populated = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      Future.microtask(() => context.read<AppState>().loadNutritionProfile());
    }
    _maybePopulate(appState.nutritionProfile);

    return Scaffold(
      appBar: AppBar(title: const Text('Nutrition profile')),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          const Text('Used by "target my nutrition" when auto-filling the meal plan.', style: TextStyle(color: Colors.grey, fontSize: 12)),
          const SizedBox(height: 12),
          Row(children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: _gender,
                decoration: const InputDecoration(labelText: 'Gender', isDense: true),
                items: const [
                  DropdownMenuItem(value: 'female', child: Text('Female')),
                  DropdownMenuItem(value: 'male', child: Text('Male')),
                  DropdownMenuItem(value: 'other', child: Text('Other')),
                ],
                onChanged: (v) => setState(() => _gender = v),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(child: TextField(controller: _ageController, decoration: const InputDecoration(labelText: 'Age', isDense: true), keyboardType: TextInputType.number)),
          ]),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(child: TextField(controller: _weightController, decoration: const InputDecoration(labelText: 'Weight (kg)', isDense: true), keyboardType: const TextInputType.numberWithOptions(decimal: true))),
            const SizedBox(width: 8),
            Expanded(child: TextField(controller: _heightController, decoration: const InputDecoration(labelText: 'Height (cm)', isDense: true), keyboardType: const TextInputType.numberWithOptions(decimal: true))),
          ]),
          const SizedBox(height: 8),
          TextField(controller: _weightGoalController, decoration: const InputDecoration(labelText: 'Weight goal (kg)', isDense: true), keyboardType: const TextInputType.numberWithOptions(decimal: true)),
          const SizedBox(height: 12),
          const Text('Daily targets', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          TextField(controller: _calorieController, decoration: const InputDecoration(labelText: 'Calories (kcal)', isDense: true), keyboardType: TextInputType.number),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(child: TextField(controller: _proteinController, decoration: const InputDecoration(labelText: 'Protein (g)', isDense: true), keyboardType: TextInputType.number)),
            const SizedBox(width: 8),
            Expanded(child: TextField(controller: _carbsController, decoration: const InputDecoration(labelText: 'Carbs (g)', isDense: true), keyboardType: TextInputType.number)),
            const SizedBox(width: 8),
            Expanded(child: TextField(controller: _fatController, decoration: const InputDecoration(labelText: 'Fat (g)', isDense: true), keyboardType: TextInputType.number)),
          ]),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton(onPressed: _save, child: const Text('Save')),
          ),
        ],
      ),
    );
  }

  void _maybePopulate(NutritionProfile? profile) {
    if (_populated || profile == null) return;
    _populated = true;
    _gender = profile.gender;
    _ageController.text = profile.age?.toString() ?? '';
    _weightController.text = profile.weight?.toString() ?? '';
    _heightController.text = profile.height?.toString() ?? '';
    _calorieController.text = profile.calorieTarget?.toString() ?? '';
    _proteinController.text = profile.proteinTargetGrams?.toString() ?? '';
    _carbsController.text = profile.carbsTargetGrams?.toString() ?? '';
    _fatController.text = profile.fatTargetGrams?.toString() ?? '';
    _weightGoalController.text = profile.weightGoalKg?.toString() ?? '';
  }

  void _save() {
    context.read<AppState>().updateNutritionProfile(
          gender: _gender,
          age: int.tryParse(_ageController.text),
          weight: double.tryParse(_weightController.text),
          height: double.tryParse(_heightController.text),
          calorieTarget: double.tryParse(_calorieController.text),
          proteinTargetGrams: double.tryParse(_proteinController.text),
          carbsTargetGrams: double.tryParse(_carbsController.text),
          fatTargetGrams: double.tryParse(_fatController.text),
          weightGoalKg: double.tryParse(_weightGoalController.text),
        );
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Nutrition profile saved.')));
  }
}
