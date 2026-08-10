import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../models/external_recipe.dart';
import '../models/ingredient.dart';
import '../state/app_state.dart';
import 'ingredient_detail_screen.dart';

/// Read-only live preview of an external recipe -- nothing is saved until the user favorites it.
class ExternalRecipeDetailScreen extends StatefulWidget {
  final ExternalRecipeSummary summary;
  const ExternalRecipeDetailScreen({super.key, required this.summary});

  @override
  State<ExternalRecipeDetailScreen> createState() => _ExternalRecipeDetailScreenState();
}

class _ExternalRecipeDetailScreenState extends State<ExternalRecipeDetailScreen> {
  ExternalRecipeDetail? _detail;
  bool _loading = true;
  bool _favoriting = false;
  String? _searchingIngredientName;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final detail = await context.read<AppState>().fetchExternalRecipeDetail(widget.summary.externalId);
    if (!mounted) return;
    setState(() {
      _detail = detail;
      _loading = false;
    });
  }

  Future<void> _favorite() async {
    setState(() => _favoriting = true);
    final ok = await context.read<AppState>().favoriteExternalRecipe(widget.summary.externalId);
    if (!mounted) return;
    setState(() => _favoriting = false);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(ok ? '"${widget.summary.name}" added to your recipes.' : 'Could not save that recipe.'),
    ));
  }

  /// Not-yet-favorited external ingredients have no backend id -- look one up by name (best
  /// effort, since TheMealDB's free-text names don't always match Open Food Facts exactly) and
  /// show whatever's found without importing anything.
  Future<void> _openIngredient(ExternalRecipeIngredient ing) async {
    setState(() => _searchingIngredientName = ing.name);
    final results = await context.read<AppState>().lookupIngredientByName(ing.name);
    if (!mounted) return;
    setState(() => _searchingIngredientName = null);
    if (results.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('No ingredient match found for "${ing.name}".')),
      );
      return;
    }
    final match = results.firstWhere(
      (r) => r.name.toLowerCase() == ing.name.toLowerCase(),
      orElse: () => results.first,
    );
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => IngredientDetailScreen(initialData: Ingredient.fromSuggestion(match), fallbackName: ing.name),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final isFavorited = appState.favoritedExternalIds.contains(widget.summary.externalId);
    final imageUrl = ApiConfig.resolveImageUrl(_detail?.imageUrl ?? widget.summary.thumbnailUrl);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.summary.name),
        actions: [
          if (_favoriting)
            const Padding(
              padding: EdgeInsets.all(16),
              child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)),
            )
          else
            IconButton(
              icon: Icon(isFavorited ? Icons.favorite : Icons.favorite_border, color: isFavorited ? Colors.red : null),
              tooltip: isFavorited ? 'Already saved' : 'Save to my recipes',
              onPressed: isFavorited ? null : _favorite,
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _detail == null
              ? const Center(child: Padding(padding: EdgeInsets.all(32), child: Text('Could not load this recipe.')))
              : ListView(
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
                      Chip(label: Text(_detail!.category)),
                      ..._detail!.tags.map((t) => Chip(label: Text('${t.type}: ${t.value}'))),
                    ]),
                    const SizedBox(height: 16),
                    Center(
                      child: Column(children: [
                        Text('${_detail!.servings}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                        const Text('Servings', style: TextStyle(fontSize: 12, color: Colors.grey)),
                      ]),
                    ),
                    if (_detail!.estimatedCaloriesPerServing != null) ...[
                      const SizedBox(height: 16),
                      Text('Per serving (estimated from ingredients): '
                          '${_detail!.estimatedCaloriesPerServing!.toStringAsFixed(0)} kcal'
                          '${_detail!.estimatedProteinPerServing != null ? ' · ${_detail!.estimatedProteinPerServing!.toStringAsFixed(0)}g protein' : ''}'
                          '${_detail!.estimatedCarbsPerServing != null ? ' · ${_detail!.estimatedCarbsPerServing!.toStringAsFixed(0)}g carbs' : ''}'
                          '${_detail!.estimatedFatPerServing != null ? ' · ${_detail!.estimatedFatPerServing!.toStringAsFixed(0)}g fat' : ''}'),
                      if (_detail!.nutritionEstimateIncomplete) ...[
                        const SizedBox(height: 4),
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(Icons.warning_amber_rounded, size: 16, color: Colors.orange.shade800),
                            const SizedBox(width: 4),
                            Expanded(
                              child: Text(
                                'May be an underestimate -- one or more ingredients couldn\'t be matched, are '
                                'missing nutrition data, or use a unit that can\'t be converted for this calculation.',
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
                    ..._detail!.ingredients.map((i) {
                      final loading = _searchingIngredientName == i.name;
                      return InkWell(
                        onTap: loading ? null : () => _openIngredient(i),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 3),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('• ${i.quantity} ${i.unit} '),
                              Flexible(
                                child: Text(
                                  i.name,
                                  style: TextStyle(
                                    color: Theme.of(context).colorScheme.primary,
                                    decoration: TextDecoration.underline,
                                    decorationColor: Theme.of(context).colorScheme.primary,
                                  ),
                                ),
                              ),
                              if (loading) ...[
                                const SizedBox(width: 6),
                                const Padding(
                                  padding: EdgeInsets.only(top: 3),
                                  child: SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2)),
                                ),
                              ],
                            ],
                          ),
                        ),
                      );
                    }),
                    if (_detail!.instructions != null && _detail!.instructions!.isNotEmpty) ...[
                      const SizedBox(height: 24),
                      const Text('Instructions', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      const SizedBox(height: 8),
                      Text(_detail!.instructions!),
                    ],
                  ],
                ),
    );
  }
}
