import 'package:flutter/material.dart';
import 'external_recipe_search_screen.dart';
import 'meal_plan_screen.dart';
import 'pantry_screen.dart';
import 'preferences_screen.dart';
import 'shopping_list_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _index = 0;

  static const _pages = [
    PantryScreen(),
    ExternalRecipeSearchScreen(),
    MealPlanScreen(),
    ShoppingListScreen(),
    PreferencesScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.kitchen), label: 'Pantry'),
          NavigationDestination(icon: Icon(Icons.restaurant_menu), label: 'Recipes'),
          NavigationDestination(icon: Icon(Icons.calendar_month), label: 'Meal Plan'),
          NavigationDestination(icon: Icon(Icons.shopping_cart), label: 'Shopping'),
          NavigationDestination(icon: Icon(Icons.tune), label: 'Preferences'),
        ],
      ),
    );
  }
}
