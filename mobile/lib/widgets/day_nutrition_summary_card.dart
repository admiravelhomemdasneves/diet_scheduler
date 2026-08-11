import 'package:flutter/material.dart';
import '../models/meal_plan.dart';
import '../theme/app_semantic_colors.dart';

/// Combined nutrition for a day's meal plan vs. the viewer's own daily targets -- shown on both
/// the day-detail page (reached from the calendar) and the "Today's meals" list view.
class DayNutritionSummaryCard extends StatelessWidget {
  final DayNutritionSummary summary;
  const DayNutritionSummaryCard({super.key, required this.summary});

  @override
  Widget build(BuildContext context) {
    final hasAnyTarget = summary.calorieTarget != null ||
        summary.proteinTarget != null ||
        summary.carbsTarget != null ||
        summary.fatTarget != null;

    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Combined nutrition today', style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            _row(context, 'Calories', summary.totalCalories, summary.calorieTarget, summary.calorieDelta, 'kcal'),
            _row(context, 'Protein', summary.totalProtein, summary.proteinTarget, summary.proteinDelta, 'g'),
            _row(context, 'Carbs', summary.totalCarbs, summary.carbsTarget, summary.carbsDelta, 'g'),
            _row(context, 'Fat', summary.totalFat, summary.fatTarget, summary.fatDelta, 'g'),
            if (!hasAnyTarget) ...[
              const SizedBox(height: 8),
              Text(
                'Set your daily nutrition targets in Preferences to see how this compares.',
                style: TextStyle(fontSize: 12, color: Theme.of(context).colorScheme.outline),
              ),
            ],
            if (summary.nutritionIncomplete) ...[
              const SizedBox(height: 8),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.warning_amber_rounded, size: 16, color: Theme.of(context).extension<AppSemanticColors>()!.warning),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      'One or more meals\' nutrition couldn\'t be fully determined, so this may be an underestimate.',
                      style: TextStyle(fontSize: 12, color: Theme.of(context).extension<AppSemanticColors>()!.warning),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _row(BuildContext context, String label, double total, double? target, double? delta, String unit) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label),
              Text('${_fmt(total)} $unit', style: const TextStyle(fontWeight: FontWeight.w600)),
            ],
          ),
          if (target != null)
            Text(
              '${_deltaLabel(delta!)} $unit ${_aboveBelowLabel(delta)} target (${_fmt(target)} $unit)',
              style: TextStyle(fontSize: 12, color: Theme.of(context).colorScheme.outline),
            ),
        ],
      ),
    );
  }

  String _deltaLabel(double delta) => _fmt(delta.abs());

  String _aboveBelowLabel(double delta) {
    if (delta > 0) return 'above';
    if (delta < 0) return 'below';
    return 'right at';
  }

  String _fmt(double v) => v == v.roundToDouble() ? v.toInt().toString() : v.toStringAsFixed(1);
}
