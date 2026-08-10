import 'package:flutter/material.dart';
import 'allergies_preferences_screen.dart';
import 'household_preferences_list_screen.dart';
import 'notification_settings_screen.dart';
import 'nutrition_profile_screen.dart';
import 'shopping_preferences_screen.dart';

/// Preferences landing page: a menu of setting categories, each opening its own dedicated screen
/// (rather than one long scrolling page) so loading/state for each category stays independent.
class PreferencesScreen extends StatelessWidget {
  const PreferencesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Preferences')),
      body: ListView(
        children: [
          _MenuTile(
            icon: Icons.monitor_weight_outlined,
            title: 'Nutrition profile',
            subtitle: 'Daily calorie and macro targets, used by "target my nutrition".',
            builder: (_) => const NutritionProfileScreen(),
          ),
          _MenuTile(
            icon: Icons.no_food_outlined,
            title: 'Allergies and Preferences',
            subtitle: 'Hard-exclude allergies and taste preferences for recipe filtering.',
            builder: (_) => const AllergiesPreferencesScreen(),
          ),
          _MenuTile(
            icon: Icons.groups_outlined,
            title: 'Household preferences',
            subtitle: 'Members, and allergies/tastes specific to each of your households.',
            builder: (_) => const HouseholdPreferencesListScreen(),
          ),
          _MenuTile(
            icon: Icons.shopping_cart_outlined,
            title: 'Shopping List',
            subtitle: 'Measurement units and the missing-ingredients lookahead window.',
            builder: (_) => const ShoppingPreferencesScreen(),
          ),
          _MenuTile(
            icon: Icons.notifications_outlined,
            title: 'Notifications',
            subtitle: 'Meal reminders and low-stock alerts.',
            builder: (_) => const NotificationSettingsScreen(),
          ),
        ],
      ),
    );
  }
}

class _MenuTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final WidgetBuilder builder;

  const _MenuTile({required this.icon, required this.title, required this.subtitle, required this.builder});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon),
      title: Text(title),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: builder)),
    );
  }
}
