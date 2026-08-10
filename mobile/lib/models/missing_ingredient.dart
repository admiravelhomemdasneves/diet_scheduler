class MissingIngredient {
  final String ingredientId;
  final String? ingredientName;
  final double quantity;
  final String unit;

  MissingIngredient({
    required this.ingredientId,
    this.ingredientName,
    required this.quantity,
    required this.unit,
  });

  /// Identifies this shortfall the same way the backend keys it (ingredientId+unit), for
  /// selection tracking and dedup -- there's no persisted id, it's a computed preview row.
  String get key => '$ingredientId|$unit';

  factory MissingIngredient.fromJson(Map<String, dynamic> json) => MissingIngredient(
        ingredientId: json['ingredientId'] as String,
        ingredientName: json['ingredientName'] as String?,
        quantity: (json['quantity'] as num).toDouble(),
        unit: json['unit'] as String,
      );
}
