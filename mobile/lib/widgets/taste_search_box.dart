import 'package:flutter/material.dart';
import '../models/taste.dart';

/// Search-as-you-type box for adding taste preferences. New additions are tagged with whichever
/// preference category (favorite/liked/disliked/forbidden) is selected via the chip row above the
/// search field. Existing selections show as chips colored by their current category (see
/// [colorForTastePreference]), capped at 3 with a "see all" action once there are more.
class TasteSearchBox extends StatefulWidget {
  final List<Taste> allTastes;
  final List<TastePreferenceEntry> selected;
  final void Function(String tasteId, String preference) onSet;
  final ValueChanged<String> onRemove;
  final VoidCallback onSeeAll;

  const TasteSearchBox({
    super.key,
    required this.allTastes,
    required this.selected,
    required this.onSet,
    required this.onRemove,
    required this.onSeeAll,
  });

  @override
  State<TasteSearchBox> createState() => _TasteSearchBoxState();
}

class _TasteSearchBoxState extends State<TasteSearchBox> {
  final _controller = TextEditingController();
  final _focusNode = FocusNode();
  String _query = '';
  String _category = 'FAVORITE';

  @override
  void dispose() {
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final selectedIds = widget.selected.map((t) => t.tasteId).toSet();
    final query = _query.trim().toLowerCase();
    final matches = query.isEmpty
        ? const <Taste>[]
        : widget.allTastes.where((t) => !selectedIds.contains(t.id) && t.name.toLowerCase().contains(query)).take(6).toList();
    final showDropdown = _focusNode.hasFocus && matches.isNotEmpty;
    final shown = widget.selected.take(3).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text('Add as', style: TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 4),
        Wrap(
          spacing: 6,
          children: tastePreferences.map((p) {
            final selected = _category == p;
            return ChoiceChip(
              label: Text(tastePreferenceLabel(p)),
              selected: selected,
              selectedColor: colorForTastePreference(p).withValues(alpha: 0.35),
              onSelected: (_) => setState(() => _category = p),
            );
          }).toList(),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _controller,
          focusNode: _focusNode,
          decoration: const InputDecoration(labelText: 'Search tastes to add', prefixIcon: Icon(Icons.search), isDense: true),
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
                    .map((t) => ListTile(
                          dense: true,
                          title: Text(t.name),
                          subtitle: Text(t.type),
                          onTap: () {
                            widget.onSet(t.id, _category);
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
          const Text('No tastes added.', style: TextStyle(color: Colors.grey, fontSize: 12))
        else
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              for (final t in shown)
                Chip(
                  label: Text(t.tasteName),
                  backgroundColor: colorForTastePreference(t.preference).withValues(alpha: 0.3),
                  onDeleted: () => widget.onRemove(t.tasteId),
                ),
              if (widget.selected.length > 3)
                ActionChip(label: Text('See all (${widget.selected.length})'), onPressed: widget.onSeeAll),
            ],
          ),
      ],
    );
  }
}
