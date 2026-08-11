import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/allergy.dart';
import '../models/app_user.dart';
import '../models/external_recipe.dart';
import '../models/household.dart';
import '../models/ingredient.dart';
import '../models/meal_plan.dart';
import '../models/missing_ingredient.dart';
import '../models/notification_setting.dart';
import '../models/nutrition_profile.dart';
import '../models/pantry_item.dart';
import '../models/recipe.dart';
import '../models/shopping_list_item.dart';
import '../models/taste.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/notification_service.dart';
import '../services/pantry_socket_service.dart';

class AppState extends ChangeNotifier {
  static const _tokenKey = 'auth_token';
  static const _householdIdKey = 'household_id';

  final _secureStorage = const FlutterSecureStorage();
  final _authService = AuthService();
  final _socketService = PantrySocketService();
  final _notificationService = NotificationService();
  late final ApiClient _api = ApiClient(() => token);
  final Set<String> _lowStockNotifiedItemIds = {};

  String? token;
  AppUser? currentUser;
  Household? currentHousehold;
  List<Household> myHouseholds = [];
  List<PantryItem> pantryItems = [];
  List<Recipe> recipes = [];
  List<ExternalRecipeSummary> externalRecipeResults = [];
  final Set<String> favoritedExternalIds = {};
  List<MealPlan> mealPlans = [];
  List<ShoppingListItem> shoppingList = [];
  List<MissingIngredient> missingIngredients = [];
  int shoppingListLookaheadDays = 7;
  List<Allergy> allAllergies = [];
  List<IngredientSuggestion> ingredientSuggestions = [];
  List<Ingredient> customIngredients = [];
  List<Taste> allTastes = [];
  List<Allergy> myAllergies = [];
  List<TastePreferenceEntry> myTastes = [];
  List<TastePreferenceEntry> householdTastes = [];
  List<Allergy> householdAllergies = [];
  NutritionProfile? nutritionProfile;
  NotificationSetting? notificationSettings;
  String? unitSystem;

  bool isLoading = true;
  String? errorMessage;

  // errorMessage alone can't represent the same error occurring twice in a row (setting a field
  // to a value it already holds doesn't produce a change), which is exactly why most screens
  // never picked it up at all -- only the handful that explicitly read the field in their own
  // build method saw it, and even those missed repeats. Every call to _setError below is instead
  // an event on this broadcast stream, which RootScreen listens to once, globally, regardless of
  // whether the message text repeats.
  final _errorEvents = StreamController<String>.broadcast();
  Stream<String> get errorStream => _errorEvents.stream;

  @override
  void dispose() {
    _errorEvents.close();
    super.dispose();
  }

  /// Records an error for both the legacy errorMessage field (still read inline by a handful of
  /// screens) and the global error stream, then notifies listeners. Replaces what was previously
  /// `errorMessage = '...'; notifyListeners();` repeated at every catch block in this file.
  void _setError(String message) {
    errorMessage = message;
    _errorEvents.add(message);
    notifyListeners();
  }

  Future<void> bootstrap() async {
    try {
      token = await _secureStorage.read(key: _tokenKey);
      if (token != null) {
        final json = await _api.get('/auth/me');
        currentUser = AppUser.fromJson(json as Map<String, dynamic>);
        final storedHouseholdId = await _secureStorage.read(key: _householdIdKey);
        if (storedHouseholdId != null) {
          await _loadHousehold(storedHouseholdId);
        }
      }
    } catch (_) {
      // Missing/expired token, unreachable backend, or no secure-storage platform
      // implementation (e.g. widget tests): fall back to a signed-out state.
      await _clearSession();
    }
    isLoading = false;
    notifyListeners();
  }

  Future<void> signInWithGoogle() async {
    errorMessage = null;
    try {
      final idToken = await _authService.signInAndGetIdToken();
      if (idToken == null) {
        return; // user cancelled
      }
      final json = await _api.post('/auth/google', {'idToken': idToken}) as Map<String, dynamic>;
      token = json['token'] as String;
      currentUser = AppUser.fromJson(json['user'] as Map<String, dynamic>);
      await _secureStorage.write(key: _tokenKey, value: token);
      notifyListeners();
    } catch (e) {
      _setError('Sign-in failed: $e');
    }
  }

  Future<void> signOut() async {
    _socketService.disconnect();
    await _authService.signOut();
    await _clearSession();
    notifyListeners();
  }

  Future<void> _clearSession() async {
    token = null;
    currentUser = null;
    currentHousehold = null;
    pantryItems = [];
    try {
      await _secureStorage.delete(key: _tokenKey);
      await _secureStorage.delete(key: _householdIdKey);
    } catch (_) {
      // Best-effort: in-memory session state above is what actually matters.
    }
  }

  Future<void> createHousehold(String name) async {
    errorMessage = null;
    try {
      final json = await _api.post('/households', {'name': name}) as Map<String, dynamic>;
      await _loadHousehold((json['id'] as String), preloaded: Household.fromJson(json));
    } catch (e) {
      _setError('Could not create household: $e');
    }
  }

  Future<void> joinHousehold(String inviteCode) async {
    errorMessage = null;
    try {
      final json = await _api.post('/households/join/${inviteCode.trim().toUpperCase()}') as Map<String, dynamic>;
      await _loadHousehold((json['id'] as String), preloaded: Household.fromJson(json));
    } catch (e) {
      _setError('Could not join household: $e');
    }
  }

