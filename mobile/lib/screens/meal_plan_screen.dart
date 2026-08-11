import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:table_calendar/table_calendar.dart';
import '../models/household.dart';
import '../models/meal_plan.dart';
import '../models/recipe.dart';
import '../state/app_state.dart';
import '../theme/app_semantic_colors.dart';
import '../widgets/auto_generate_dialog.dart';
import '../widgets/day_nutrition_summary_card.dart';
import 'day_meal_plan_screen.dart';
import 'pick_recipe_screen.dart';
import 'recipe_detail_screen.dart';

/// Single source of truth for how a calendar day's meal-plan marker is drawn, shared by the
/// calendar's own marker dots and the legend that explains them below it. Previously the legend
/// hardcoded its own literal-color copy of this (a hardcoded teal, independent of the actual
/// marker color), which silently drifted out of sync with the real marker color
/// (colorScheme.primary) -- a duplication bug, not a color-literal bug, so simply swapping the
/// legend's hardcoded colors for theme ones would have fixed today's mismatch while leaving the
/// same trap for the next palette change. Routing both call sites through this one function
/// makes that class of drift structurally impossible.
class MealMarkerStyle {
  final IconData icon;
  final Color color;
  const MealMarkerStyle(this.icon, this.color);
}

MealMarkerStyle mealMarkerStyle(BuildContext context, bool hasMeals) {
  final colorScheme = Theme.of(context).colorScheme;
  return hasMeals
      ? MealMarkerStyle(Icons.restaurant, colorScheme.primary)
      : MealMarkerStyle(Icons.circle_outlined, colorScheme.onSurfaceVariant);
}

class MealMarkerLegendSwatch extends StatelessWidget {
  final bool hasMeals;
  final String label;
  const MealMarkerLegendSwatch({required this.hasMeals, required this.label});

  @override
  Widget build(BuildContext context) {
    final style = mealMarkerStyle(context, hasMeals);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(style.icon, size: 12, color: style.color),
        const SizedBox(width: 4),
        Text(label, style: TextStyle(fontSize: 12, color: Theme.of(context).colorScheme.onSurfaceVariant)),
      ],
    );
  }
}

enum _ViewMode { list, calendar }

class MealPlanScreen extends StatefulWidget {
  const MealPlanScreen({super.key});

  @override
  State<MealPlanScreen> createState() => _MealPlanScreenState();
}

class _MealPlanScreenState extends State<MealPlanScreen> {
  late final DateTime _today;
  bool _loaded = false;
  _ViewMode _viewMode = _ViewMode.calendar;
  late DateTime _focusedMonth;
  DateTime? _selectedDay;
  DayNutritionSummary? _todaySummary;
  String? _todaySummaryKey;

  // Long-press selections: which meals (Today's-list view) or which calendar days are picked for a
  // targeted auto-fill/regenerate, instead of the default "all meals" / "next N days" behavior.
  final Set<String> _selectedMealTypes = {};
  final Set<DateTime> _selectedDays = {};

  @override
  void initState() {
    super.initState();
    final today = DateTime.now();
    _today = DateTime(today.year, today.month, today.day);
    _focusedMonth = _today;
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      final s = context.read<AppState>();
      Future.microtask(() => _viewMode == _ViewMode.calendar ? _loadMonth(_focusedMonth) : s.loadMealPlans(_today, _today));
    }

