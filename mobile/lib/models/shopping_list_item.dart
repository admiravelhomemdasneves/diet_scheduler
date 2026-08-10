class ShoppingListItem {
  final String id;
  final String householdId;
  final String ingredientId;
  final String? ingredientName;
  final double quantity;
  final String unit;
  final bool checked;
  final String source; // MANUAL / AUTO_GENERATED

  ShoppingListItem({
    required this.id,
    required this.householdId,
    required this.ingredientId,
    this.ingredientName,
    required this.quantity,
    required this.unit,
    required this.checked,
    required this.source,
  });

  factory ShoppingListItem.fromJson(Map<String, dynamic> json) => ShoppingListItem(
        id: json['id'] as String,
        householdId: json['householdId'] as String,
        ingredientId: json['ingredientId'] as String,
        ingredientName: json['ingredientName'] as String?,
        quantity: (json['quantity'] as num).toDouble(),
        unit: json['unit'] as String,
        checked: json['checked'] as bool,
        source: json['source'] as String,
      );
}