  Future<void> loadMyHouseholds() async {
    try {
      final json = await _api.get('/users/me/households') as List<dynamic>;
      myHouseholds = json.map((j) => Household.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load your households: $e');
    }
  }

  Future<void> switchHousehold(String householdId) async {
    if (currentHousehold?.id == householdId) return;
    errorMessage = null;
    _socketService.disconnect();
    pantryItems = [];
    recipes = [];
    mealPlans = [];
    shoppingList = [];
    householdTastes = [];
    householdAllergies = [];
    _lowStockNotifiedItemIds.clear();
    try {
      await _loadHousehold(householdId);
    } catch (e) {
      _setError('Could not switch household: $e');
    }
  }

  Future<void> _loadHousehold(String householdId, {Household? preloaded}) async {
    currentHousehold = preloaded ?? Household.fromJson(await _api.get('/households/$householdId') as Map<String, dynamic>);
    await _secureStorage.write(key: _householdIdKey, value: currentHousehold!.id);
    await _notificationService.init();
    await loadPantry();
    await loadNotificationSettings();
    await refreshTodayNotifications();
    _connectSocket();
    notifyListeners();
  }

  Future<void> leaveHousehold(String householdId) async {
    errorMessage = null;
    try {
      await _api.post('/households/$householdId/leave');
      await _handleHouseholdRemoved(householdId);
    } catch (e) {
      _setError('Could not leave household: $e');
    }
  }

  /// Owner-only; deletes the household (and all its data) for every member, not just the caller.
  Future<void> deleteHousehold(String householdId) async {
    errorMessage = null;
    try {
      await _api.delete('/households/$householdId');
      await _handleHouseholdRemoved(householdId);
    } catch (e) {
      _setError('Could not delete household: $e');
    }
  }

  /// Shared by a local leave/delete and the HOUSEHOLD_DELETED socket event (another member deleted
  /// it while we're connected) -- drops it from the list and, if it was the active household,
  /// switches to another one we still belong to or falls back to the no-household onboarding screen.
  Future<void> _handleHouseholdRemoved(String householdId) async {
    myHouseholds = myHouseholds.where((h) => h.id != householdId).toList();
    if (currentHousehold?.id == householdId) {
      if (myHouseholds.isNotEmpty) {
        await switchHousehold(myHouseholds.first.id);
      } else {
        await _clearCurrentHousehold();
      }
    }
    notifyListeners();
  }

  Future<void> _clearCurrentHousehold() async {
    _socketService.disconnect();
    currentHousehold = null;
    pantryItems = [];
    recipes = [];
    mealPlans = [];
    shoppingList = [];
    householdTastes = [];
    householdAllergies = [];
    _lowStockNotifiedItemIds.clear();
    await _secureStorage.delete(key: _householdIdKey);
  }

  void _connectSocket() {
    if (token == null || currentHousehold == null) return;
    _socketService.connect(currentHousehold!.id, token!, _handleSocketEvent);
  }

  void _handleSocketEvent(Map<String, dynamic> event) {
    final type = event['type'] as String?;
    if (type == null) return;

    if (type.startsWith('PANTRY_ITEM_')) {
      final itemJson = event['item'] as Map<String, dynamic>?;
      if (itemJson == null) return;
      final item = PantryItem.fromJson(itemJson);
      switch (type) {
        case 'PANTRY_ITEM_CREATED':
          if (!pantryItems.any((i) => i.id == item.id)) {
            pantryItems = [...pantryItems, item];
          }
          break;
        case 'PANTRY_ITEM_UPDATED':
          pantryItems = pantryItems.map((i) => i.id == item.id ? item : i).toList();
          break;
        case 'PANTRY_ITEM_DELETED':
          pantryItems = pantryItems.where((i) => i.id != item.id).toList();
          break;
      }
    } else if (type.startsWith('MEAL_PLAN_')) {
      final mpJson = event['mealPlan'] as Map<String, dynamic>?;
      if (mpJson == null) return;
      final mp = MealPlan.fromJson(mpJson);
      switch (type) {
        case 'MEAL_PLAN_CREATED':
          if (!mealPlans.any((m) => m.id == mp.id)) {
            mealPlans = [...mealPlans, mp];
          }
          break;
        case 'MEAL_PLAN_UPDATED':
          mealPlans = mealPlans.map((m) => m.id == mp.id ? mp : m).toList();
          break;
        case 'MEAL_PLAN_DELETED':
          mealPlans = mealPlans.where((m) => m.id != mp.id).toList();
          break;
      }
    } else if (type.startsWith('SHOPPING_LIST_')) {
      if (type == 'SHOPPING_LIST_REGENERATED') {
        final itemsJson = event['items'] as List<dynamic>?;
        if (itemsJson == null) return;
        shoppingList = itemsJson.map((j) => ShoppingListItem.fromJson(j as Map<String, dynamic>)).toList();
      } else {
        final itemJson = event['item'] as Map<String, dynamic>?;
        if (itemJson == null) return;
        final item = ShoppingListItem.fromJson(itemJson);
        switch (type) {
          case 'SHOPPING_LIST_ITEM_CREATED':
            if (!shoppingList.any((i) => i.id == item.id)) {
              shoppingList = [...shoppingList, item];
            }
            break;
          case 'SHOPPING_LIST_ITEM_UPDATED':
            shoppingList = shoppingList.map((i) => i.id == item.id ? item : i).toList();
            break;
          case 'SHOPPING_LIST_ITEM_DELETED':
            shoppingList = shoppingList.where((i) => i.id != item.id).toList();
            break;
        }
      }
    } else if (type == 'HOUSEHOLD_DELETED') {
      // Someone else (the owner) deleted the household we're currently connected to -- react the
      // same way a local leave/delete does, rather than leaving the UI pointed at a dead household.
      final householdId = event['householdId'] as String?;
      if (householdId != null) {
        _handleHouseholdRemoved(householdId);
        return;
      }
    }
    notifyListeners();
  }

  Future<void> loadPantry() async {
    if (currentHousehold == null) return;
    final json = await _api.get('/households/${currentHousehold!.id}/pantry') as List<dynamic>;
    pantryItems = json.map((j) => PantryItem.fromJson(j as Map<String, dynamic>)).toList();
    _checkLowStock();
    notifyListeners();
  }

  void _checkLowStock() {
    final settings = notificationSettings;
    if (settings == null || !settings.lowStockAlertEnabled) return;
    for (final item in pantryItems) {
      if (item.quantity >= settings.lowStockThreshold) continue;
      if (!_lowStockNotifiedItemIds.add(item.id)) continue; // already alerted this session
      _notificationService.showLowStockAlert(
        id: 1000 + item.id.hashCode.remainder(100000),
        title: 'Running low: ${item.ingredientName ?? 'pantry item'}',
        body: 'Only ${item.quantity} ${item.unit} left in the pantry.',
      );
    }
  }

  Future<void> addPantryItem({
    String? ingredientId,
    String? ingredientName,
    String? ingredientBarcode,
    String? ingredientImageUrl,
    String? ingredientCategory,
    double? ingredientCaloriesPer100g,
    double? ingredientProteinPer100g,
    double? ingredientCarbsPer100g,
    double? ingredientFatPer100g,
    required String location,
    required double quantity,
    required String unit,
    String? expirationDate,
  }) async {
    if (currentHousehold == null) return;
    try {
      await _api.post('/households/${currentHousehold!.id}/pantry', {
        'ingredientId': ?ingredientId,
        'ingredientName': ?ingredientName,
        'ingredientBarcode': ?ingredientBarcode,
        'ingredientImageUrl': ?ingredientImageUrl,
        'ingredientCategory': ?ingredientCategory,
        'ingredientCaloriesPer100g': ?ingredientCaloriesPer100g,
        'ingredientProteinPer100g': ?ingredientProteinPer100g,
        'ingredientCarbsPer100g': ?ingredientCarbsPer100g,
        'ingredientFatPer100g': ?ingredientFatPer100g,
        'location': location,
        'quantity': quantity,
        'unit': unit,
        'expirationDate': ?expirationDate,
      });
      // The item also arrives via the WebSocket broadcast; no local mutation needed here.
    } catch (e) {
      _setError('Could not add item: $e');
    }
  }

  /// Live type-ahead search; fails silently (empty results) rather than surfacing a persistent
  /// error banner every time a per-keystroke network call hiccups.
  Future<void> searchIngredients(String query) async {
    if (query.trim().isEmpty || currentHousehold == null) {
      ingredientSuggestions = [];
      notifyListeners();
      return;
    }
    try {
      final json = await _api.get('/pantry/ingredients/search'
          '?query=${Uri.encodeQueryComponent(query.trim())}&householdId=${currentHousehold!.id}') as List<dynamic>;
      ingredientSuggestions = json.map((j) => IngredientSuggestion.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (_) {
      // best-effort
    }
  }

  void clearIngredientSuggestions() {
    if (ingredientSuggestions.isEmpty) return;
    ingredientSuggestions = [];
    notifyListeners();
  }

  /// Resolves a scanned barcode to an Ingredient via Open Food Facts (find-or-create server-side).
  /// Returns null (and sets errorMessage) if the barcode isn't recognized.
  Future<Ingredient?> scanBarcode(String barcode) async {
    try {
      final json = await _api.post('/pantry/scan-barcode', {'barcode': barcode}) as Map<String, dynamic>;
      return Ingredient.fromJson(json);
    } catch (e) {
      _setError('Barcode not recognized: $e');
      return null;
    }
  }

  /// Fetches full details (image, nutrition, custom tag) for a single ingredient by id --
  /// used by the ingredient detail view linked to from recipe ingredient lists.
  Future<Ingredient?> getIngredient(String id) async {
    try {
      final json = await _api.get('/pantry/ingredients/$id') as Map<String, dynamic>;
      return Ingredient.fromJson(json);
    } catch (e) {
      _setError('Could not load ingredient: $e');
      return null;
    }
  }

  /// One-off name lookup (e.g. tapping an ingredient on a not-yet-favorited external recipe,
  /// which has no ingredient id of its own). Returns results directly rather than touching
  /// [ingredientSuggestions], which backs the unrelated live-search dropdown. Fails silently
  /// (empty results) rather than surfacing an error banner for what's just a "no match" case.
  Future<List<IngredientSuggestion>> lookupIngredientByName(String name) async {
    if (currentHousehold == null) return [];
    try {
      final json = await _api.get('/pantry/ingredients/search'
          '?query=${Uri.encodeQueryComponent(name.trim())}&householdId=${currentHousehold!.id}') as List<dynamic>;
      return json.map((j) => IngredientSuggestion.fromJson(j as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> updatePantryItem(
    PantryItem item, {
    String? location,
    double? quantity,
    String? unit,
    String? ingredientName,
    String? ingredientImageUrl,
    double? ingredientCaloriesPer100g,
    double? ingredientProteinPer100g,
    double? ingredientCarbsPer100g,
    double? ingredientFatPer100g,
  }) async {
    try {
      await _api.patch('/pantry/${item.id}', {
        'location': ?location,
        'quantity': ?quantity,
        'unit': ?unit,
        'ingredientName': ?ingredientName,
        'ingredientImageUrl': ?ingredientImageUrl,
        'ingredientCaloriesPer100g': ?ingredientCaloriesPer100g,
        'ingredientProteinPer100g': ?ingredientProteinPer100g,
        'ingredientCarbsPer100g': ?ingredientCarbsPer100g,
        'ingredientFatPer100g': ?ingredientFatPer100g,
      });
    } catch (e) {
      _setError('Could not update item: $e');
    }
  }

  Future<void> deletePantryItem(String itemId) async {
    try {
      await _api.delete('/pantry/$itemId');
    } catch (e) {
      _setError('Could not delete item: $e');
    }
  }

  Future<void> loadCustomIngredients() async {
    if (currentHousehold == null) return;
    try {
      final json = await _api.get('/households/${currentHousehold!.id}/custom-ingredients') as List<dynamic>;
      customIngredients = json.map((j) => Ingredient.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load custom ingredients: $e');
    }
  }

  Future<bool> createCustomIngredient({
    required String name,
    String? imageUrl,
    double? caloriesPer100g,
    double? proteinPer100g,
    double? carbsPer100g,
    double? fatPer100g,
  }) async {
    if (currentHousehold == null) return false;
    try {
      await _api.post('/households/${currentHousehold!.id}/custom-ingredients', {
        'name': name,
        'imageUrl': ?imageUrl,
        'caloriesPer100g': ?caloriesPer100g,
        'proteinPer100g': ?proteinPer100g,
        'carbsPer100g': ?carbsPer100g,
        'fatPer100g': ?fatPer100g,
      });
      await loadCustomIngredients();
      return true;
    } catch (e) {
      _setError('Could not create ingredient: $e');
      return false;
    }
  }

  Future<bool> updateCustomIngredient({
    required String id,
    required String name,
    String? imageUrl,
    double? caloriesPer100g,
    double? proteinPer100g,
    double? carbsPer100g,
    double? fatPer100g,
  }) async {
    if (currentHousehold == null) return false;
    try {
      await _api.patch('/households/${currentHousehold!.id}/custom-ingredients/$id', {
        'name': name,
        'imageUrl': ?imageUrl,
        'caloriesPer100g': ?caloriesPer100g,
        'proteinPer100g': ?proteinPer100g,
        'carbsPer100g': ?carbsPer100g,
        'fatPer100g': ?fatPer100g,
      });
      await loadCustomIngredients();
      return true;
    } catch (e) {
      _setError('Could not update ingredient: $e');
      return false;
    }
  }

  Future<bool> deleteCustomIngredient(String id) async {
    if (currentHousehold == null) return false;
    try {
      await _api.delete('/households/${currentHousehold!.id}/custom-ingredients/$id');
      customIngredients = customIngredients.where((i) => i.id != id).toList();
      notifyListeners();
      return true;
    } catch (e) {
      _setError('Could not delete ingredient: $e');
      return false;
    }
  }

  void leaveHouseholdView() {
    _socketService.disconnect();
    currentHousehold = null;
    pantryItems = [];
    notifyListeners();
  }

  // --- Recipes ---

  Future<void> loadRecipes({List<String>? tags, String? stockFilter}) async {
    try {
      final query = <String>[];
      if (currentHousehold != null) query.add('householdId=${currentHousehold!.id}');
      if (tags != null) {
        for (final t in tags) {
          query.add('tags=${Uri.encodeQueryComponent(t)}');
        }
      }
      if (stockFilter != null) query.add('stockFilter=$stockFilter');
      final path = '/recipes${query.isEmpty ? '' : '?${query.join('&')}'}';
      final json = await _api.get(path) as List<dynamic>;
      recipes = json.map((j) => Recipe.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load recipes: $e');
    }
  }

  Future<Recipe?> createRecipe({
    required String name,
    required String category,
    String? instructions,
    required int servings,
    int? prepTimeMinutes,
    int? cookTimeMinutes,
    double? caloriesPerServing,
    double? proteinPerServing,
    double? carbsPerServing,
    double? fatPerServing,
    required bool isPrivate,
    required List<Map<String, dynamic>> ingredients,
    required List<Map<String, dynamic>> tags,
  }) async {
    try {
      final json = await _api.post('/recipes', {
        'name': name,
        'category': category,
        'instructions': ?instructions,
        'servings': servings,
        'prepTimeMinutes': ?prepTimeMinutes,
        'cookTimeMinutes': ?cookTimeMinutes,
        'caloriesPerServing': ?caloriesPerServing,
        'proteinPerServing': ?proteinPerServing,
        'carbsPerServing': ?carbsPerServing,
        'fatPerServing': ?fatPerServing,
        'isPrivate': isPrivate,
        'ingredients': ingredients,
        'tags': tags,
      }) as Map<String, dynamic>;
      final created = Recipe.fromJson(json);
      await loadRecipes();
      return created;
    } catch (e) {
      _setError('Could not create recipe: $e');
      return null;
    }
  }

  Future<bool> uploadRecipeImage(String recipeId, File file) async {
    try {
      final json = await _api.uploadFile('/recipes/$recipeId/image', 'file', file) as Map<String, dynamic>;
      final updated = Recipe.fromJson(json);
      recipes = recipes.map((r) => r.id == updated.id ? updated : r).toList();
      notifyListeners();
      return true;
    } catch (e) {
      _setError('Could not upload photo: $e');
      return false;
    }
  }

  Future<void> deleteRecipe(String recipeId) async {
    try {
      await _api.delete('/recipes/$recipeId');
      recipes = recipes.where((r) => r.id != recipeId).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not delete recipe: $e');
    }
  }

  Future<void> searchExternalRecipes(String query) async {
    if (query.trim().isEmpty) {
      externalRecipeResults = [];
      notifyListeners();
      return;
    }
    try {
      final json = await _api.get('/external-recipes/search?query=${Uri.encodeQueryComponent(query.trim())}') as List<dynamic>;
      externalRecipeResults = json.map((j) => ExternalRecipeSummary.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not search recipes: $e');
    }
  }

  Future<Recipe?> fetchRecipe(String id) async {
    try {
      final json = await _api.get('/recipes/$id') as Map<String, dynamic>;
      return Recipe.fromJson(json);
    } catch (e) {
      _setError('Could not load recipe: $e');
      return null;
    }
  }

  /// Live preview of an external recipe; not stored anywhere, nothing is saved server-side either.
  Future<ExternalRecipeDetail?> fetchExternalRecipeDetail(String externalId) async {
    try {
      final json = await _api.get('/external-recipes/$externalId') as Map<String, dynamic>;
      return ExternalRecipeDetail.fromJson(json);
    } catch (e) {
      _setError('Could not load recipe details: $e');
      return null;
    }
  }

  Future<bool> favoriteExternalRecipe(String externalId) async {
    try {
      final json = await _api.post('/external-recipes/$externalId/favorite') as Map<String, dynamic>;
      final recipe = Recipe.fromJson(json);
      if (!recipes.any((r) => r.id == recipe.id)) {
        recipes = [...recipes, recipe];
      }
      favoritedExternalIds.add(externalId);
      notifyListeners();
      return true;
    } catch (e) {
      _setError('Could not save recipe: $e');
      return false;
    }
  }

  // --- Meal plan ---

  Future<void> loadMealPlans(DateTime from, DateTime to) async {
    if (currentHousehold == null) return;
    try {
      final fromStr = _formatDate(from);
      final toStr = _formatDate(to);
      final json = await _api.get(
          '/households/${currentHousehold!.id}/meal-plan?from=$fromStr&to=$toStr') as List<dynamic>;
      mealPlans = json.map((j) => MealPlan.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load meal plan: $e');
    }
  }

  Future<bool> createMealPlan({
    required DateTime date,
    required String mealType,
    required String recipeId,
    List<Map<String, dynamic>>? portions,
  }) async {
    if (currentHousehold == null) return false;
    try {
      await _api.post('/households/${currentHousehold!.id}/meal-plan', {
        'date': _formatDate(date),
        'mealType': mealType,
        'recipeId': recipeId,
        'portions': ?portions,
      });
      // Also arrives via the WebSocket broadcast, but that's not guaranteed to have landed yet by the time we return.
      if (_isToday(date)) await refreshTodayNotifications();
      return true;
    } catch (e) {
      _setError('Could not assign meal: $e');
      return false;
    }
  }

  Future<void> deleteMealPlan(String mealPlanId) async {
    try {
      await _api.delete('/meal-plan/$mealPlanId');
      final deletedDate = mealPlans.where((m) => m.id == mealPlanId).map((m) => m.date).toList();
      mealPlans = mealPlans.where((m) => m.id != mealPlanId).toList();
      if (deletedDate.isNotEmpty && deletedDate.first == _formatDate(DateTime.now())) {
        await refreshTodayNotifications();
      }
      notifyListeners();
    } catch (e) {
      _setError('Could not remove meal: $e');
    }
  }

  bool _isToday(DateTime d) => _formatDate(d) == _formatDate(DateTime.now());

  Future<bool> autoGenerateSchedule({
    required DateTime from,
    required DateTime to,
    required List<String> mealTypes,
    String stockFilter = 'NONE',
    int avoidRepeatDays = 7,
    bool targetNutrition = false,
    bool regenerate = false,
  }) async {
    if (currentHousehold == null) return false;
    try {
      final json = await _api.post('/households/${currentHousehold!.id}/meal-plan/auto-generate', {
        'from': _formatDate(from),
        'to': _formatDate(to),
        'mealTypes': mealTypes,
        'stockFilter': stockFilter,
        'avoidRepeatDays': avoidRepeatDays,
        'targetNutrition': targetNutrition,
        'regenerate': regenerate,
      }) as List<dynamic>;
      final created = json.map((j) => MealPlan.fromJson(j as Map<String, dynamic>)).toList();
      bool touchedToday = false;
      for (final mp in created) {
        // On regenerate, the server deleted whatever previously occupied this date+mealType; the
        // WS broadcast for that will land eventually, but replace it locally now for a snappy UI
        // instead of a stale duplicate flashing until the round trip completes.
        mealPlans = [
          ...mealPlans.where((m) => m.id != mp.id && !(regenerate && m.date == mp.date && m.mealType == mp.mealType)),
          mp,
        ];
        if (mp.date == _formatDate(DateTime.now())) touchedToday = true;
      }
      if (touchedToday) await refreshTodayNotifications();
      notifyListeners();
      return true;
    } catch (e) {
      _setError('Could not auto-generate schedule: $e');
      return false;
    }
  }

  /// Best-effort: returns null (silently) rather than surfacing an error banner, since this is a
  /// secondary info panel on the day page, not something that should block viewing/editing meals.
  Future<DayNutritionSummary?> getDayNutritionSummary(DateTime date) async {
    if (currentHousehold == null) return null;
    try {
      final json = await _api.get(
          '/households/${currentHousehold!.id}/meal-plan/day-summary?date=${_formatDate(date)}') as Map<String, dynamic>;
      return DayNutritionSummary.fromJson(json);
    } catch (_) {
      return null;
    }
  }

  Future<void> confirmCooked(String mealPlanId) async {
    try {
      final json = await _api.post('/meal-plan/$mealPlanId/confirm-cooked') as Map<String, dynamic>;
      final updated = MealPlan.fromJson(json);
      mealPlans = mealPlans.map((m) => m.id == updated.id ? updated : m).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not mark cooked: $e');
    }
  }

  // --- Shopping list ---

  Future<void> loadShoppingList() async {
    if (currentHousehold == null) return;
    try {
      final json = await _api.get('/households/${currentHousehold!.id}/shopping-list') as List<dynamic>;
      shoppingList = json.map((j) => ShoppingListItem.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load shopping list: $e');
    }
  }

  Future<void> generateShoppingList(DateTime from, DateTime to) async {
    if (currentHousehold == null) return;
    try {
      final json = await _api.post(
          '/households/${currentHousehold!.id}/shopping-list/generate?from=${_formatDate(from)}&to=${_formatDate(to)}') as List<dynamic>;
      shoppingList = json.map((j) => ShoppingListItem.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not generate shopping list: $e');
    }
  }

  Future<void> addShoppingListItem({required String ingredientName, required double quantity, required String unit}) async {
    if (currentHousehold == null) return;
    try {
      await _api.post('/households/${currentHousehold!.id}/shopping-list', {
        'ingredientName': ingredientName,
        'quantity': quantity,
        'unit': unit,
      });
    } catch (e) {
      _setError('Could not add item: $e');
    }
  }

  Future<void> toggleShoppingListItem(String itemId, bool checked) async {
    try {
      await _api.patch('/shopping-list/$itemId', {'checked': checked});
    } catch (e) {
      _setError('Could not update item: $e');
    }
  }

  Future<void> deleteShoppingListItem(String itemId) async {
    try {
      await _api.delete('/shopping-list/$itemId');
      shoppingList = shoppingList.where((i) => i.id != itemId).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not remove item: $e');
    }
  }

  String _formatDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  // --- Missing-for-future-recipes preview ---

  Future<void> loadShoppingPreferences() async {
    try {
      final json = await _api.get('/users/me/shopping-preferences') as Map<String, dynamic>;
      shoppingListLookaheadDays = json['missingIngredientsLookaheadDays'] as int;
      notifyListeners();
    } catch (e) {
      _setError('Could not load shopping preferences: $e');
    }
  }

  Future<void> updateShoppingPreferences(int days) async {
    try {
      final json = await _api.patch('/users/me/shopping-preferences', {'missingIngredientsLookaheadDays': days}) as Map<String, dynamic>;
      shoppingListLookaheadDays = json['missingIngredientsLookaheadDays'] as int;
      notifyListeners();
    } catch (e) {
      _setError('Could not save shopping preferences: $e');
    }
  }

  Future<void> loadMissingIngredients() async {
    if (currentHousehold == null) return;
    try {
      final json = await _api.get(
          '/households/${currentHousehold!.id}/shopping-list/missing-upcoming?days=$shoppingListLookaheadDays') as List<dynamic>;
      missingIngredients = json.map((j) => MissingIngredient.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load missing ingredients: $e');
    }
  }

  /// Removes the items from [missingIngredients] immediately (before the network call resolves),
  /// restoring them on failure. A `Dismissible` requires its item gone from the underlying list
  /// synchronously within the frame its onDismissed fires -- waiting for the API response first
  /// leaves the dismissed widget "still part of the tree" and Flutter throws.
  Future<void> ignoreMissingIngredients(List<MissingIngredient> items) async {
    if (currentHousehold == null) return;
    final keys = items.map((i) => i.key).toSet();
    final removed = missingIngredients.where((i) => keys.contains(i.key)).toList();
    missingIngredients = missingIngredients.where((i) => !keys.contains(i.key)).toList();
    notifyListeners();
    try {
      await _api.post('/households/${currentHousehold!.id}/shopping-list/missing-upcoming/ignore', {
        'items': items.map((i) => {'ingredientId': i.ingredientId, 'unit': i.unit}).toList(),
      });
    } catch (e) {
      missingIngredients = [...missingIngredients, ...removed];
      _setError('Could not ignore ingredient(s): $e');
    }
  }

  Future<void> addMissingIngredientsToList(List<MissingIngredient> items) async {
    if (currentHousehold == null) return;
    final keys = items.map((i) => i.key).toSet();
    final removed = missingIngredients.where((i) => keys.contains(i.key)).toList();
    missingIngredients = missingIngredients.where((i) => !keys.contains(i.key)).toList();
    notifyListeners();
    try {
      final json = await _api.post('/households/${currentHousehold!.id}/shopping-list/missing-upcoming/add', {
        'items': items.map((i) => {'ingredientId': i.ingredientId, 'quantity': i.quantity, 'unit': i.unit}).toList(),
      }) as List<dynamic>;
      shoppingList = json.map((j) => ShoppingListItem.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      missingIngredients = [...missingIngredients, ...removed];
      _setError('Could not add ingredient(s) to shopping list: $e');
    }
  }

  // --- Notifications ---

  Future<void> loadNotificationSettings() async {
    try {
      final json = await _api.get('/users/me/notification-settings') as Map<String, dynamic>;
      notificationSettings = NotificationSetting.fromJson(json);
      notifyListeners();
    } catch (e) {
      _setError('Could not load notification settings: $e');
    }
  }

  Future<void> updateNotificationSettings({
    String? lunchTime,
    String? dinnerTime,
    String? snackTime,
    bool? dailyMealReminderEnabled,
    bool? startCookingReminderEnabled,
    bool? lowStockAlertEnabled,
    double? lowStockThreshold,
  }) async {
    try {
      final json = await _api.patch('/users/me/notification-settings', {
        'lunchTime': ?lunchTime,
        'dinnerTime': ?dinnerTime,
        'snackTime': ?snackTime,
        'dailyMealReminderEnabled': ?dailyMealReminderEnabled,
        'startCookingReminderEnabled': ?startCookingReminderEnabled,
        'lowStockAlertEnabled': ?lowStockAlertEnabled,
        'lowStockThreshold': ?lowStockThreshold,
      }) as Map<String, dynamic>;
      notificationSettings = NotificationSetting.fromJson(json);
      _lowStockNotifiedItemIds.clear(); // threshold/toggle may have changed; let items re-qualify
      await refreshTodayNotifications();
      notifyListeners();
    } catch (e) {
      _setError('Could not save notification settings: $e');
    }
  }

  /// (Re)schedules today's meal + start-cooking reminders from whatever is currently on the meal plan.
  Future<void> refreshTodayNotifications() async {
    if (currentHousehold == null) return;
    final settings = notificationSettings ?? await _fetchNotificationSettingsSilently();
    if (settings == null) return;

    final today = DateTime.now();
    final todayStr = _formatDate(today);
    List<MealPlan> todaysPlans;
    try {
      final json = await _api.get('/households/${currentHousehold!.id}/meal-plan?from=$todayStr&to=$todayStr') as List<dynamic>;
      todaysPlans = json.map((j) => MealPlan.fromJson(j as Map<String, dynamic>)).toList();
    } catch (_) {
      return; // best-effort; leave any previously scheduled notifications in place
    }

    for (int i = 0; i < mealTypes.length; i++) {
      final mealType = mealTypes[i];
      final mealReminderId = i + 1; // 1,2,3
      final cookReminderId = i + 11; // 11,12,13
      await _notificationService.cancel(mealReminderId);
      await _notificationService.cancel(cookReminderId);

      MealPlan? plan;
      for (final m in todaysPlans) {
        if (m.mealType == mealType) {
          plan = m;
          break;
        }
      }
      if (plan == null) continue;

      final mealTime = _parseTimeToday(settings.timeFor(mealType), today);
      if (mealTime == null) continue;

      if (settings.dailyMealReminderEnabled) {
        await _notificationService.scheduleAt(
          id: mealReminderId,
          title: 'Time for ${mealType.toLowerCase()}',
          body: plan.recipeName ?? 'Check your meal plan',
          localTime: mealTime,
        );
      }

      if (settings.startCookingReminderEnabled && plan.prepTimeMinutes != null && plan.cookTimeMinutes != null) {
        final startBy = mealTime.subtract(Duration(minutes: plan.prepTimeMinutes! + plan.cookTimeMinutes!));
        await _notificationService.scheduleAt(
          id: cookReminderId,
          title: 'Start cooking now',
          body: '${plan.recipeName ?? 'Your meal'} takes ${plan.prepTimeMinutes! + plan.cookTimeMinutes!} min to be ready by ${settings.timeFor(mealType).substring(0, 5)}.',
          localTime: startBy,
        );
      }
    }
  }

  Future<NotificationSetting?> _fetchNotificationSettingsSilently() async {
    try {
      final json = await _api.get('/users/me/notification-settings') as Map<String, dynamic>;
      notificationSettings = NotificationSetting.fromJson(json);
      return notificationSettings;
    } catch (_) {
      return null;
    }
  }

  DateTime? _parseTimeToday(String hhmmss, DateTime day) {
    final parts = hhmmss.split(':');
    if (parts.length < 2) return null;
    final hour = int.tryParse(parts[0]);
    final minute = int.tryParse(parts[1]);
    if (hour == null || minute == null) return null;
    return DateTime(day.year, day.month, day.day, hour, minute);
  }

  // --- Nutrition profile ---

  Future<void> loadNutritionProfile() async {
    try {
      final json = await _api.get('/users/me/nutrition-profile') as Map<String, dynamic>;
      nutritionProfile = NutritionProfile.fromJson(json);
      notifyListeners();
    } catch (e) {
      _setError('Could not load nutrition profile: $e');
    }
  }

  Future<void> updateNutritionProfile({
    String? gender,
    int? age,
    double? weight,
    double? height,
    double? calorieTarget,
    double? proteinTargetGrams,
    double? carbsTargetGrams,
    double? fatTargetGrams,
    double? weightGoalKg,
  }) async {
    try {
      final json = await _api.patch('/users/me/nutrition-profile', {
        'gender': ?gender,
        'age': ?age,
        'weight': ?weight,
        'height': ?height,
        'calorieTarget': ?calorieTarget,
        'proteinTargetGrams': ?proteinTargetGrams,
        'carbsTargetGrams': ?carbsTargetGrams,
        'fatTargetGrams': ?fatTargetGrams,
        'weightGoalKg': ?weightGoalKg,
      }) as Map<String, dynamic>;
      nutritionProfile = NutritionProfile.fromJson(json);
      notifyListeners();
    } catch (e) {
      _setError('Could not save nutrition profile: $e');
    }
  }

  // --- Unit system preference ---

  Future<void> loadUnitSystem() async {
    try {
      final json = await _api.get('/users/me/unit-system') as Map<String, dynamic>;
      unitSystem = json['unitSystem'] as String;
      notifyListeners();
    } catch (e) {
      _setError('Could not load unit system preference: $e');
    }
  }

  Future<void> updateUnitSystem(String value) async {
    try {
      final json = await _api.patch('/users/me/unit-system', {'unitSystem': value}) as Map<String, dynamic>;
      unitSystem = json['unitSystem'] as String;
      notifyListeners();
    } catch (e) {
      _setError('Could not save unit system preference: $e');
    }
  }

  // --- Preferences (allergies & tastes) ---

  Future<void> loadReferenceData() async {
    try {
      final allergyJson = await _api.get('/allergies') as List<dynamic>;
      final tasteJson = await _api.get('/tastes') as List<dynamic>;
      allAllergies = allergyJson.map((j) => Allergy.fromJson(j as Map<String, dynamic>)).toList();
      allTastes = tasteJson.map((j) => Taste.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load reference data: $e');
    }
  }

  Future<void> loadMyPreferences() async {
    try {
      final allergyJson = await _api.get('/users/me/allergies') as List<dynamic>;
      final tasteJson = await _api.get('/users/me/tastes') as List<dynamic>;
      myAllergies = allergyJson.map((j) => Allergy.fromJson(j as Map<String, dynamic>)).toList();
      myTastes = tasteJson.map((j) => TastePreferenceEntry.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load your preferences: $e');
    }
  }

  Future<void> addMyAllergy(String allergyId) async {
    try {
      await _api.put('/users/me/allergies/$allergyId');
      await loadMyPreferences();
    } catch (e) {
      _setError('Could not add allergy: $e');
    }
  }

  Future<void> removeMyAllergy(String allergyId) async {
    try {
      await _api.delete('/users/me/allergies/$allergyId');
      await loadMyPreferences();
    } catch (e) {
      _setError('Could not remove allergy: $e');
    }
  }

  Future<void> setMyTaste(String tasteId, String preference) async {
    try {
      await _api.put('/users/me/tastes/$tasteId', {'preference': preference});
      await loadMyPreferences();
    } catch (e) {
      _setError('Could not set taste preference: $e');
    }
  }

  Future<void> removeMyTaste(String tasteId) async {
    try {
      await _api.delete('/users/me/tastes/$tasteId');
      await loadMyPreferences();
    } catch (e) {
      _setError('Could not remove taste preference: $e');
    }
  }

  Future<void> loadHouseholdTastes(String householdId) async {
    try {
      final json = await _api.get('/households/$householdId/tastes') as List<dynamic>;
      householdTastes = json.map((j) => TastePreferenceEntry.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load household preferences: $e');
    }
  }

  Future<void> setHouseholdTaste(String householdId, String tasteId, String preference) async {
    try {
      await _api.put('/households/$householdId/tastes/$tasteId', {'preference': preference});
      await loadHouseholdTastes(householdId);
    } catch (e) {
      _setError('Could not set household taste preference: $e');
    }
  }

  Future<void> removeHouseholdTaste(String householdId, String tasteId) async {
    try {
      await _api.delete('/households/$householdId/tastes/$tasteId');
      await loadHouseholdTastes(householdId);
    } catch (e) {
      _setError('Could not remove household taste preference: $e');
    }
  }

  Future<void> loadHouseholdAllergies(String householdId) async {
    try {
      final json = await _api.get('/households/$householdId/allergies') as List<dynamic>;
      householdAllergies = json.map((j) => Allergy.fromJson(j as Map<String, dynamic>)).toList();
      notifyListeners();
    } catch (e) {
      _setError('Could not load household allergies: $e');
    }
  }

  Future<void> addHouseholdAllergy(String householdId, String allergyId) async {
    try {
      await _api.put('/households/$householdId/allergies/$allergyId');
      await loadHouseholdAllergies(householdId);
    } catch (e) {
      _setError('Could not add household allergy: $e');
    }
  }

  Future<void> removeHouseholdAllergy(String householdId, String allergyId) async {
    try {
      await _api.delete('/households/$householdId/allergies/$allergyId');
      await loadHouseholdAllergies(householdId);
    } catch (e) {
      _setError('Could not remove household allergy: $e');
    }
  }
}
