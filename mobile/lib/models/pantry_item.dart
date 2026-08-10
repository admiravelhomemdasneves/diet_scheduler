const List<String> pantryLocations = ['PANTRY', 'FRIDGE', 'FREEZER'];

class PantryItem {
  final String id;
  final String householdId;
  final String ingredientId;
  final String? ingredientName;
  final String? ingredientImageUrl;
  final double? ingredientCaloriesPer100g;
  final double? ingredientProteinPer100g;
  final double? ingredientCarbsPer100g;
  final double? ingredientFatPer100g;
  final bool ingredientCustom;
  final String location;
  final double quantity;
  final String unit;
  final String? expirationDate; // ISO-8601 date (yyyy-MM-dd), nullable

  PantryItem({
    required this.id,
    required this.householdId,
    required this.ingredientId,
    this.ingredientName,
    this.ingredientImageUrl,
    this.ingredientCaloriesPer100g,
    this.ingredientProteinPer100g,
    this.ingredientCarbsPer100g,
    this.ingredientFatPer100g,
    this.ingredientCustom = false,
    required this.location,
    required this.quantity,
    required this.unit,
    this.expirationDate,
  });

  factory PantryItem.fromJson(Map<String, dynamic> json) => PantryItem(
        id: json['id'] as String,
        householdId: json['householdId'] as String,
        ingredientId: json['ingredientId'] as String,
        ingredientName: json['ingredientName'] as String?,
        ingredientImageUrl: json['ingredientImageUrl'] as String?,
        ingredientCaloriesPer100g: (json['ingredientCaloriesPer100g'] as num?)?.toDouble(),
        ingredientProteinPer100g: (json['ingredientProteinPer100g'] as num?)?.toDouble(),
        ingredientCarbsPer100g: (json['ingredientCarbsPer100g'] as num?)?.toDouble(),
        ingredientFatPer100g: (json['ingredientFatPer100g'] as num?)?.toDouble(),
        ingredientCustom: json['ingredientCustom'] as bool? ?? false,
        location: json['location'] as String,
        quantity: (json['quantity'] as num).toDouble(),
        unit: json['unit'] as String,
        expirationDate: json['expirationDate'] as String?,
      );
}
