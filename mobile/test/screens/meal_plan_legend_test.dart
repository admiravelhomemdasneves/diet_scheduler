import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:dietscheduler/screens/meal_plan_screen.dart';
import 'package:dietscheduler/theme/app_color_schemes.dart';
import 'package:dietscheduler/theme/app_theme.dart';

/// Regression test for the calendar legend/marker duplication bug (see MealMarkerLegendSwatch's
/// doc comment): the legend previously hardcoded its own copy of the marker's icon/color, which
/// silently drifted from the actual marker style. Both now route through the single
/// mealMarkerStyle(context, hasMeals) function; this test asserts MealMarkerLegendSwatch's
/// rendered Icon actually reflects that function's output, for both states, in both themes --
/// so a future change to mealMarkerStyle that isn't picked up by the legend fails loudly here
/// instead of silently drifting again.
void main() {
  for (final brightness in [Brightness.light, Brightness.dark]) {
    for (final hasMeals in [true, false]) {
      testWidgets(
        'MealMarkerLegendSwatch(hasMeals: $hasMeals) matches mealMarkerStyle in ${brightness.name} theme',
        (tester) async {
          late MealMarkerStyle expected;
          await tester.pumpWidget(
            MaterialApp(
              theme: buildAppTheme(brightness == Brightness.dark ? AppColorSchemes.dark : AppColorSchemes.light),
              home: Builder(
                builder: (context) {
                  expected = mealMarkerStyle(context, hasMeals);
                  return Scaffold(
                    body: MealMarkerLegendSwatch(hasMeals: hasMeals, label: 'test label'),
                  );
                },
              ),
            ),
          );

          final renderedIcon = tester.widget<Icon>(find.byType(Icon));
          expect(renderedIcon.icon, expected.icon);
          expect(renderedIcon.color, expected.color);
        },
      );
    }
  }
}
