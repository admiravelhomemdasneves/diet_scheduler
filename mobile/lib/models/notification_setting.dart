/// Times are "HH:mm:ss" strings as returned by the backend (java.time.LocalTime JSON form).
class NotificationSetting {
  final String lunchTime;
  final String dinnerTime;
  final String snackTime;
  final bool dailyMealReminderEnabled;
  final bool startCookingReminderEnabled;
  final bool lowStockAlertEnabled;
  final double lowStockThreshold;

  NotificationSetting({
    required this.lunchTime,
    required this.dinnerTime,
    required this.snackTime,
    required this.dailyMealReminderEnabled,
    required this.startCookingReminderEnabled,
    required this.lowStockAlertEnabled,
    required this.lowStockThreshold,
  });

  factory NotificationSetting.fromJson(Map<String, dynamic> json) => NotificationSetting(
        lunchTime: json['lunchTime'] as String,
        dinnerTime: json['dinnerTime'] as String,
        snackTime: json['snackTime'] as String,
        dailyMealReminderEnabled: json['dailyMealReminderEnabled'] as bool,
        startCookingReminderEnabled: json['startCookingReminderEnabled'] as bool,
        lowStockAlertEnabled: json['lowStockAlertEnabled'] as bool,
        lowStockThreshold: (json['lowStockThreshold'] as num).toDouble(),
      );

  String timeFor(String mealType) {
    switch (mealType) {
      case 'LUNCH':
        return lunchTime;
      case 'DINNER':
        return dinnerTime;
      case 'SNACK':
      default:
        return snackTime;
    }
  }
}
