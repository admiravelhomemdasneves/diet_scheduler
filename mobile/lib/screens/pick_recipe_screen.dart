import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/recipe.dart';
import '../state/app_state.dart';

/// Pushed to pick a recipe for a meal slot; pops with the chosen Recipe, or null if cancelled.
class PickRecipeScreen extends StatefulWidget {
  const PickRecipeScreen({super.key});

  @override
  State<PickRecipeScreen> createState() => _PickRecipeScreenState();
}

class _PickRecipeScreenState extends State<PickRecipeScreen> {
  String _stockFilter = 'NONE';

  @override
  void initState() {
    super.initState();
    context.read<AppState>().loadRecipes(stockFilter: _stockFilter);
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pick a recipe'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'NONE', label: Text('All')),
                ButtonSegment(value: 'PRIORITIZE', label: Text('Stock first')),
                ButtonSegment(value: 'ONLY_STOCK', label: Text('In stock only')),
              ],
              selected: {_stockFilter},
              onSelectionChanged: (s) {
                setState(() => _stockFilter = s.first);
                context.read<AppState>().loadRecipes(stockFilter: _stockFilter);
              },
            ),
          ),
        ),
      ),
      body: appState.recipes.isEmpty
          ? const Center(child: Text('No matching recipes.'))
          : ListView.builder(
              itemCount: appState.recipes.length,
              itemBuilder: (context, index) {
                final Recipe recipe = appState.recipes[index];
                final coverage = recipe.stockCoverage;
                return ListTile(
                  title: Text(recipe.name),
                  subtitle: Text('${recipe.category} · ${recipe.servings} servings'
                      '${coverage != null ? ' · ${(coverage * 100).round()}% in stock' : ''}'),
                  onTap: () => Navigator.of(context).pop(recipe),
                );
              },
            ),
    );
  }
}
