class HouseholdMember {
  final String userId;
  final String? displayName;
  final String? email;
  final String role;

  HouseholdMember({required this.userId, this.displayName, this.email, required this.role});

  factory HouseholdMember.fromJson(Map<String, dynamic> json) => HouseholdMember(
        userId: json['userId'] as String,
        displayName: json['displayName'] as String?,
        email: json['email'] as String?,
        role: json['role'] as String,
      );
}

class Household {
  final String id;
  final String name;
  final String inviteCode;
  final List<HouseholdMember> members;

  Household({required this.id, required this.name, required this.inviteCode, required this.members});

  factory Household.fromJson(Map<String, dynamic> json) => Household(
        id: json['id'] as String,
        name: json['name'] as String,
        inviteCode: json['inviteCode'] as String,
        members: (json['members'] as List<dynamic>)
            .map((m) => HouseholdMember.fromJson(m as Map<String, dynamic>))
            .toList(),
      );
}
