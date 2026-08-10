import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../state/app_state.dart';
import 'add_pantry_item_screen.dart';
import 'barcode_scanner_screen.dart';
import 'custom_ingredients_screen.dart';
import 'household_switcher_screen.dart';

class PantryScreen extends StatelessWidget {
  const PantryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final household = appState.currentHousehold!;

    return Scaffold(
      appBar: AppBar(
        title: Text(household.name),
        actions: [
          IconButton(
            icon: const Icon(Icons.group),
            tooltip: 'Invite code: ${household.inviteCode}',
            onPressed: () => showDialog(
              context: context,
              builder: (_) => AlertDialog(
                title: const Text('Invite code'),
                content: SelectableText(household.inviteCode),
                actions: [
                  TextButton(onPressed: () => Navigator.pop(context), child: const Text('Close')),
                ],
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.qr_code_scanner),
            tooltip: 'Scan barcode',
            onPressed: () => _scanAndAdd(context),
          ),
          IconButton(
            icon: const Icon(Icons.edit_note),
            tooltip: 'Custom ingredients',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const CustomIngredientsScreen()),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.swap_horiz),
            tooltip: 'Switch household',
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const HouseholdSwitcherScreen()),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => context.read<AppState>().signOut(),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => context.read<AppState>().loadPantry(),
        child: appState.pantryItems.isEmpty
            ? ListView(
                children: const [
                  Padding(
                    padding: EdgeInsets.all(32),
                    child: Center(child: Text('No pantry items yet. Tap + to add one.')),
                  ),
                ],
              )
            : ListView.builder(
                itemCount: appState.pantryItems.length,
                itemBuilder: (context, index) {
                  final item = appState.pantryItems[index];
                  final imageUrl = ApiConfig.resolveImageUrl(item.ingredientImageUrl);
                  return Dismissible(
                    key: ValueKey(item.id),
                    background: Container(color: Colors.red, alignment: Alignment.centerRight, padding: const EdgeInsets.only(right: 20), child: const Icon(Icons.delete, color: Colors.white)),
                    direction: DismissDirection.endToStart,
                    onDismissed: (_) => context.read<AppState>().deletePantryItem(item.id),
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
                          : null,
                      title: Row(
                        children: [
                          Flexible(child: Text(item.ingredientName ?? '(unknown ingredient)', overflow: TextOverflow.ellipsis)),
                          if (item.ingredientCustom) ...[
                            const SizedBox(width: 6),
                            Icon(Icons.edit, size: 14, color: Theme.of(context).colorScheme.primary),
                          ],
                        ],
                      ),
                      subtitle: Text('${item.location} · ${item.quantity} ${item.unit}'
                          '${item.expirationDate != null ? ' · exp ${item.expirationDate}' : ''}'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => AddPantryItemScreen(editing: item)),
                      ),
                    ),
                  );
                },
              ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => const AddPantryItemScreen()),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }

  Future<void> _scanAndAdd(BuildContext context) async {
    final barcode = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const BarcodeScannerScreen()),
    );
    if (barcode == null || !context.mounted) return;

    final appState = context.read<AppState>();
    final ingredient = await appState.scanBarcode(barcode);
    if (!context.mounted) return;

    if (ingredient == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Barcode not recognized — add it manually instead.')),
      );
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AddPantryItemScreen()));
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => AddPantryItemScreen(prefill: ingredient)),
    );
  }
}
