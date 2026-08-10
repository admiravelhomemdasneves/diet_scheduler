import 'package:flutter/material.dart';
import '../models/allergy.dart';

/// Search-as-you-type box for adding allergies (ingredient-type allergens only -- it filters
/// [allAllergies], a reference list separate from Taste's DIET/CUISINE/FLAVOR entries, so diets and
/// cuisines never surface here). Existing selections show as removable chips below the box, capped
/// at 3 with a "see all" action once there are more.
class AllergySearchBox extends StatefulWidget {
  final List<Allergy> allAllergies;
  final List<Allergy> selected;
  final ValueChanged<String> onAdd;
  final ValueChanged<String> onRemove;
  final VoidCallback onSeeAll;

  const AllergySearchBox({
    super.key,
    required this.allAllergies,
    required this.selected,
    required this.onAdd,
    required this.onRemove,
    required this.onSeeAll,
  });

  @override
  State<AllergySearchBox> createState() => _AllergySearchBoxState();
}

class _AllergySearchBoxState extends State<AllergySearchBox> {
  final _controller = TextEditingController();
  final _focusNode = FocusNode();
  String _query = '';

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final selectedIds = widget.selected.map((a) => a.id).toSet();
    final query = _query.trim().toLowerCase();
    final matches = query.isEmpty
        ? const <Allergy>[]
        : widget.allAllergies.where((a) => !selectedIds.contains(a.id) && a.name.toLowerCase().contains(query)).take(6).toList();
    final showDropdown = _focusNode.hasFocus && matches.isNotEmpty;
    final shown = widget.selected.take(3).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _controller,
          focusNode: _focusNode,
          decoration: const InputDecoration(labelText: 'Search allergies to add', prefixIcon: Icon(Icons.search), isDense: true),
          onChanged: (v) => setState(() => _query = v),
        ),
        if (showDropdown)
          Card(
            margin: const EdgeInsets.only(top: 4),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 180),
              child: ListView(
                shrinkWrap: true,
                padding: EdgeInsets.zero,
                children: matches
                    .map((a) => ListTile(
                          dense: true,
                          title: Text(a.name),
                          onTap: () {
                            widget.onAdd(a.id);
                            _controller.clear();
                            setState(() => _query = '');
                            _focusNode.unfocus();
                          },
                        ))
                    .toList(),
              ),
            ),
          ),
        const SizedBox(height: 8),
        if (widget.selected.isEmpty)
          const Text('No allergies added.', style: TextStyle(color: Colors.grey, fontSize: 12))
        else
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              for (final a in shown) Chip(label: Text(a.name), onDeleted: () => widget.onRemove(a.id)),
              if (widget.selected.length > 3)
                ActionChip(label: Text('See all (${widget.selected.length})'), onPressed: widget.onSeeAll),
            ],
          ),
      ],
    );
  }
}
