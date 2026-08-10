import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/main.dart';

void main() {
  testWidgets('App boots to the sign-in screen when signed out', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());
    await tester.pump();

    expect(find.text('DietScheduler'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Sign in with Google'), findsOneWidget);
  });
}
