import 'recipe.dart';

class ExternalRecipeSummary {
  final String externalId;
  final String name;
  final String? thumbnailUrl;
  final String? category;
  final String? area;

  ExternalRecipeSummary({required this.externalId, required this.name, this.thumbnailUrl, this.category, this.area});

  factory ExternalRecipeSummary.fromJson(Map<String, dynamic> json) => ExternalRecipeSummary(
        externalId: json['externalId'] as String,
        name: json['name'] as String,
        thumbnailUrl: json['thumbnailUrl'] as String?,
        category: json['category'] as String?,
        area: json['area'] as String?,
      );
}

class ExternalRecipeIngredient {
  final String name;
  final double quantity;
  final String unit;

  ExternalRecipeIngredient({required this.name, required this.quantity, required this.unit});

  factory ExternalRecipeIngredient.fromJson(Map<String, dynamic> json) => ExternalRecipeIngredient(
        name: json['name'] as String,
        quantity: (json['quantity'] as num).toDouble(),
        unit: json['unit'] as String,
      );
}

class ExternalRecipeDetail {
  final String externalId;
  final String name;
  final String category;
  final String? instructions;
  final int servings;
  final String? imageUrl;
  final List<ExternalRecipeIngredient> ingredients;
  final List<RecipeTagRef> tags;
  // Best-effort estimate from a live Open Food Facts search per ingredient -- TheMealDB has no
  // nutrition data of its own, so this (unlike a saved Recipe) is always the estimate, never an
  // author-entered value.
  final double? estimatedCaloriesPerServing;
  final double? estimatedProteinPerServing;
  final double? estimatedCarbsPerServing;
  final double? estimatedFatPerServing;
  final bool nutritionEstimateIncomplete;

  ExternalRecipeDetail({
    required this.externalId,
    required this.name,
    required this.category,
    this.instructions,
    required this.servings,
    this.imageUrl,
    required this.ingredients,
    required this.tags,
    this.estimatedCaloriesPerServing,
    this.estimatedProteinPerServing,
    this.estimatedCarbsPerServing,
    this.estimatedFatPerServing,
    this.nutritionEstimateIncomplete = false,
  });

  factory ExternalRecipeDetail.fromJson(Map<String, dynamic> json) => ExternalRecipeDetail(
        externalId: json['externalId'] as String,
        name: json['name'] as String,
        category: json['category'] as String,
        instructions: json['instructions'] as String?,
        servings: json['servings'] as int,
        imageUrl: json['imageUrl'] as String?,
        ingredients: (json['ingredients'] as List<dynamic>? ?? [])
            .map((j) => ExternalRecipeIngredient.fromJson(j as Map<String, dynamic>))
            .toList(),
        tags: (json['tags'] as List<dynamic>? ?? []).map((j) => RecipeTagRef.fromJson(j as Map<String, dynamic>)).toList(),
        estimatedCaloriesPerServing: (json['estimatedCaloriesPerServing'] as num?)?.toDouble(),
        estimatedProteinPerServing: (json['estimatedProteinPerServing'] as num?)?.toDouble(),
        estimatedCarbsPerServing: (json['estimatedCarbsPerServing'] as num?)?.toDouble(),
        estimatedFatPerServing: (json['estimatedFatPerServing'] as num?)?.toDouble(),
        nutritionEstimateIncomplete: json['nutritionEstimateIncomplete'] as bool? ?? false,
      );
}
