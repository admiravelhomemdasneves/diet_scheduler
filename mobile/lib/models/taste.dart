import 'package:flutter/material.dart';

const List<String> tastePreferences = ['FAVORITE', 'LIKED', 'DISLIKED', 'FORBIDDEN'];

Color colorForTastePreference(String preference) {
  switch (preference) {
    case 'FAVORITE':
      return Colors.green;
    case 'LIKED':
      return Colors.blue;
    case 'DISLIKED':
      return Colors.orange;
    case 'FORBIDDEN':
      return Colors.red;
    default:
      return Colors.grey;
  }
}

String tastePreferenceLabel(String preference) =>
    preference.isEmpty ? preference : preference[0] + preference.substring(1).toLowerCase();

class Taste {
  final String id;
  final String type; // FLAVOR / DIET / CUISINE
  final String name;

  Taste({required this.id, required this.type, required this.name});

  factory Taste.fromJson(Map<String, dynamic> json) =>
      Taste(id: json['id'] as String, type: json['type'] as String, name: json['name'] as String);
}

class TastePreferenceEntry {
  final String tasteId;
  final String tasteName;
  final String type;
  final String preference;

  TastePreferenceEntry({required this.tasteId, required this.tasteName, required this.type, required this.preference});

  factory TastePreferenceEntry.fromJson(Map<String, dynamic> json) => TastePreferenceEntry(
        tasteId: json['tasteId'] as String,
        tasteName: json['tasteName'] as String,
        type: json['type'] as String,
        preference: json['preference'] as String,
      );
}
