class UnitGroup {
  final String label;
  final List<String> units;
  const UnitGroup(this.label, this.units);
}

const List<String> _metricSolids = ['g', 'kg'];
const List<String> _imperialSolids = ['oz', 'lb'];
const List<String> _metricLiquids = ['ml', 'l'];
const List<String> _imperialLiquids = ['fl oz', 'qt', 'gal'];

/// Always shown regardless of the unit-system preference: widely used in recipes either way.
const List<String> culinaryMeasures = ['tsp', 'tbsp', 'cup', 'pint'];

/// Always shown: for countable, non-measured ingredients (e.g. "3 cloves", "2 eggs").
const List<String> countUnits = ['unit', 'piece', 'clove', 'slice'];

/// Builds the grouped unit list for a dropdown, gated by [unitSystem] ('METRIC'/'IMPERIAL'/'BOTH').
List<UnitGroup> unitGroupsFor(String unitSystem) {
  final showMetric = unitSystem == 'METRIC' || unitSystem == 'BOTH';
  final showImperial = unitSystem == 'IMPERIAL' || unitSystem == 'BOTH';

  return [
    UnitGroup('Solids', [
      if (showMetric) ..._metricSolids,
      if (showImperial) ..._imperialSolids,
    ]),
    UnitGroup('Liquids', [
      if (showMetric) ..._metricLiquids,
      if (showImperial) ..._imperialLiquids,
    ]),
    const UnitGroup('Culinary measures', culinaryMeasures),
    const UnitGroup('Count', countUnits),
  ];
}
