class AppUser {
  final String id;
  final String email;
  final String displayName;

  AppUser({required this.id, required this.email, required this.displayName});

  factory AppUser.fromJson(Map<String, dynamic> json) => AppUser(
        id: json['id'] as String,
        email: json['email'] as String,
        displayName: json['displayName'] as String,
      );
}
