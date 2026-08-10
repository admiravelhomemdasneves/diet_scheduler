import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:dietscheduler/main.dart';

void main() {
  testWidgets('App boots to the sign-in screen when signed out', (WidgetTester tester) async {
    // flutter_secure_storage's Linux implementation talks to the real OS secret service over
    // D-Bus; in a headless test environment (e.g. WSL with no secret service running) that call
    // hangs instead of throwing, which stalls AppState.bootstrap() forever. Stub the channel so
    // it behaves the same as platforms with no secure-storage implementation: report "nothing
    // stored" immediately.
    const channel = MethodChannel('plugins.it_nomads.com/flutter_secure_storage');
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        switch (methodCall.method) {
          case 'read':
            return null;
          case 'delete':
          case 'write':
            return null;
          default:
            return null;
        }
      },
    );
    addTearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
    });

    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();

    expect(find.text('DietScheduler'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Sign in with Google'), findsOneWidget);
  });
}
