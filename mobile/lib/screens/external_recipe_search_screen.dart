import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/external_recipe.dart';
import '../state/app_state.dart';
import 'external_recipe_detail_screen.dart';
import 'recipes_screen.dart';

class ExternalRecipeSearchScreen extends StatefulWidget {
  const ExternalRecipeSearchScreen({super.key});

  @override
  State<ExternalRecipeSearchScreen> createState() => _ExternalRecipeSearchScreenState();
}

class _ExternalRecipeSearchScreenState extends State<ExternalRecipeSearchScreen> {
  final _queryController = TextEditingController();
  bool _searching = false;
  bool _searched = false;
  bool _gridView = true;
  final Set<String> _favoriting = {};

  @override
  void dispose() {
    _queryController.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    if (_queryController.text.trim().isEmpty) return;
    setState(() => _searching = true);
    await context.read<AppState>().searchExternalRecipes(_queryController.text);
    if (!mounted) return;
    setState(() {
      _searching = false;
      _searched = true;
    });
  }

  Future<void> _favorite(ExternalRecipeSummary summary) async {
    setState(() => _favoriting.add(summary.externalId));
    final ok = await context.read<AppState>().favoriteExternalRecipe(summary.externalId);
    if (!mounted) return;
    setState(() => _favoriting.remove(summary.externalId));
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(ok ? '"${summary.name}" added to your recipes.' : 'Could not save that recipe.'),
    ));
  }

  void _openDetail(ExternalRecipeSummary summary) {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => ExternalRecipeDetailScreen(summary: summary)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Recipes'),
        actions: [
          IconButton(
            icon: Icon(_gridView ? Icons.view_list : Icons.grid_view),
            tooltip: _gridView ? 'List view' : 'Grid view',
            onPressed: () => setState(() => _gridView = !_gridView),
          ),
          IconButton(
            icon: const Icon(Icons.favorite),
            tooltip: 'Favourite recipes',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const RecipesScreen()),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(children: [
              Expanded(
                child: TextField(
                  controller: _queryController,
                  decoration: const InputDecoration(labelText: 'Search by recipe name', isDense: true, border: OutlineInputBorder()),
                  textInputAction: TextInputAction.search,
                  onSubmitted: (_) => _search(),
                ),
              ),
              const SizedBox(width: 8),
              IconButton(icon: const Icon(Icons.search), onPressed: _search),
            ]),
          ),
          if (_searching) const Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator()),
          if (!_searching && _searched && appState.externalRecipeResults.isEmpty)
            const Padding(padding: EdgeInsets.all(32), child: Text('No recipes found. Try a different search term.')),
          Expanded(
            child: _gridView ? _buildGrid(appState) : _buildList(appState),
          ),
          if (appState.errorMessage != null)
            Padding(padding: const EdgeInsets.all(12), child: Text(appState.errorMessage!, style: const TextStyle(color: Colors.red))),
        ],
      ),
    );
  }

  Widget _buildList(AppState appState) {
    return ListView.builder(
      itemCount: appState.externalRecipeResults.length,
      itemBuilder: (context, index) {
        final summary = appState.externalRecipeResults[index];
        final isFavoriting = _favoriting.contains(summary.externalId);
        final isFavorited = appState.favoritedExternalIds.contains(summary.externalId);
        return ListTile(
          leading: summary.thumbnailUrl != null
              ? ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: Image.network(
                    summary.thumbnailUrl!,
                    width: 48,
                    height: 48,
                    fit: BoxFit.cover,
                    errorBuilder: (_, _, _) => const Icon(Icons.restaurant),
                  ),
                )
              : const Icon(Icons.restaurant),
          title: Text(summary.name),
          subtitle: Text([summary.category, summary.area].where((s) => s != null && s.isNotEmpty).join(' · ')),
          onTap: () => _openDetail(summary),
          trailing: isFavoriting
              ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
              : IconButton(
                  icon: Icon(isFavorited ? Icons.favorite : Icons.favorite_border, color: isFavorited ? Colors.red : null),
                  tooltip: isFavorited ? 'Already saved' : 'Save to my recipes',
                  onPressed: isFavorited ? null : () => _favorite(summary),
                ),
        );
      },
    );
  }

  Widget _buildGrid(AppState appState) {
    return GridView.builder(
      padding: const EdgeInsets.all(12),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: 12,
        mainAxisSpacing: 12,
        childAspectRatio: 1,
      ),
      itemCount: appState.externalRecipeResults.length,
      itemBuilder: (context, index) {
        final summary = appState.externalRecipeResults[index];
        return GestureDetector(
          onTap: () => _openDetail(summary),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: summary.thumbnailUrl != null
                ? Image.network(
                    summary.thumbnailUrl!,
                    fit: BoxFit.cover,
                    errorBuilder: (_, _, _) => _GridNameFallback(name: summary.name),
                  )
                : _GridNameFallback(name: summary.name),
          ),
        );
      },
    );
  }
}

class _GridNameFallback extends StatelessWidget {
  final String name;
  const _GridNameFallback({required this.name});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
      ),
      alignment: Alignment.center,
      padding: const EdgeInsets.all(12),
      child: Text(
        name,
        textAlign: TextAlign.center,
        maxLines: 3,
        overflow: TextOverflow.ellipsis,
        style: const TextStyle(fontWeight: FontWeight.w500),
      ),
    );
  }
}
