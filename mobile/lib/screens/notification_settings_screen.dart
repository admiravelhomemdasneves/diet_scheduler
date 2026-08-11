import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/notification_setting.dart';
import '../state/app_state.dart';

class NotificationSettingsScreen extends StatefulWidget {
  const NotificationSettingsScreen({super.key});

  @override
  State<NotificationSettingsScreen> createState() => _NotificationSettingsScreenState();
}

class _NotificationSettingsScreenState extends State<NotificationSettingsScreen> {
  bool _loaded = false;
  TimeOfDay _lunch = const TimeOfDay(hour: 12, minute: 30);
  TimeOfDay _dinner = const TimeOfDay(hour: 19, minute: 30);
  TimeOfDay _snack = const TimeOfDay(hour: 16, minute: 0);
  bool _dailyMealReminder = true;
  bool _startCookingReminder = true;
  bool _lowStockAlert = true;
  final _thresholdController = TextEditingController(text: '1');
  bool _populated = false;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    if (!_loaded) {
      _loaded = true;
      Future.microtask(() => context.read<AppState>().loadNotificationSettings());
    }
    _maybePopulate(appState.notificationSettings);

    return Scaffold(
      appBar: AppBar(title: const Text('Notifications')),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          Text('Local reminders scheduled on this device for today\'s meal plan.',
              style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 12)),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Daily meal reminder'),
            value: _dailyMealReminder,
            onChanged: (v) => setState(() => _dailyMealReminder = v),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('"Start cooking by" reminder'),
            value: _startCookingReminder,
            onChanged: (v) => setState(() => _startCookingReminder = v),
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Lunch time'),
            trailing: Text(_lunch.format(context)),
            onTap: () => _pickTime(_lunch, (t) => _lunch = t),
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Dinner time'),
            trailing: Text(_dinner.format(context)),
            onTap: () => _pickTime(_dinner, (t) => _dinner = t),
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Snack time'),
            trailing: Text(_snack.format(context)),
            onTap: () => _pickTime(_snack, (t) => _snack = t),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Low stock alerts'),
            value: _lowStockAlert,
            onChanged: (v) => setState(() => _lowStockAlert = v),
          ),
          Row(children: [
            const Text('Alert when quantity below:'),
            const SizedBox(width: 8),
            SizedBox(
              width: 60,
              child: TextField(controller: _thresholdController, keyboardType: const TextInputType.numberWithOptions(decimal: true), decoration: const InputDecoration(isDense: true)),
            ),
          ]),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton(onPressed: _save, child: const Text('Save')),
          ),
        ],
      ),
    );
  }

  void _maybePopulate(NotificationSetting? settings) {
    if (_populated || settings == null) return;
    _populated = true;
    _lunch = _parseTime(settings.lunchTime) ?? _lunch;
    _dinner = _parseTime(settings.dinnerTime) ?? _dinner;
    _snack = _parseTime(settings.snackTime) ?? _snack;
    _dailyMealReminder = settings.dailyMealReminderEnabled;
    _startCookingReminder = settings.startCookingReminderEnabled;
    _lowStockAlert = settings.lowStockAlertEnabled;
    _thresholdController.text = settings.lowStockThreshold.toString();
  }

  TimeOfDay? _parseTime(String hhmmss) {
    final parts = hhmmss.split(':');
    if (parts.length < 2) return null;
    final h = int.tryParse(parts[0]);
    final m = int.tryParse(parts[1]);
    if (h == null || m == null) return null;
    return TimeOfDay(hour: h, minute: m);
  }

  String _formatTime(TimeOfDay t) => '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}:00';

  Future<void> _pickTime(TimeOfDay current, void Function(TimeOfDay) onPicked) async {
    final picked = await showTimePicker(context: context, initialTime: current);
    if (picked != null) setState(() => onPicked(picked));
  }

  void _save() {
    context.read<AppState>().updateNotificationSettings(
          lunchTime: _formatTime(_lunch),
          dinnerTime: _formatTime(_dinner),
          snackTime: _formatTime(_snack),
          dailyMealReminderEnabled: _dailyMealReminder,
          startCookingReminderEnabled: _startCookingReminder,
          lowStockAlertEnabled: _lowStockAlert,
          lowStockThreshold: double.tryParse(_thresholdController.text) ?? 1.0,
        );
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Notification settings saved.')));
  }
}
