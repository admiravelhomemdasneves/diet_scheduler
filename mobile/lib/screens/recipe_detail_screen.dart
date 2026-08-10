import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../models/recipe.dart';
import '../state/app_state.dart';
import 'ingredient_detail_screen.dart';

class RecipeDetailScreen extends StatefulWidget {
  final Recipe recipe;
  const RecipeDetailScreen({super.key, required this.recipe});

  @override
  State<RecipeDetailScreen> createState() => _RecipeDetailScreenState();
}

class _RecipeDetailScreenState extends State<RecipeDetailScreen> {
  bool _uploadingPhoto = false;

  Future<void> _pickAndUploadPhoto(String recipeId) async {
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

    setState(() => _uploadingPhoto = true);
    final ok = await context.read<AppState>().uploadRecipeImage(recipeId, File(picked.path));
    if (!mounted) return;
    setState(() => _uploadingPhoto = false);
    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Could not save the photo.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    // Look the recipe up fresh so an uploaded photo (or other change) shows immediately.
    final recipe = appState.recipes.firstWhere((r) => r.id == widget.recipe.id, orElse: () => widget.recipe);
    final isOwner = recipe.createdByUserId != null && recipe.createdByUserId == appState.currentUser?.id;
    final imageUrl = ApiConfig.resolveImageUrl(recipe.imageUrl);

    return Scaffold(
      appBar: AppBar(
        title: Text(recipe.name),
        actions: [
          if (isOwner)
            IconButton(
              icon: _uploadingPhoto
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.add_a_photo_outlined),
              tooltip: imageUrl != null ? 'Change photo' : 'Add photo',
              onPressed: _uploadingPhoto ? null : () => _pickAndUploadPhoto(recipe.id),
            ),
          if (isOwner)
            IconButton(
              icon: const Icon(Icons.delete),
              onPressed: () async {
                await context.read<AppState>().deleteRecipe(recipe.id);
                if (context.mounted) Navigator.pop(context);
              },
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (imageUrl != null) ...[
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: AspectRatio(
                aspectRatio: 16 / 9,
                child: Image.network(
                  imageUrl,
                  fit: BoxFit.cover,
                  errorBuilder: (_, _, _) => const ColoredBox(color: Colors.black12, child: Icon(Icons.restaurant, size: 48)),
                ),
              ),
            ),
            const SizedBox(height: 16),
          ],
          Wrap(spacing: 8, children: [
            Chip(label: Text(recipe.category)),
            if (recipe.isPrivate) const Chip(label: Text('Private'), avatar: Icon(Icons.lock, size: 16)),
            ...recipe.tags.map((t) => Chip(label: Text('${t.type}: ${t.value}'))),
          ]),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _Stat(label: 'Servings', value: '${recipe.servings}'),
              if (recipe.prepTimeMinutes != null) _Stat(label: 'Prep', value: '${recipe.prepTimeMinutes} min'),
              if (recipe.cookTimeMinutes != null) _Stat(label: 'Cook', value: '${recipe.cookTimeMinutes} min'),
            ],
          ),
          if (recipe.caloriesPerServing != null) ...[
            const SizedBox(height: 16),
            Text('Per serving: ${recipe.caloriesPerServing?.toStringAsFixed(0)} kcal'
                '${recipe.proteinPerServing != null ? ' · ${recipe.proteinPerServing!.toStringAsFixed(0)}g protein' : ''}'
                '${recipe.carbsPerServing != null ? ' · ${recipe.carbsPerServing!.toStringAsFixed(0)}g carbs' : ''}'
                '${recipe.fatPerServing != null ? ' · ${recipe.fatPerServing!.toStringAsFixed(0)}g fat' : ''}'),
          ] else if (recipe.estimatedCaloriesPerServing != null) ...[
            const SizedBox(height: 16),
            Text('Per serving (estimated from ingredients): ${recipe.estimatedCaloriesPerServing!.toStringAsFixed(0)} kcal'
                '${recipe.estimatedProteinPerServing != null ? ' · ${recipe.estimatedProteinPerServing!.toStringAsFixed(0)}g protein' : ''}'
                '${recipe.estimatedCarbsPerServing != null ? ' · ${recipe.estimatedCarbsPerServing!.toStringAsFixed(0)}g carbs' : ''}'
                '${recipe.estimatedFatPerServing != null ? ' · ${recipe.estimatedFatPerServing!.toStringAsFixed(0)}g fat' : ''}'),
            if (recipe.nutritionEstimateIncomplete) ...[
              const SizedBox(height: 4),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.warning_amber_rounded, size: 16, color: Colors.orange.shade800),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      'May be an underestimate -- one or more ingredients are missing nutrition data '
                      'or use a unit that can\'t be converted for this calculation.',
                      style: TextStyle(fontSize: 12, color: Colors.orange.shade800),
                    ),
                  ),
                ],
              ),
            ],
          ],
          const SizedBox(height: 24),
          const Text('Ingredients', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 8),
          ...recipe.ingredients.map((i) {
            return InkWell(
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => IngredientDetailScreen(ingredientId: i.ingredientId, fallbackName: i.ingredientName),
                ),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 3),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('• ${i.quantity} ${i.unit} '),
                    Flexible(
                      child: Text(
                        i.ingredientName ?? '(ingredient)',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.primary,
                          decoration: TextDecoration.underline,
                          decorationColor: Theme.of(context).colorScheme.primary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          }),
          if (recipe.instructions != null && recipe.instructions!.isNotEmpty) ...[
            const SizedBox(height: 24),
            const Text('Instructions', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 8),
            Text(recipe.instructions!),
          ],
        ],
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  final String label;
  final String value;
  const _Stat({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }
}
