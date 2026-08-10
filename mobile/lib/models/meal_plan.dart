const List<String> mealTypes = ['LUNCH', 'DINNER', 'SNACK'];

String formatMealPlanDate(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

String weekdayLabel(DateTime d) {
  const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  return '${weekdays[d.weekday - 1]} ${d.day}/${d.month}';
}

class MealPlanPortionRef {
  final String userId;
  final String? displayName;
  final double portionMultiplier;

  MealPlanPortionRef({required this.userId, this.displayName, required this.portionMultiplier});

  factory MealPlanPortionRef.fromJson(Map<String, dynamic> json) => MealPlanPortionRef(
        userId: json['userId'] as String,
        displayName: json['displayName'] as String?,
        portionMultiplier: (json['portionMultiplier'] as num).toDouble(),
      );
}

/// The requesting user's own combined nutrition for a day's meal plan, compared against their
/// nutrition-profile targets (Preferences). A target/delta field is null only if that particular
/// target hasn't been set.
class DayNutritionSummary {
  final double totalCalories;
  final double totalProtein;
  final double totalCarbs;
  final double totalFat;
  final double? calorieTarget;
  final double? proteinTarget;
  final double? carbsTarget;
  final double? fatTarget;
  final double? calorieDelta;
  final double? proteinDelta;
  final double? carbsDelta;
  final double? fatDelta;
  final bool nutritionIncomplete;

  DayNutritionSummary({
    required this.totalCalories,
    required this.totalProtein,
    required this.totalCarbs,
    required this.totalFat,
    this.calorieTarget,
    this.proteinTarget,
    this.carbsTarget,
    this.fatTarget,
    this.calorieDelta,
    this.proteinDelta,
    this.carbsDelta,
    this.fatDelta,
    this.nutritionIncomplete = false,
  });

  factory DayNutritionSummary.fromJson(Map<String, dynamic> json) => DayNutritionSummary(
        totalCalories: (json['totalCalories'] as num).toDouble(),
        totalProtein: (json['totalProtein'] as num).toDouble(),
        totalCarbs: (json['totalCarbs'] as num).toDouble(),
        totalFat: (json['totalFat'] as num).toDouble(),
        calorieTarget: (json['calorieTarget'] as num?)?.toDouble(),
        proteinTarget: (json['proteinTarget'] as num?)?.toDouble(),
        carbsTarget: (json['carbsTarget'] as num?)?.toDouble(),
        fatTarget: (json['fatTarget'] as num?)?.toDouble(),
        calorieDelta: (json['calorieDelta'] as num?)?.toDouble(),
        proteinDelta: (json['proteinDelta'] as num?)?.toDouble(),
        carbsDelta: (json['carbsDelta'] as num?)?.toDouble(),
        fatDelta: (json['fatDelta'] as num?)?.toDouble(),
        nutritionIncomplete: json['nutritionIncomplete'] as bool? ?? false,
      );
}

class MealPlan {
  final String id;
  final String householdId;
  final String date; // yyyy-MM-dd
  final String mealType;
  final String recipeId;
  final String? recipeName;
  final int? prepTimeMinutes;
  final int? cookTimeMinutes;
  final List<MealPlanPortionRef> portions;
  final bool cooked;

  MealPlan({
    required this.id,
    required this.householdId,
    required this.date,
    required this.mealType,
    required this.recipeId,
    this.recipeName,
    this.prepTimeMinutes,
    this.cookTimeMinutes,
    required this.portions,
    this.cooked = false,
  });

  factory MealPlan.fromJson(Map<String, dynamic> json) => MealPlan(
        id: json['id'] as String,
        householdId: json['householdId'] as String,
        date: json['date'] as String,
        mealType: json['mealType'] as String,
        recipeId: json['recipeId'] as String,
        recipeName: json['recipeName'] as String?,
        prepTimeMinutes: json['prepTimeMinutes'] as int?,
        cookTimeMinutes: json['cookTimeMinutes'] as int?,
        portions: (json['portions'] as List<dynamic>? ?? [])
            .map((j) => MealPlanPortionRef.fromJson(j as Map<String, dynamic>))
            .toList(),
        cooked: json['cooked'] as bool? ?? false,
      );
}
