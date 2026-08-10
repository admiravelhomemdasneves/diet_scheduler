import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import '../config.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;
  ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

class ApiClient {
  final String? Function() tokenProvider;
  ApiClient(this.tokenProvider);

  Uri _uri(String path) => Uri.parse('${ApiConfig.baseHttpUrl}$path');

  Map<String, String> _headers() {
    final headers = <String, String>{'Content-Type': 'application/json'};
    final token = tokenProvider();
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  Future<dynamic> get(String path) async {
    final res = await http.get(_uri(path), headers: _headers());
    return _handle(res);
  }

  Future<dynamic> post(String path, [Map<String, dynamic>? body]) async {
    final res = await http.post(_uri(path), headers: _headers(), body: body != null ? jsonEncode(body) : null);
    return _handle(res);
  }

  Future<dynamic> patch(String path, Map<String, dynamic> body) async {
    final res = await http.patch(_uri(path), headers: _headers(), body: jsonEncode(body));
    return _handle(res);
  }

  Future<dynamic> put(String path, [Map<String, dynamic>? body]) async {
    final res = await http.put(_uri(path), headers: _headers(), body: body != null ? jsonEncode(body) : null);
    return _handle(res);
  }

  Future<void> delete(String path) async {
    final res = await http.delete(_uri(path), headers: _headers());
    _handle(res);
  }

  Future<dynamic> uploadFile(String path, String fieldName, File file) async {
    final request = http.MultipartRequest('POST', _uri(path));
    final token = tokenProvider();
    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    request.files.add(await http.MultipartFile.fromPath(fieldName, file.path, contentType: _imageMediaType(file.path)));
    final streamedResponse = await request.send();
    final res = await http.Response.fromStream(streamedResponse);
    return _handle(res);
  }

  /// Matches the extensions the backend's image upload endpoint actually accepts.
  MediaType _imageMediaType(String filePath) {
    final ext = filePath.toLowerCase().split('.').last;
    switch (ext) {
      case 'png':
        return MediaType('image', 'png');
      case 'webp':
        return MediaType('image', 'webp');
      default:
        return MediaType('image', 'jpeg');
    }
  }

  dynamic _handle(http.Response res) {
    if (res.statusCode >= 200 && res.statusCode < 300) {
      if (res.body.isEmpty) return null;
      return jsonDecode(res.body);
    }
    String message = 'Request failed (${res.statusCode})';
    try {
      final parsed = jsonDecode(res.body);
      if (parsed is Map && parsed['message'] != null) {
        message = parsed['message'] as String;
      }
    } catch (_) {
      // response body wasn't JSON; fall back to the generic message
    }
    throw ApiException(res.statusCode, message);
  }
}
