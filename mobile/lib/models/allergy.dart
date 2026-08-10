class Allergy {
  final String id;
  final String name;
  final String? category;

  Allergy({required this.id, required this.name, this.category});

  factory Allergy.fromJson(Map<String, dynamic> json) =>
      Allergy(id: json['id'] as String, name: json['name'] as String, category: json['category'] as String?);
}
