import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../models/ingredient.dart';
import '../models/pantry_item.dart';
import '../state/app_state.dart';
import '../widgets/ingredient_name_field.dart';
import '../widgets/unit_dropdown.dart';

/// Full-page ingredient entry: name (with live search-as-you-type suggestions), image, nutrition
/// facts and pantry location/quantity/unit -- all always editable, whether the ingredient is a
/// fresh custom name, a barcode/search match downloaded from Open Food Facts, or one already
/// attached to an existing pantry item. Saving an actual change to a shared/other-household
/// ingredient forks a new "custom" ingredient owned by this household (see
/// IngredientService.resolveEditableIngredient on the backend) rather than mutating shared data;
/// an unchanged submission, or editing an ingredient already owned by this household, doesn't.
class AddPantryItemScreen extends StatefulWidget {
  final Ingredient? prefill;
  final PantryItem? editing;

  const AddPantryItemScreen({super.key, this.prefill, this.editing})
      : assert(prefill == null || editing == null, 'Cannot both prefill and edit an item');

  @override
  State<AddPantryItemScreen> createState() => _AddPantryItemScreenState();
}

class _AddPantryItemScreenState extends State<AddPantryItemScreen> {
  late final _quantityController =
      TextEditingController(text: widget.editing != null ? _fmt(widget.editing!.quantity) : '1');
  late final _imageUrlController =
      TextEditingController(text: widget.editing?.ingredientImageUrl ?? widget.prefill?.imageUrl ?? '');
  late final _caloriesController = TextEditingController(
      text: _fmt(widget.editing?.ingredientCaloriesPer100g ?? widget.prefill?.caloriesPer100g));
  late final _proteinController = TextEditingController(
      text: _fmt(widget.editing?.ingredientProteinPer100g ?? widget.prefill?.proteinPer100g));
  late final _carbsController = TextEditingController(
      text: _fmt(widget.editing?.ingredientCarbsPer100g ?? widget.prefill?.carbsPer100g));
  late final _fatController =
      TextEditingController(text: _fmt(widget.editing?.ingredientFatPer100g ?? widget.prefill?.fatPer100g));

  late String _name = widget.editing?.ingredientName ?? widget.prefill?.name ?? '';
  late String _unit = widget.editing?.unit ?? widget.prefill?.defaultUnit ?? 'unit';
  late String _location = widget.editing?.location ?? pantryLocations.first;

  // The ingredient this submission resolves against server-side: reused as-is if unchanged,
  // updated in place if it's already this household's custom ingredient, or forked into a new
  // custom one otherwise. Null means "no known ingredient yet" (a fresh typed name).
  late String? _baseIngredientId = widget.editing?.ingredientId ?? widget.prefill?.id;
  late bool _baseIsCustom = widget.editing?.ingredientCustom ?? widget.prefill?.custom ?? false;
  // Barcode/category of the last-picked Open Food Facts suggestion (no id of its own yet) --
  // only relevant on the fresh-name path, so a picked-then-untouched OFF match still enriches
  // the brand new global ingredient exactly like before.
  IngredientSuggestion? _lastOffSuggestion;

  bool get _isEditing => widget.editing != null;

  static String _fmt(double? v) {
    if (v == null) return '';
    return v == v.roundToDouble() ? v.toInt().toString() : v.toString();
  }

  @override
  void dispose() {
    _quantityController.dispose();
    _imageUrlController.dispose();
    _caloriesController.dispose();
    _proteinController.dispose();
    _carbsController.dispose();
    _fatController.dispose();
    super.dispose();
  }

  void _onSuggestionSelected(IngredientSuggestion? s) {
    // IngredientNameField also calls this with null on every keystroke to invalidate a stale
    // pick; ignore that here so retyping a name (to rename/correct it) doesn't detach the
    // in-progress edit from its base ingredient or wipe the nutrition fields the user can see.
    if (s == null) return;
    setState(() {
      _baseIngredientId = s.id;
      _baseIsCustom = s.custom;
      _lastOffSuggestion = s.id == null ? s : null;
      _imageUrlController.text = s.imageUrl ?? '';
      _caloriesController.text = _fmt(s.caloriesPer100g);
      _proteinController.text = _fmt(s.proteinPer100g);
      _carbsController.text = _fmt(s.carbsPer100g);
      _fatController.text = _fmt(s.fatPer100g);
    });
  }

  String? _emptyToNull(String s) => s.trim().isEmpty ? null : s.trim();

  void _submit() {
    final quantity = double.tryParse(_quantityController.text);
    if (_name.trim().isEmpty || quantity == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a name and a valid quantity.')),
      );
      return;
    }
    final appState = context.read<AppState>();
    final imageUrl = _emptyToNull(_imageUrlController.text);
    final calories = double.tryParse(_caloriesController.text);
    final protein = double.tryParse(_proteinController.text);
    final carbs = double.tryParse(_carbsController.text);
    final fat = double.tryParse(_fatController.text);

    if (_isEditing) {
      appState.updatePantryItem(
        widget.editing!,
        location: _location,
        quantity: quantity,
        unit: _unit,
        ingredientName: _name.trim(),
        ingredientImageUrl: imageUrl,
        ingredientCaloriesPer100g: calories,
        ingredientProteinPer100g: protein,
        ingredientCarbsPer100g: carbs,
        ingredientFatPer100g: fat,
      );
    } else {
      appState.addPantryItem(
        ingredientId: _baseIngredientId,
        ingredientName: _name.trim(),
        ingredientBarcode: _baseIngredientId == null ? _lastOffSuggestion?.barcode : null,
        ingredientCategory: _baseIngredientId == null ? _lastOffSuggestion?.category : null,
        ingredientImageUrl: imageUrl,
        ingredientCaloriesPer100g: calories,
        ingredientProteinPer100g: protein,
        ingredientCarbsPer100g: carbs,
        ingredientFatPer100g: fat,
        location: _location,
        quantity: quantity,
        unit: _unit,
      );
    }
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final rawImageUrl = _imageUrlController.text.trim();
    final imageUrl = ApiConfig.resolveImageUrl(rawImageUrl.isEmpty ? null : rawImageUrl);

    return Scaffold(
      appBar: AppBar(title: Text(_isEditing ? 'Edit pantry item' : 'Add pantry item')),
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
            if (_baseIsCustom) ...[
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.center,
                child: Chip(
                  visualDensity: VisualDensity.compact,
                  avatar: const Icon(Icons.edit, size: 16),
                  label: const Text('Custom ingredient'),
                ),
              ),
            ],
            const SizedBox(height: 16),
            IngredientNameField(
              initialValue: _name,
              onNameChanged: (v) => _name = v,
              onSuggestionSelected: _onSuggestionSelected,
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
            const SizedBox(height: 20),
            Text('Location', style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            SegmentedButton<String>(
              showSelectedIcon: false,
              segments: pantryLocations
                  .map((l) => ButtonSegment(value: l, label: Text(_titleCase(l))))
                  .toList(),
              selected: {_location},
              onSelectionChanged: (s) => setState(() => _location = s.first),
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _quantityController,
              decoration: const InputDecoration(labelText: 'Quantity'),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
            ),
            const SizedBox(height: 12),
            UnitDropdown(value: _unit, onChanged: (v) => setState(() => _unit = v)),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _submit,
              child: Text(_isEditing ? 'Save changes' : 'Add to pantry'),
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

  String _titleCase(String s) => s.isEmpty ? s : s[0] + s.substring(1).toLowerCase();
}
