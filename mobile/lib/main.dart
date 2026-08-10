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

class RootScreen extends StatelessWidget {
  const RootScreen({super.key});

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
