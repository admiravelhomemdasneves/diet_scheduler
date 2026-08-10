class RecipeIngredientRef {
  final String ingredientId;
  final String? ingredientName;
  final double quantity;
  final String unit;

  RecipeIngredientRef({required this.ingredientId, this.ingredientName, required this.quantity, required this.unit});

  factory RecipeIngredientRef.fromJson(Map<String, dynamic> json) => RecipeIngredientRef(
        ingredientId: json['ingredientId'] as String,
        ingredientName: json['ingredientName'] as String?,
        quantity: (json['quantity'] as num).toDouble(),
        unit: json['unit'] as String,
      );
}

class RecipeTagRef {
  final String type; // FLAVOR / DIET / CUISINE
  final String value;

  RecipeTagRef({required this.type, required this.value});

  factory RecipeTagRef.fromJson(Map<String, dynamic> json) =>
      RecipeTagRef(type: json['type'] as String, value: json['value'] as String);
}

const List<String> recipeCategories = ['SNACK', 'ENTREE', 'MAIN', 'DESSERT'];

class Recipe {
  final String id;
  final String name;
  final String category;
  final String? instructions;
  final int servings;
  final int? prepTimeMinutes;
  final int? cookTimeMinutes;
  final double? caloriesPerServing;
  final double? proteinPerServing;
  final double? carbsPerServing;
  final double? fatPerServing;
  // Fallback nutrition computed server-side from the ingredient list, only present when the
  // fields above are null (no author-entered nutrition). See recipe_detail_screen.dart.
  final double? estimatedCaloriesPerServing;
  final double? estimatedProteinPerServing;
  final double? estimatedCarbsPerServing;
  final double? estimatedFatPerServing;
  final bool nutritionEstimateIncomplete;
  final String? createdByUserId;
  final String? imageUrl;
  final bool isPrivate;
  final List<RecipeIngredientRef> ingredients;
  final List<RecipeTagRef> tags;
  final double? stockCoverage;

  Recipe({
    required this.id,
    required this.name,
    required this.category,
    this.instructions,
    required this.servings,
    this.prepTimeMinutes,
    this.cookTimeMinutes,
    this.caloriesPerServing,
    this.proteinPerServing,
    this.carbsPerServing,
    this.fatPerServing,
    this.estimatedCaloriesPerServing,
    this.estimatedProteinPerServing,
    this.estimatedCarbsPerServing,
    this.estimatedFatPerServing,
    this.nutritionEstimateIncomplete = false,
    this.createdByUserId,
    this.imageUrl,
    required this.isPrivate,
    required this.ingredients,
    required this.tags,
    this.stockCoverage,
  });

  factory Recipe.fromJson(Map<String, dynamic> json) => Recipe(
        id: json['id'] as String,
        name: json['name'] as String,
        category: json['category'] as String,
        instructions: json['instructions'] as String?,
        servings: json['servings'] as int,
        prepTimeMinutes: json['prepTimeMinutes'] as int?,
        cookTimeMinutes: json['cookTimeMinutes'] as int?,
        caloriesPerServing: (json['caloriesPerServing'] as num?)?.toDouble(),
        proteinPerServing: (json['proteinPerServing'] as num?)?.toDouble(),
        carbsPerServing: (json['carbsPerServing'] as num?)?.toDouble(),
        fatPerServing: (json['fatPerServing'] as num?)?.toDouble(),
        estimatedCaloriesPerServing: (json['estimatedCaloriesPerServing'] as num?)?.toDouble(),
        estimatedProteinPerServing: (json['estimatedProteinPerServing'] as num?)?.toDouble(),
        estimatedCarbsPerServing: (json['estimatedCarbsPerServing'] as num?)?.toDouble(),
        estimatedFatPerServing: (json['estimatedFatPerServing'] as num?)?.toDouble(),
        nutritionEstimateIncomplete: json['nutritionEstimateIncomplete'] as bool? ?? false,
        createdByUserId: json['createdByUserId'] as String?,
        imageUrl: json['imageUrl'] as String?,
        isPrivate: json['isPrivate'] as bool,
        ingredients: (json['ingredients'] as List<dynamic>? ?? [])
            .map((j) => RecipeIngredientRef.fromJson(j as Map<String, dynamic>))
            .toList(),
        tags: (json['tags'] as List<dynamic>? ?? []).map((j) => RecipeTagRef.fromJson(j as Map<String, dynamic>)).toList(),
        stockCoverage: (json['stockCoverage'] as num?)?.toDouble(),
      );
}
