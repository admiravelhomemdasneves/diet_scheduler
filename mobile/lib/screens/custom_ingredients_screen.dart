import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../state/app_state.dart';
import 'custom_ingredient_edit_screen.dart';

/// Lists the household's custom ingredients -- created explicitly here, or forked automatically
/// whenever a pantry item's ingredient name/image/nutrition is edited (see AddPantryItemScreen).
class CustomIngredientsScreen extends StatefulWidget {
  const CustomIngredientsScreen({super.key});

  @override
  State<CustomIngredientsScreen> createState() => _CustomIngredientsScreenState();
}

class _CustomIngredientsScreenState extends State<CustomIngredientsScreen> {
  bool _loaded = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      Future.microtask(() => context.read<AppState>().loadCustomIngredients());
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Custom ingredients')),
      body: RefreshIndicator(
        onRefresh: () => context.read<AppState>().loadCustomIngredients(),
        child: appState.customIngredients.isEmpty
            ? ListView(
                children: const [
                  Padding(
                    padding: EdgeInsets.all(32),
                    child: Center(
                      child: Text('No custom ingredients yet. Editing a pantry item\'s '
                          'ingredient details creates one automatically, or tap + to add one.'),
                    ),
                  ),
                ],
              )
            : ListView.builder(
                itemCount: appState.customIngredients.length,
                itemBuilder: (context, index) {
                  final ingredient = appState.customIngredients[index];
                  final imageUrl = ApiConfig.resolveImageUrl(ingredient.imageUrl);
                  return Dismissible(
                    key: ValueKey(ingredient.id),
                    background: Container(
                      color: Theme.of(context).colorScheme.error,
                      alignment: Alignment.centerRight,
                      padding: const EdgeInsets.only(right: 20),
                      child: Icon(Icons.delete, color: Theme.of(context).colorScheme.onError),
                    ),
                    direction: DismissDirection.endToStart,
                    confirmDismiss: (_) async {
                      // A failure here shows via the global error-stream listener at RootScreen,
                      // not a bespoke SnackBar -- deleteCustomIngredient still calls _setError.
                      return context.read<AppState>().deleteCustomIngredient(ingredient.id);
                    },
                    onDismissed: (_) {},
                    child: ListTile(
                      leading: imageUrl != null
                          ? ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: Image.network(
                                imageUrl,
                                width: 40,
                                height: 40,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => const Icon(Icons.image_not_supported_outlined),
                              ),
                            )
                          : const Icon(Icons.image_not_supported_outlined),
                      title: Text(ingredient.name),
                      subtitle: Text(ingredient.caloriesPer100g != null
                          ? '${ingredient.caloriesPer100g!.round()} kcal/100g'
                          : 'No nutrition data'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => CustomIngredientEditScreen(editing: ingredient)),
                      ),
                    ),
                  );
                },
              ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => const CustomIngredientEditScreen()),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }
}
