import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config.dart';
import '../models/ingredient.dart';
import '../state/app_state.dart';

/// Ingredient-name text field with a live, debounced search-as-you-type dropdown of Open Food
/// Facts matches (thumbnail + name). Picking a suggestion reports it back via [onSuggestionSelected]
/// so the caller can pass its nutrition data straight through when creating a new ingredient;
/// any further edit to the text clears the selection so stale data never gets submitted.
class IngredientNameField extends StatefulWidget {
  final String initialValue;
  final ValueChanged<String> onNameChanged;
  final ValueChanged<IngredientSuggestion?> onSuggestionSelected;

  const IngredientNameField({
    super.key,
    this.initialValue = '',
    required this.onNameChanged,
    required this.onSuggestionSelected,
  });

  @override
  State<IngredientNameField> createState() => _IngredientNameFieldState();
}

class _IngredientNameFieldState extends State<IngredientNameField> {
  late final _controller = TextEditingController(text: widget.initialValue);
  final _focusNode = FocusNode();
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _focusNode.addListener(() {
      if (!_focusNode.hasFocus) {
        context.read<AppState>().clearIngredientSuggestions();
      }
    });
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    widget.onNameChanged(value);
    widget.onSuggestionSelected(null);
    _debounce?.cancel();
    if (value.trim().isEmpty) {
      context.read<AppState>().clearIngredientSuggestions();
      return;
    }
    _debounce = Timer(const Duration(milliseconds: 400), () {
      context.read<AppState>().searchIngredients(value.trim());
    });
  }

  void _select(IngredientSuggestion suggestion) {
    _controller.value = TextEditingValue(
      text: suggestion.name,
      selection: TextSelection.collapsed(offset: suggestion.name.length),
    );
    widget.onNameChanged(suggestion.name);
    widget.onSuggestionSelected(suggestion);
    context.read<AppState>().clearIngredientSuggestions();
    _focusNode.unfocus();
  }

  @override
  Widget build(BuildContext context) {
    final suggestions = context.watch<AppState>().ingredientSuggestions;
    final showDropdown = _focusNode.hasFocus && suggestions.isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          controller: _controller,
          focusNode: _focusNode,
          decoration: const InputDecoration(labelText: 'Ingredient name'),
          onChanged: _onChanged,
        ),
        if (showDropdown)
          ConstrainedBox(
            constraints: const BoxConstraints(maxHeight: 180),
            child: Card(
              margin: const EdgeInsets.only(top: 4),
              child: ListView.builder(
                shrinkWrap: true,
                padding: EdgeInsets.zero,
                itemCount: suggestions.length,
                itemBuilder: (context, i) {
                  final s = suggestions[i];
                  final imageUrl = ApiConfig.resolveImageUrl(s.imageUrl);
                  final missingNutrition = s.caloriesPer100g == null ||
                      s.proteinPer100g == null ||
                      s.carbsPer100g == null ||
                      s.fatPer100g == null;
                  return ListTile(
                    dense: true,
                    leading: imageUrl != null
                        ? ClipRRect(
                            borderRadius: BorderRadius.circular(4),
                            child: Image.network(
                              imageUrl,
                              width: 32,
                              height: 32,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) => const Icon(Icons.image_not_supported_outlined),
                            ),
                          )
                        : const Icon(Icons.image_not_supported_outlined),
                    title: Text(s.name),
                    trailing: missingNutrition
                        ? Tooltip(
                            message: 'Nutrition data missing or incomplete',
                            child: Icon(Icons.warning_amber_rounded, color: Colors.orange.shade700, size: 20),
                          )
                        : null,
                    onTap: () => _select(s),
                  );
                },
              ),
            ),
          ),
      ],
    );
  }
}