    // Refetch today's nutrition summary whenever today's meal plans actually change, rather than
    // on every rebuild (same signature-based pattern as the day-detail page).
    final todayStr = formatMealPlanDate(_today);
    final todaySummaryKey =
        appState.mealPlans.where((m) => m.date == todayStr).map((m) => '${m.id}:${m.cooked}').join(',');
    if (todaySummaryKey != _todaySummaryKey) {
      _todaySummaryKey = todaySummaryKey;
      Future.microtask(_loadTodaySummary);
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(_viewMode == _ViewMode.calendar ? 'Meal plan' : "Today's meals"),
        actions: [
          IconButton(
            icon: Icon(_viewMode == _ViewMode.list ? Icons.calendar_month : Icons.view_agenda),
            tooltip: _viewMode == _ViewMode.list ? 'Calendar view' : 'List view',
            onPressed: () => _setViewMode(_viewMode == _ViewMode.list ? _ViewMode.calendar : _ViewMode.list),
          ),
          IconButton(
            icon: const Icon(Icons.auto_awesome),
            tooltip: 'Auto-fill / regenerate',
            onPressed: () => _viewMode == _ViewMode.list ? _autoGenerateToday(context) : _autoGenerateCalendar(context),
          ),
        ],
      ),
      body: _viewMode == _ViewMode.list ? _buildListView(appState) : _buildCalendarView(appState),
    );
  }

  void _setViewMode(_ViewMode mode) {
    setState(() => _viewMode = mode);
    if (mode == _ViewMode.list) {
      context.read<AppState>().loadMealPlans(_today, _today);
    } else {
      _loadMonth(_focusedMonth);
    }
  }

  void _loadMonth(DateTime month) {
    final monthStart = DateTime(month.year, month.month, 1);
    final monthEnd = DateTime(month.year, month.month + 1, 0);
    context.read<AppState>().loadMealPlans(monthStart, monthEnd);
  }

  Future<void> _loadTodaySummary() async {
    final summary = await context.read<AppState>().getDayNutritionSummary(_today);
    if (!mounted) return;
    setState(() => _todaySummary = summary);
  }

  Widget _buildListView(AppState appState) {
    final dateStr = formatMealPlanDate(_today);
    final dayPlans = appState.mealPlans.where((m) => m.date == dateStr).toList();
    return RefreshIndicator(
      onRefresh: () => context.read<AppState>().loadMealPlans(_today, _today),
      child: ListView(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: Text(weekdayLabel(_today), style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
          if (_selectedMealTypes.isNotEmpty) _buildSelectionBar('${_selectedMealTypes.length} meal(s) selected', () => setState(_selectedMealTypes.clear)),
          if (_todaySummary != null) DayNutritionSummaryCard(summary: _todaySummary!),
          for (final mealType in mealTypes)
            MealSlot(
              day: _today,
              mealType: mealType,
              mealPlan: _findByMealType(dayPlans, mealType),
              selected: _selectedMealTypes.contains(mealType),
              onLongPress: (mt) => setState(() {
                if (_selectedMealTypes.contains(mt)) {
                  _selectedMealTypes.remove(mt);
                } else {
                  _selectedMealTypes.add(mt);
                }
              }),
            ),
        ],
      ),
    );
  }

  Widget _buildSelectionBar(String label, VoidCallback onClear) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          Expanded(child: Text(label, style: const TextStyle(fontWeight: FontWeight.w500))),
          TextButton(onPressed: onClear, child: const Text('Clear')),
        ],
      ),
    );
  }

  Widget _buildCalendarView(AppState appState) {
    return SingleChildScrollView(
      child: Column(
      children: [
        if (_selectedDays.isNotEmpty) _buildSelectionBar('${_selectedDays.length} day(s) selected', () => setState(_selectedDays.clear)),
        TableCalendar<MealPlan>(
          firstDay: DateTime.now().subtract(const Duration(days: 365)),
          lastDay: DateTime.now().add(const Duration(days: 365)),
          focusedDay: _focusedMonth,
          headerStyle: const HeaderStyle(formatButtonVisible: false),
          selectedDayPredicate: (day) => _selectedDay != null && isSameDay(day, _selectedDay),
          eventLoader: (day) {
            final dateStr = formatMealPlanDate(day);
            return appState.mealPlans.where((m) => m.date == dateStr).toList();
          },
          onPageChanged: (focusedDay) {
            setState(() => _focusedMonth = focusedDay);
            _loadMonth(focusedDay);
          },
          onDaySelected: (selectedDay, focusedDay) async {
            setState(() {
              _selectedDay = selectedDay;
              _focusedMonth = focusedDay;
            });
            await Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => DayMealPlanScreen(date: selectedDay)),
            );
            if (mounted) _loadMonth(_focusedMonth);
          },
          onDayLongPressed: (day, focusedDay) {
            final normalized = DateTime(day.year, day.month, day.day);
            setState(() {
              _focusedMonth = focusedDay;
              final existing = _selectedDays.where((d) => isSameDay(d, normalized)).toList();
              if (existing.isNotEmpty) {
                _selectedDays.removeAll(existing);
              } else {
                _selectedDays.add(normalized);
              }
            });
          },
          calendarBuilders: CalendarBuilders(
            markerBuilder: (context, day, events) {
              final style = mealMarkerStyle(context, events.isNotEmpty);
              return Icon(style.icon, size: 10, color: style.color);
            },
            defaultBuilder: (context, day, focusedDay) => _multiSelectDayCell(context, day),
            todayBuilder: (context, day, focusedDay) => _multiSelectDayCell(context, day),
          ),
        ),
        const Padding(
          padding: EdgeInsets.symmetric(vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const MealMarkerLegendSwatch(hasMeals: true, label: 'Meals planned'),
              const SizedBox(width: 16),
              const MealMarkerLegendSwatch(hasMeals: false, label: 'No meals planned'),
            ],
          ),
        ),
      ],
      ),
    );
  }

  /// Overlays a highlighted ring on days held via long-press for a targeted auto-fill; falls back to
  /// the calendar's normal rendering (null) for everything else, keeping the built-in today/selected
  /// styling intact.
  Widget? _multiSelectDayCell(BuildContext context, DateTime day) {
    if (!_selectedDays.any((d) => isSameDay(d, day))) return null;
    return Center(
      child: Container(
        width: 36,
        height: 36,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: Theme.of(context).colorScheme.primary, width: 2),
        ),
        alignment: Alignment.center,
        child: Text('${day.day}'),
      ),
    );
  }

  MealPlan? _findByMealType(List<MealPlan> plans, String mealType) {
    for (final p in plans) {
      if (p.mealType == mealType) return p;
    }
    return null;
  }

  Future<void> _autoGenerateToday(BuildContext context) async {
    final hasSelection = _selectedMealTypes.isNotEmpty;
    final options = await showAutoGenerateDialog(
      context,
      description: hasSelection
          ? 'Generates a plan for ${_selectedMealTypes.length} selected meal(s) today.'
          : "Generates today's plan (${weekdayLabel(_today)}).",
      initialRegenerate: true,
      showMealTypeSelector: !hasSelection,
      preselectedMealTypes: hasSelection ? _selectedMealTypes.toList() : null,
    );
    if (options == null || !context.mounted) return;

    final ok = await context.read<AppState>().autoGenerateSchedule(
          from: _today,
          to: _today,
          mealTypes: options.mealTypes,
          stockFilter: options.stockFilter,
          avoidRepeatDays: options.avoidRepeatDays,
          targetNutrition: options.targetNutrition,
          regenerate: options.regenerate,
        );
    if (context.mounted) {
      setState(_selectedMealTypes.clear);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? 'Plan generated.' : 'Could not generate a plan.')),
      );
    }
  }

  Future<void> _autoGenerateCalendar(BuildContext context) async {
    final hasDaySelection = _selectedDays.isNotEmpty;
    final options = await showAutoGenerateDialog(
      context,
      description: hasDaySelection
          ? 'Fills/regenerates ${_selectedDays.length} selected day(s).'
          : 'Fills empty slots starting ${weekdayLabel(_today)}.',
      showDaysField: !hasDaySelection,
      initialDays: 14,
    );
    if (options == null || !context.mounted) return;

    final appState = context.read<AppState>();
    bool ok = true;
    if (hasDaySelection) {
      for (final day in _selectedDays) {
        final result = await appState.autoGenerateSchedule(
          from: day,
          to: day,
          mealTypes: options.mealTypes,
          stockFilter: options.stockFilter,
          avoidRepeatDays: options.avoidRepeatDays,
          targetNutrition: options.targetNutrition,
          regenerate: options.regenerate,
        );
        ok = ok && result;
      }
    } else {
      final end = _today.add(Duration(days: (options.days ?? 14) - 1));
      ok = await appState.autoGenerateSchedule(
        from: _today,
        to: end,
        mealTypes: options.mealTypes,
        stockFilter: options.stockFilter,
        avoidRepeatDays: options.avoidRepeatDays,
        targetNutrition: options.targetNutrition,
        regenerate: options.regenerate,
      );
    }
    if (context.mounted) {
      setState(_selectedDays.clear);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(ok ? 'Schedule filled.' : 'Could not auto-fill schedule.')),
      );
    }
  }
}

