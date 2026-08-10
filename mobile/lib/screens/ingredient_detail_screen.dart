import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../models/ingredient.dart';
import '../state/app_state.dart';

/// Read-only ingredient detail view, linked to from recipe ingredient lists so a household can
/// see an ingredient's image/nutrition without leaving the recipe. Custom ingredients are managed
/// from the dedicated custom-ingredients page instead of from here.
///
/// Pass either [ingredientId] (fetched via GET /pantry/ingredients/{id} -- used for a recipe's own
/// saved ingredients, which always have one) or [initialData] (used for an ingredient with no
/// persisted id yet, e.g. one found by name on a not-yet-favorited external recipe -- see
/// AppState.lookupIngredientByName / Ingredient.fromSuggestion -- so no extra round trip is needed).
class IngredientDetailScreen extends StatefulWidget {
  final String? ingredientId;
  final Ingredient? initialData;
  final String? fallbackName;

  const IngredientDetailScreen({super.key, this.ingredientId, this.initialData, this.fallbackName})
      : assert(ingredientId != null || initialData != null, 'Either ingredientId or initialData is required');

  @override
  State<IngredientDetailScreen> createState() => _IngredientDetailScreenState();
}

class _IngredientDetailScreenState extends State<IngredientDetailScreen> {
  Ingredient? _ingredient;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    if (widget.initialData != null) {
      _ingredient = widget.initialData;
      _loading = false;
    } else {
      _load();
    }
  }

  Future<void> _load() async {
    final ingredient = await context.read<AppState>().getIngredient(widget.ingredientId!);
    if (!mounted) return;
    setState(() {
      _ingredient = ingredient;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final ingredient = _ingredient;
    return Scaffold(
      appBar: AppBar(title: Text(ingredient?.name ?? widget.fallbackName ?? 'Ingredient')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ingredient == null
              ? const Center(child: Text('Could not load this ingredient.'))
              : _buildBody(context, ingredient),
    );
  }

  Widget _buildBody(BuildContext context, Ingredient ingredient) {
    final imageUrl = ApiConfig.resolveImageUrl(ingredient.imageUrl);
    final hasNutrition = ingredient.caloriesPer100g != null ||
        ingredient.proteinPer100g != null ||
        ingredient.carbsPer100g != null ||
        ingredient.fatPer100g != null;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Center(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: imageUrl != null
                ? Image.network(
                    imageUrl,
                    width: 140,
                    height: 140,
                    fit: BoxFit.cover,
                    errorBuilder: (_, _, _) => _imagePlaceholder(context),
                  )
                : _imagePlaceholder(context),
          ),
        ),
        const SizedBox(height: 16),
        Center(
          child: Text(ingredient.name, style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
        ),
        if (ingredient.category != null) ...[
          const SizedBox(height: 4),
          Center(
            child: Text(ingredient.category!, style: TextStyle(color: Theme.of(context).colorScheme.outline)),
          ),
        ],
        if (ingredient.custom) ...[
          const SizedBox(height: 8),
          Center(
            child: Chip(
              visualDensity: VisualDensity.compact,
              avatar: const Icon(Icons.edit, size: 16),
              label: const Text('Custom ingredient'),
            ),
          ),
        ],
        const SizedBox(height: 24),
        Text('Nutrition per 100g', style: Theme.of(context).textTheme.titleSmall),
        const SizedBox(height: 8),
        if (!hasNutrition)
          const Text('No nutrition data available for this ingredient.')
        else
          Card(
            margin: EdgeInsets.zero,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  _NutritionRow(label: 'Calories', value: ingredient.caloriesPer100g, unit: 'kcal'),
                  _NutritionRow(label: 'Protein', value: ingredient.proteinPer100g, unit: 'g'),
                  _NutritionRow(label: 'Carbs', value: ingredient.carbsPer100g, unit: 'g'),
                  _NutritionRow(label: 'Fat', value: ingredient.fatPer100g, unit: 'g'),
                ],
              ),
            ),
          ),
      ],
    );
  }

  Widget _imagePlaceholder(BuildContext context) => Container(
        width: 140,
        height: 140,
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        child: const Icon(Icons.image_not_supported_outlined, size: 40),
      );
}

class _NutritionRow extends StatelessWidget {
  final String label;
  final double? value;
  final String unit;

  const _NutritionRow({required this.label, required this.value, required this.unit});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label),
          Text(value != null ? '${_fmt(value!)} $unit' : '—', style: const TextStyle(fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  String _fmt(double v) => v == v.roundToDouble() ? v.toInt().toString() : v.toStringAsFixed(1);
}
