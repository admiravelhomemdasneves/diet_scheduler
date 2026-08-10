class Ingredient {
  final String id;
  final String name;
  final String? category;
  final String? defaultUnit;
  final String? barcode;
  final String? imageUrl;
  final double? caloriesPer100g;
  final double? proteinPer100g;
  final double? carbsPer100g;
  final double? fatPer100g;
  final bool custom;

  Ingredient({
    required this.id,
    required this.name,
    this.category,
    this.defaultUnit,
    this.barcode,
    this.imageUrl,
    this.caloriesPer100g,
    this.proteinPer100g,
    this.carbsPer100g,
    this.fatPer100g,
    this.custom = false,
  });

  factory Ingredient.fromJson(Map<String, dynamic> json) => Ingredient(
        id: json['id'] as String,
        name: json['name'] as String,
        category: json['category'] as String?,
        defaultUnit: json['defaultUnit'] as String?,
        barcode: json['barcode'] as String?,
        imageUrl: json['imageUrl'] as String?,
        caloriesPer100g: (json['caloriesPer100g'] as num?)?.toDouble(),
        proteinPer100g: (json['proteinPer100g'] as num?)?.toDouble(),
        carbsPer100g: (json['carbsPer100g'] as num?)?.toDouble(),
        fatPer100g: (json['fatPer100g'] as num?)?.toDouble(),
        custom: json['custom'] as bool? ?? false,
      );

  /// Builds display data straight from a search match, for ingredients with no persisted id yet
  /// (an Open Food Facts result not yet added to anyone's pantry/recipe) -- used to show an
  /// ingredient detail view without a backend round trip via GET /pantry/ingredients/{id}.
  factory Ingredient.fromSuggestion(IngredientSuggestion s) => Ingredient(
        id: s.id ?? '',
        name: s.name,
        category: s.category,
        barcode: s.barcode,
        imageUrl: s.imageUrl,
        caloriesPer100g: s.caloriesPer100g,
        proteinPer100g: s.proteinPer100g,
        carbsPer100g: s.carbsPer100g,
        fatPer100g: s.fatPer100g,
        custom: s.custom,
      );
}

/// A live ingredient-name-dropdown match: either the household's own custom ingredient (has an
/// [id], [custom] true -- picking it reuses that exact ingredient) or an Open Food Facts result
/// (no [id] yet -- one is only created if the item is actually added/edited in).
class IngredientSuggestion {
  final String? id;
  final String name;
  final String? barcode;
  final String? imageUrl;
  final String? category;
  final double? caloriesPer100g;
  final double? proteinPer100g;
  final double? carbsPer100g;
  final double? fatPer100g;
  final bool custom;

  IngredientSuggestion({
    this.id,
    required this.name,
    this.barcode,
    this.imageUrl,
    this.category,
    this.caloriesPer100g,
    this.proteinPer100g,
    this.carbsPer100g,
    this.fatPer100g,
    this.custom = false,
  });

  factory IngredientSuggestion.fromJson(Map<String, dynamic> json) => IngredientSuggestion(
        id: json['id'] as String?,
        name: json['name'] as String,
        barcode: json['barcode'] as String?,
        imageUrl: json['imageUrl'] as String?,
        category: json['category'] as String?,
        caloriesPer100g: (json['caloriesPer100g'] as num?)?.toDouble(),
        proteinPer100g: (json['proteinPer100g'] as num?)?.toDouble(),
        carbsPer100g: (json['carbsPer100g'] as num?)?.toDouble(),
        fatPer100g: (json['fatPer100g'] as num?)?.toDouble(),
        custom: json['custom'] as bool? ?? false,
      );
}
