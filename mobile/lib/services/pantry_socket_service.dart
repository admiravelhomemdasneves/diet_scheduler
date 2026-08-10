import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../config.dart';

/// Broadcast-only channel: the server pushes pantry change events, the client never sends messages.
class PantrySocketService {
  WebSocketChannel? _channel;
  StreamSubscription? _subscription;

  void connect(String householdId, String token, void Function(Map<String, dynamic> event) onEvent) {
    disconnect();
    final uri = Uri.parse('${ApiConfig.baseWsUrl}/ws/households/$householdId?token=$token');
    final channel = WebSocketChannel.connect(uri);
    _channel = channel;
    _subscription = channel.stream.listen(
      (message) {
        final data = jsonDecode(message as String) as Map<String, dynamic>;
        onEvent(data);
      },
      onError: (_) {},
      onDone: () {},
      cancelOnError: false,
    );
  }

  void disconnect() {
    _subscription?.cancel();
    _subscription = null;
    _channel?.sink.close();
    _channel = null;
  }
}
