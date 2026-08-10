import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

/// Wraps flutter_local_notifications. Scheduled times are computed from device-local
/// wall-clock time, then converted to an absolute UTC instant -- this sidesteps needing
/// the device's IANA timezone name (which would require an extra plugin) while still
/// firing at the correct moment.
class NotificationService {
  static const _mealChannel = AndroidNotificationDetails(
    'meal_reminders',
    'Meal reminders',
    channelDescription: 'Daily meal and start-cooking reminders',
    importance: Importance.high,
    priority: Priority.high,
  );
  static const _lowStockChannel = AndroidNotificationDetails(
    'low_stock_alerts',
    'Low stock alerts',
    channelDescription: 'Pantry items running low',
    importance: Importance.high,
    priority: Priority.high,
  );

  final _plugin = FlutterLocalNotificationsPlugin();
  bool _initialized = false;

  Future<void> init() async {
    if (_initialized) return;
    _initialized = true;

    tzdata.initializeTimeZones();
    tz.setLocalLocation(tz.UTC);

    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosInit = DarwinInitializationSettings();
    await _plugin.initialize(const InitializationSettings(android: androidInit, iOS: iosInit));

    await _plugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
    await _plugin
        .resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(alert: true, badge: true, sound: true);
  }

  Future<void> cancel(int id) => _plugin.cancel(id);

  Future<void> scheduleAt({
    required int id,
    required String title,
    required String body,
    required DateTime localTime,
  }) async {
    if (localTime.isBefore(DateTime.now())) return; // don't schedule in the past
    final scheduled = tz.TZDateTime.from(localTime.toUtc(), tz.UTC);
    await _plugin.zonedSchedule(
      id,
      title,
      body,
      scheduled,
      const NotificationDetails(android: _mealChannel, iOS: DarwinNotificationDetails()),
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      uiLocalNotificationDateInterpretation: UILocalNotificationDateInterpretation.absoluteTime,
    );
  }

  Future<void> showLowStockAlert({required int id, required String title, required String body}) {
    return _plugin.show(
      id,
      title,
      body,
      const NotificationDetails(android: _lowStockChannel, iOS: DarwinNotificationDetails()),
    );
  }
}
