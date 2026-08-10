import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/missing_ingredient.dart';
import '../state/app_state.dart';
import '../widgets/unit_dropdown.dart';

class ShoppingListScreen extends StatefulWidget {
  const ShoppingListScreen({super.key});

  @override
  State<ShoppingListScreen> createState() => _ShoppingListScreenState();
}

class _ShoppingListScreenState extends State<ShoppingListScreen> {
  bool _loaded = false;
  final Set<String> _selectedMissing = {};

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      final s = context.read<AppState>();
      Future.microtask(() {
        s.loadShoppingList();
        s.loadShoppingPreferences().then((_) => s.loadMissingIngredients());
      });
    }

    final unchecked = appState.shoppingList.where((i) => !i.checked).toList();
    final checked = appState.shoppingList.where((i) => i.checked).toList();
    final missing = appState.missingIngredients;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Shopping list'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Regenerate from next 14 days of meal plan',
            onPressed: () {
              final today = DateTime.now();
              final start = DateTime(today.year, today.month, today.day);
              context.read<AppState>().generateShoppingList(start, start.add(const Duration(days: 13)));
              context.read<AppState>().loadMissingIngredients();
            },
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          await context.read<AppState>().loadShoppingList();
          await context.read<AppState>().loadMissingIngredients();
        },
        child: ListView(
          children: [
            if (missing.isNotEmpty) ..._buildMissingSection(context, appState, missing),
            if (appState.shoppingList.isEmpty && missing.isEmpty)
              const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: Text('Nothing here yet. Tap refresh to generate from your meal plan, or + to add manually.')),
              ),
            ...unchecked.map((item) => CheckboxListTile(
                  value: item.checked,
                  title: Text(item.ingredientName ?? '(unknown)'),
                  subtitle: Text('${item.quantity} ${item.unit}${item.source == 'MANUAL' ? ' · manual' : ''}'),
                  secondary: IconButton(
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () => context.read<AppState>().deleteShoppingListItem(item.id),
                  ),
                  onChanged: (v) => context.read<AppState>().toggleShoppingListItem(item.id, v ?? false),
                )),
            if (checked.isNotEmpty) const Padding(padding: EdgeInsets.fromLTRB(16, 16, 16, 4), child: Text('Checked', style: TextStyle(color: Colors.grey))),
            ...checked.map((item) => CheckboxListTile(
                  value: item.checked,
                  title: Text(item.ingredientName ?? '(unknown)', style: const TextStyle(decoration: TextDecoration.lineThrough)),
                  subtitle: Text('${item.quantity} ${item.unit}'),
                  secondary: IconButton(
                    icon: const Icon(Icons.delete_outline),
                    onPressed: () => context.read<AppState>().deleteShoppingListItem(item.id),
                  ),
                  onChanged: (v) => context.read<AppState>().toggleShoppingListItem(item.id, v ?? false),
                )),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddDialog(context),
        child: const Icon(Icons.add),
      ),
    );
  }

  /// "Missing for future recipes" -- ingredients needed by meal plans over the next N days
  /// (N = the "Number of days to check missing ingredients" preference) that aren't sufficiently
  /// stocked. Swipe right to add to the shopping list, swipe left to ignore; long-press to
  /// multi-select and use the "Ignore all"/"Add all" bar instead.
  List<Widget> _buildMissingSection(BuildContext context, AppState appState, List<MissingIngredient> missing) {
    return [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
        child: Text(
          'Missing for future recipes (next ${appState.shoppingListLookaheadDays} days)',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
      const Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 4),
        child: Text('Swipe right to add, left to ignore.', style: TextStyle(color: Colors.grey, fontSize: 12)),
      ),
      if (_selectedMissing.isNotEmpty)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: Row(
            children: [
              Expanded(child: Text('${_selectedMissing.length} selected', style: const TextStyle(fontWeight: FontWeight.w500))),
              TextButton(onPressed: () => _ignoreSelected(context, missing), child: const Text('Ignore all')),
              TextButton(onPressed: () => _addSelected(context, missing), child: const Text('Add all to list')),
            ],
          ),
        ),
      ...missing.map((item) {
        final selected = _selectedMissing.contains(item.key);
        return Dismissible(
          key: ValueKey(item.key),
          direction: DismissDirection.horizontal,
          background: Container(
            color: Colors.green,
            alignment: Alignment.centerLeft,
            padding: const EdgeInsets.only(left: 20),
            child: const Icon(Icons.add_shopping_cart, color: Colors.white),
          ),
          secondaryBackground: Container(
            color: Colors.grey,
            alignment: Alignment.centerRight,
            padding: const EdgeInsets.only(right: 20),
            child: const Icon(Icons.visibility_off, color: Colors.white),
          ),
          onDismissed: (direction) {
            if (direction == DismissDirection.startToEnd) {
              context.read<AppState>().addMissingIngredientsToList([item]);
            } else {
              context.read<AppState>().ignoreMissingIngredients([item]);
            }
            setState(() => _selectedMissing.remove(item.key));
          },
          child: ListTile(
            dense: true,
            selected: selected,
            selectedTileColor: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.4),
            leading: Icon(selected ? Icons.check_circle : Icons.warning_amber_rounded, color: selected ? Theme.of(context).colorScheme.primary : Colors.orange),
            title: Text(item.ingredientName ?? '(ingredient)'),
            subtitle: Text('${item.quantity} ${item.unit}'),
            onLongPress: () => setState(() {
              if (selected) {
                _selectedMissing.remove(item.key);
              } else {
                _selectedMissing.add(item.key);
              }
            }),
          ),
        );
      }),
      const Divider(),
    ];
  }

  Future<void> _ignoreSelected(BuildContext context, List<MissingIngredient> missing) async {
    final items = missing.where((m) => _selectedMissing.contains(m.key)).toList();
    await context.read<AppState>().ignoreMissingIngredients(items);
    if (mounted) setState(_selectedMissing.clear);
  }

  Future<void> _addSelected(BuildContext context, List<MissingIngredient> missing) async {
    final items = missing.where((m) => _selectedMissing.contains(m.key)).toList();
    await context.read<AppState>().addMissingIngredientsToList(items);
    if (mounted) setState(_selectedMissing.clear);
  }

  void _showAddDialog(BuildContext context) {
    final nameController = TextEditingController();
    final quantityController = TextEditingController(text: '1');
    String unit = 'unit';

    showDialog(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setState) => AlertDialog(
          title: const Text('Add item'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: nameController, decoration: const InputDecoration(labelText: 'Item')),
              TextField(
                controller: quantityController,
                decoration: const InputDecoration(labelText: 'Quantity'),
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
              ),
              UnitDropdown(value: unit, onChanged: (v) => setState(() => unit = v)),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cancel')),
            FilledButton(
              onPressed: () {
                final quantity = double.tryParse(quantityController.text);
                if (nameController.text.trim().isEmpty || quantity == null) return;
                Navigator.pop(dialogContext);
                context.read<AppState>().addShoppingListItem(
                      ingredientName: nameController.text.trim(),
                      quantity: quantity,
                      unit: unit,
                    );
              },
              child: const Text('Add'),
            ),
          ],
        ),
      ),
    );
  }
}
