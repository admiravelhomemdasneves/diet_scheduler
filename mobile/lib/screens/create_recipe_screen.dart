import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';
import '../models/recipe.dart';
import '../models/taste.dart';
import '../state/app_state.dart';
import '../widgets/unit_dropdown.dart';

class _IngredientRow {
  final nameController = TextEditingController();
  final quantityController = TextEditingController(text: '1');
  String unit = 'unit';
}

class CreateRecipeScreen extends StatefulWidget {
  const CreateRecipeScreen({super.key});

  @override
  State<CreateRecipeScreen> createState() => _CreateRecipeScreenState();
}

class _CreateRecipeScreenState extends State<CreateRecipeScreen> {
  final _nameController = TextEditingController();
  final _instructionsController = TextEditingController();
  final _servingsController = TextEditingController(text: '2');
  final _prepController = TextEditingController();
  final _cookController = TextEditingController();
  String _category = recipeCategories.first;
  bool _isPrivate = false;
  bool _submitting = false;
  final List<_IngredientRow> _ingredients = [_IngredientRow()];
  final Set<String> _selectedTasteIds = {};
  XFile? _pickedPhoto;

  @override
  void initState() {
    super.initState();
    final appState = context.read<AppState>();
    if (appState.allTastes.isEmpty) {
      appState.loadReferenceData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final tastesByType = <String, List<Taste>>{};
    for (final t in appState.allTastes) {
      tastesByType.putIfAbsent(t.type, () => []).add(t);
    }

    return Scaffold(
      appBar: AppBar(title: const Text('New recipe')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          GestureDetector(
            onTap: _pickPhoto,
            child: _pickedPhoto == null
                ? Container(
                    height: 140,
                    width: double.infinity,
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.add_a_photo_outlined, size: 32),
                          SizedBox(height: 4),
                          Text('Add a photo (optional)'),
                        ],
                      ),
                    ),
                  )
                : ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.file(File(_pickedPhoto!.path), height: 140, width: double.infinity, fit: BoxFit.cover),
                  ),
          ),
          const SizedBox(height: 16),
          TextField(controller: _nameController, decoration: const InputDecoration(labelText: 'Name')),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            initialValue: _category,
            items: recipeCategories.map((c) => DropdownMenuItem(value: c, child: Text(c))).toList(),
            onChanged: (v) => setState(() => _category = v ?? _category),
            decoration: const InputDecoration(labelText: 'Category'),
          ),
          const SizedBox(height: 8),
          Row(children: [
            Expanded(
                child: TextField(
                    controller: _servingsController,
                    decoration: const InputDecoration(labelText: 'Servings'),
                    keyboardType: TextInputType.number)),
            const SizedBox(width: 8),
            Expanded(
                child: TextField(
                    controller: _prepController,
                    decoration: const InputDecoration(labelText: 'Prep (min)'),
                    keyboardType: TextInputType.number)),
            const SizedBox(width: 8),
            Expanded(
                child: TextField(
                    controller: _cookController,
                    decoration: const InputDecoration(labelText: 'Cook (min)'),
                    keyboardType: TextInputType.number)),
          ]),
          const SizedBox(height: 8),
          TextField(
            controller: _instructionsController,
            decoration: const InputDecoration(labelText: 'Instructions', alignLabelWithHint: true),
            maxLines: 4,
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Private (only visible to you)'),
            value: _isPrivate,
            onChanged: (v) => setState(() => _isPrivate = v),
          ),
          const SizedBox(height: 16),
          const Text('Ingredients', style: TextStyle(fontWeight: FontWeight.bold)),
          ..._ingredients.asMap().entries.map((entry) {
            final row = entry.value;
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(children: [
                Expanded(flex: 3, child: TextField(controller: row.nameController, decoration: const InputDecoration(labelText: 'Ingredient'))),
                const SizedBox(width: 8),
                Expanded(flex: 2, child: TextField(controller: row.quantityController, decoration: const InputDecoration(labelText: 'Qty'), keyboardType: const TextInputType.numberWithOptions(decimal: true))),
                const SizedBox(width: 8),
                Expanded(flex: 2, child: UnitDropdown(value: row.unit, onChanged: (v) => setState(() => row.unit = v))),
                IconButton(
                  icon: const Icon(Icons.remove_circle_outline),
                  onPressed: _ingredients.length == 1 ? null : () => setState(() => _ingredients.removeAt(entry.key)),
                ),
              ]),
            );
          }),
          TextButton.icon(
            onPressed: () => setState(() => _ingredients.add(_IngredientRow())),
            icon: const Icon(Icons.add),
            label: const Text('Add ingredient'),
          ),
          const SizedBox(height: 16),
          const Text('Tags', style: TextStyle(fontWeight: FontWeight.bold)),
          for (final type in tastesByType.keys) ...[
            Padding(padding: const EdgeInsets.only(top: 8, bottom: 4), child: Text(type, style: const TextStyle(color: Colors.grey))),
            Wrap(
              spacing: 8,
              children: tastesByType[type]!.map((t) {
                final selected = _selectedTasteIds.contains(t.id);
                return FilterChip(
                  label: Text(t.name),
                  selected: selected,
                  onSelected: (v) => setState(() => v ? _selectedTasteIds.add(t.id) : _selectedTasteIds.remove(t.id)),
                );
              }).toList(),
            ),
          ],
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: _submitting ? const CircularProgressIndicator() : const Text('Create recipe'),
          ),
          if (appState.errorMessage != null) ...[
            const SizedBox(height: 12),
            Text(appState.errorMessage!, style: const TextStyle(color: Colors.red)),
          ],
        ],
      ),
    );
  }

  Future<void> _pickPhoto() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_camera),
              title: const Text('Take photo'),
              onTap: () => Navigator.pop(sheetContext, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Choose from gallery'),
              onTap: () => Navigator.pop(sheetContext, ImageSource.gallery),
            ),
          ],
        ),
      ),
    );
    if (source == null) return;
    final picked = await ImagePicker().pickImage(source: source, maxWidth: 1600, imageQuality: 85);
    if (picked == null || !mounted) return;
    setState(() => _pickedPhoto = picked);
  }

  Future<void> _submit() async {
    final appState = context.read<AppState>();
    final servings = int.tryParse(_servingsController.text);
    if (_nameController.text.trim().isEmpty || servings == null) return;

    final ingredients = _ingredients
        .where((r) => r.nameController.text.trim().isNotEmpty)
        .map((r) => {
              'ingredientName': r.nameController.text.trim(),
              'quantity': double.tryParse(r.quantityController.text) ?? 1,
              'unit': r.unit,
            })
        .toList();
    if (ingredients.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Add at least one ingredient.')));
      return;
    }

    final tags = appState.allTastes
        .where((t) => _selectedTasteIds.contains(t.id))
        .map((t) => {'type': t.type, 'value': t.name})
        .toList();

    setState(() => _submitting = true);
    final created = await appState.createRecipe(
      name: _nameController.text.trim(),
      category: _category,
      instructions: _instructionsController.text.trim().isEmpty ? null : _instructionsController.text.trim(),
      servings: servings,
      prepTimeMinutes: int.tryParse(_prepController.text),
      cookTimeMinutes: int.tryParse(_cookController.text),
      isPrivate: _isPrivate,
      ingredients: ingredients,
      tags: tags,
    );
    if (created != null && _pickedPhoto != null) {
      await appState.uploadRecipeImage(created.id, File(_pickedPhoto!.path));
    }
    if (!mounted) return;
    setState(() => _submitting = false);
    if (created != null) Navigator.pop(context);
  }
}
