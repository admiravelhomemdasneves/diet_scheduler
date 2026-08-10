import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/units.dart';
import '../state/app_state.dart';

const _headerPrefix = '__unit_group_header__';

/// Grouped unit picker (Solids / Liquids / Culinary measures / Count), filtered by the
/// user's unit-system preference (Metric/Imperial/Both). Falls back to showing the current
/// value verbatim if it isn't one of the known units (e.g. legacy free-text data).
class UnitDropdown extends StatelessWidget {
  final String value;
  final ValueChanged<String> onChanged;
  final String? labelText;

  const UnitDropdown({super.key, required this.value, required this.onChanged, this.labelText = 'Unit'});

  @override
  Widget build(BuildContext context) {
    final unitSystem = context.watch<AppState>().unitSystem ?? 'METRIC';
    final groups = unitGroupsFor(unitSystem);
    final knownUnits = groups.expand((g) => g.units).toSet();

    final items = <DropdownMenuItem<String>>[];
    for (final group in groups) {
      if (group.units.isEmpty) continue;
      items.add(DropdownMenuItem<String>(
        enabled: false,
        value: '$_headerPrefix${group.label}',
        child: Text(group.label, style: TextStyle(fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.outline)),
      ));
      items.addAll(group.units.map((u) => DropdownMenuItem<String>(
            value: u,
            child: Padding(padding: const EdgeInsets.only(left: 12), child: Text(u)),
          )));
    }
    if (!knownUnits.contains(value)) {
      items.add(DropdownMenuItem<String>(value: value, child: Text(value)));
    }

    return DropdownButtonFormField<String>(
      initialValue: value,
      isExpanded: true,
      decoration: InputDecoration(labelText: labelText),
      items: items,
      onChanged: (v) {
        if (v == null || v.startsWith(_headerPrefix)) return;
        onChanged(v);
      },
    );
  }
}
