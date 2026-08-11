import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'screens/home_screen.dart';
import 'screens/household_screen.dart';
import 'screens/sign_in_screen.dart';
import 'state/app_state.dart';
import 'theme/app_color_schemes.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AppState()..bootstrap(),
      child: MaterialApp(
        title: 'DietScheduler',
        debugShowCheckedModeBanner: false,
        theme: buildAppTheme(AppColorSchemes.light),
        darkTheme: buildAppTheme(AppColorSchemes.dark),
        themeMode: ThemeMode.system,
        home: const RootScreen(),
      ),
    );
  }
}

/// Wired once here, at the top of the widget tree, rather than per-screen: a StreamSubscription
/// on AppState.errorStream lives for the whole app session and shows a SnackBar for every error
/// event regardless of which of the app's ~28 screens happens to be visible when it fires. This
/// is what makes error surfacing "free" for every screen, including the ones (Pantry, MealPlan,
/// ShoppingList, Notification, Nutrition, most of Preferences) that previously read
/// appState.errorMessage nowhere at all.
class RootScreen extends StatefulWidget {
  const RootScreen({super.key});

  @override
  State<RootScreen> createState() => _RootScreenState();
}

class _RootScreenState extends State<RootScreen> {
  StreamSubscription<String>? _errorSubscription;

  @override
  void initState() {
    super.initState();
    _errorSubscription = context.read<AppState>().errorStream.listen((message) {
      final colorScheme = Theme.of(context).colorScheme;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message, style: TextStyle(color: colorScheme.onErrorContainer)),
          backgroundColor: colorScheme.errorContainer,
          behavior: SnackBarBehavior.floating,
        ),
      );
    });
  }

  @override
  void dispose() {
    _errorSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    if (appState.isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (appState.currentUser == null) {
      return const SignInScreen();
    }
    if (appState.currentHousehold == null) {
      return const HouseholdScreen();
    }
    return const HomeScreen();
  }
}
