import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../models/ingredient.dart';
import '../state/app_state.dart';

/// Small standalone create/edit form for a household's custom ingredient (name/image/nutrition
/// only -- no pantry-specific fields like location/quantity/unit). Reached from the custom
/// ingredients management page, not from the pantry-item flow (see AddPantryItemScreen for that).
class CustomIngredientEditScreen extends StatefulWidget {
  final Ingredient? editing;

  const CustomIngredientEditScreen({super.key, this.editing});

  @override
  State<CustomIngredientEditScreen> createState() => _CustomIngredientEditScreenState();
}

class _CustomIngredientEditScreenState extends State<CustomIngredientEditScreen> {
  late final _nameController = TextEditingController(text: widget.editing?.name ?? '');
  late final _imageUrlController = TextEditingController(text: widget.editing?.imageUrl ?? '');
  late final _caloriesController = TextEditingController(text: _fmt(widget.editing?.caloriesPer100g));
  late final _proteinController = TextEditingController(text: _fmt(widget.editing?.proteinPer100g));
  late final _carbsController = TextEditingController(text: _fmt(widget.editing?.carbsPer100g));
  late final _fatController = TextEditingController(text: _fmt(widget.editing?.fatPer100g));
  bool _saving = false;

  bool get _isEditing => widget.editing != null;

  static String _fmt(double? v) {
    if (v == null) return '';
    return v == v.roundToDouble() ? v.toInt().toString() : v.toString();
  }

  @override
  void dispose() {
    _nameController.dispose();
    _imageUrlController.dispose();
    _caloriesController.dispose();
    _proteinController.dispose();
    _carbsController.dispose();
    _fatController.dispose();
    super.dispose();
  }

  String? _emptyToNull(String s) => s.trim().isEmpty ? null : s.trim();

  Future<void> _save() async {
    final name = _nameController.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Enter a name.')));
      return;
    }
    setState(() => _saving = true);
    final appState = context.read<AppState>();
    final ok = _isEditing
        ? await appState.updateCustomIngredient(
            id: widget.editing!.id,
            name: name,
            imageUrl: _emptyToNull(_imageUrlController.text),
            caloriesPer100g: double.tryParse(_caloriesController.text),
            proteinPer100g: double.tryParse(_proteinController.text),
            carbsPer100g: double.tryParse(_carbsController.text),
            fatPer100g: double.tryParse(_fatController.text),
          )
        : await appState.createCustomIngredient(
            name: name,
            imageUrl: _emptyToNull(_imageUrlController.text),
            caloriesPer100g: double.tryParse(_caloriesController.text),
            proteinPer100g: double.tryParse(_proteinController.text),
            carbsPer100g: double.tryParse(_carbsController.text),
            fatPer100g: double.tryParse(_fatController.text),
          );
    if (!mounted) return;
    if (ok) {
      Navigator.pop(context);
    } else {
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(appState.errorMessage ?? 'Could not save ingredient.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final rawImageUrl = _imageUrlController.text.trim();
    final imageUrl = ApiConfig.resolveImageUrl(rawImageUrl.isEmpty ? null : rawImageUrl);

    return Scaffold(
      appBar: AppBar(title: Text(_isEditing ? 'Edit ingredient' : 'New custom ingredient')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: imageUrl != null
                    ? Image.network(
                        imageUrl,
                        width: 120,
                        height: 120,
                        fit: BoxFit.cover,
                        errorBuilder: (_, _, _) => _imagePlaceholder(),
                      )
                    : _imagePlaceholder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: 'Ingredient name'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _imageUrlController,
              decoration: const InputDecoration(labelText: 'Image URL'),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 20),
            Text('Nutrition per 100g', style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(child: _nutritionField('Calories', _caloriesController)),
                const SizedBox(width: 8),
                Expanded(child: _nutritionField('Protein (g)', _proteinController)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(child: _nutritionField('Carbs (g)', _carbsController)),
                const SizedBox(width: 8),
                Expanded(child: _nutritionField('Fat (g)', _fatController)),
              ],
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _saving ? null : _save,
              child: Text(_saving ? 'Saving…' : 'Save'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _imagePlaceholder() => Container(
        width: 120,
        height: 120,
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        child: const Icon(Icons.image_not_supported_outlined, size: 40),
      );

  Widget _nutritionField(String label, TextEditingController controller) => TextField(
        controller: controller,
        decoration: InputDecoration(labelText: label),
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
      );
}
