class NutritionProfile {
  final String? gender;
  final int? age;
  final double? weight;
  final double? height;
  final double? calorieTarget;
  final double? proteinTargetGrams;
  final double? carbsTargetGrams;
  final double? fatTargetGrams;
  final double? weightGoalKg;

  NutritionProfile({
    this.gender,
    this.age,
    this.weight,
    this.height,
    this.calorieTarget,
    this.proteinTargetGrams,
    this.carbsTargetGrams,
    this.fatTargetGrams,
    this.weightGoalKg,
  });

  factory NutritionProfile.fromJson(Map<String, dynamic> json) => NutritionProfile(
        gender: json['gender'] as String?,
        age: json['age'] as int?,
        weight: (json['weight'] as num?)?.toDouble(),
        height: (json['height'] as num?)?.toDouble(),
        calorieTarget: (json['calorieTarget'] as num?)?.toDouble(),
        proteinTargetGrams: (json['proteinTargetGrams'] as num?)?.toDouble(),
        carbsTargetGrams: (json['carbsTargetGrams'] as num?)?.toDouble(),
        fatTargetGrams: (json['fatTargetGrams'] as num?)?.toDouble(),
        weightGoalKg: (json['weightGoalKg'] as num?)?.toDouble(),
      );
}