class MealSlot extends StatelessWidget {
  final DateTime day;
  final String mealType;
  final MealPlan? mealPlan;
  final bool selected;
  final ValueChanged<String>? onLongPress;

  const MealSlot({
    super.key,
    required this.day,
    required this.mealType,
    required this.mealPlan,
    this.selected = false,
    this.onLongPress,
  });

  @override
  Widget build(BuildContext context) {
    final leading = SizedBox(
      width: 60,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (selected) ...[
            Icon(Icons.check_circle, size: 16, color: Theme.of(context).colorScheme.primary),
            const SizedBox(width: 4),
          ],
          Flexible(child: Text(mealType, style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant))),
        ],
      ),
    );
    final longPressHandler = onLongPress == null ? null : () => onLongPress!(mealType);

    if (mealPlan == null) {
      return ListTile(
        dense: true,
        selected: selected,
        selectedTileColor: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.4),
        leading: leading,
        title: Text('Tap to assign', style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant)),
        onTap: () => _assign(context),
        onLongPress: longPressHandler,
      );
    }
    final portionsSummary = mealPlan!.portions.map((p) => '${p.displayName ?? '?'} ×${p.portionMultiplier}').join(', ');
    return ListTile(
      dense: true,
      selected: selected,
      selectedTileColor: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.4),
      leading: leading,
      title: Text(mealPlan!.recipeName ?? '(recipe)'),
      subtitle: Text('$portionsSummary${mealPlan!.cooked ? ' · cooked' : ''}'),
      onTap: () => _openRecipe(context),
      onLongPress: longPressHandler,
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (mealPlan!.cooked)
            Icon(Icons.check_circle, color: Theme.of(context).extension<AppSemanticColors>()!.success)
          else
            IconButton(
              icon: const Icon(Icons.restaurant),
              tooltip: 'Mark cooked (deducts pantry stock)',
              onPressed: () => context.read<AppState>().confirmCooked(mealPlan!.id),
            ),
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: () => context.read<AppState>().deleteMealPlan(mealPlan!.id),
          ),
        ],
      ),
    );
  }

  Future<void> _openRecipe(BuildContext context) async {
    final appState = context.read<AppState>();
    final recipe = await appState.fetchRecipe(mealPlan!.recipeId);
    if (!context.mounted) return;
    if (recipe == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not load recipe.')),
      );
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => RecipeDetailScreen(recipe: recipe)),
    );
  }

  Future<void> _assign(BuildContext context) async {
    final recipe = await Navigator.of(context).push<Recipe>(
      MaterialPageRoute(builder: (_) => const PickRecipeScreen()),
    );
    if (recipe == null || !context.mounted) return;

    final appState = context.read<AppState>();
    final household = appState.currentHousehold!;
    final portions = await _pickPortions(context, household);
    if (portions == null || !context.mounted) return;

    final ok = await appState.createMealPlan(date: day, mealType: mealType, recipeId: recipe.id, portions: portions);
    if (ok && context.mounted) {
      final range = DateTime.now();
      context.read<AppState>().loadMealPlans(
          DateTime(range.year, range.month, range.day), DateTime(range.year, range.month, range.day).add(const Duration(days: 13)));
    }
  }

  Future<List<Map<String, dynamic>>?> _pickPortions(BuildContext context, Household household) {
    final controllers = {for (final m in household.members) m.userId: TextEditingController(text: '1')};
    return showDialog<List<Map<String, dynamic>>>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Portions'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: household.members
              .map((m) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(children: [
                      Expanded(child: Text(m.displayName ?? m.email ?? m.userId)),
                      SizedBox(
                        width: 60,
                        child: TextField(
                          controller: controllers[m.userId],
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(isDense: true),
                        ),
                      ),
                    ]),
                  ))
              .toList(),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              final portions = household.members
                  .map((m) => {
                        'userId': m.userId,
                        'portionMultiplier': double.tryParse(controllers[m.userId]!.text) ?? 1.0,
                      })
                  .toList();
              Navigator.pop(dialogContext, portions);
            },
            child: const Text('Assign'),
          ),
        ],
      ),
    );
  }
}
