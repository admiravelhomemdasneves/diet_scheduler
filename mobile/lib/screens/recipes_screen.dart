import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../state/app_state.dart';
import 'create_recipe_screen.dart';
import 'recipe_detail_screen.dart';

class RecipesScreen extends StatefulWidget {
  const RecipesScreen({super.key});

  @override
  State<RecipesScreen> createState() => _RecipesScreenState();
}

class _RecipesScreenState extends State<RecipesScreen> {
  final _tagController = TextEditingController();
  bool _loaded = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      final s = context.read<AppState>();
      Future.microtask(() => s.loadRecipes());
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Favourite Recipes')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(children: [
              Expanded(
                child: TextField(
                  controller: _tagController,
                  decoration: const InputDecoration(
                    labelText: 'Filter by tags (comma separated)',
                    isDense: true,
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              IconButton(
                icon: const Icon(Icons.search),
                onPressed: () {
                  final tags = _tagController.text.split(',').map((t) => t.trim()).where((t) => t.isNotEmpty).toList();
                  context.read<AppState>().loadRecipes(tags: tags.isEmpty ? null : tags);
                },
              ),
            ]),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => context.read<AppState>().loadRecipes(),
              child: appState.recipes.isEmpty
                  ? ListView(children: const [Padding(padding: EdgeInsets.all(32), child: Center(child: Text('No recipes match. Tap + to add one.')))])
                  : ListView.builder(
                      itemCount: appState.recipes.length,
                      itemBuilder: (context, index) {
                        final recipe = appState.recipes[index];
                        final imageUrl = ApiConfig.resolveImageUrl(recipe.imageUrl);
                        return ListTile(
                          leading: imageUrl != null
                              ? ClipRRect(
                                  borderRadius: BorderRadius.circular(4),
                                  child: Image.network(
                                    imageUrl,
                                    width: 48,
                                    height: 48,
                                    fit: BoxFit.cover,
                                    errorBuilder: (_, _, _) => const Icon(Icons.restaurant),
                                  ),
                                )
                              : (recipe.isPrivate ? const Icon(Icons.lock_outline) : const Icon(Icons.restaurant)),
                          title: Text(recipe.name),
                          subtitle: Text('${recipe.category} · ${recipe.servings} servings'
                              '${recipe.caloriesPerServing != null ? ' · ${recipe.caloriesPerServing!.toStringAsFixed(0)} kcal' : ''}'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => Navigator.of(context).push(
                            MaterialPageRoute(builder: (_) => RecipeDetailScreen(recipe: recipe)),
                          ),
                        );
                      },
                    ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CreateRecipeScreen()));
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
