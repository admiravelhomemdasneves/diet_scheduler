package com.dietscheduler.backend.seed;

import com.dietscheduler.backend.pantry.Ingredient;
import com.dietscheduler.backend.pantry.IngredientRepository;
import com.dietscheduler.backend.preferences.Allergy;
import com.dietscheduler.backend.preferences.AllergyRepository;
import com.dietscheduler.backend.preferences.Taste;
import com.dietscheduler.backend.preferences.TasteRepository;
import com.dietscheduler.backend.preferences.TasteType;
import com.dietscheduler.backend.recipe.Recipe;
import com.dietscheduler.backend.recipe.RecipeCategory;
import com.dietscheduler.backend.recipe.RecipeIngredient;
import com.dietscheduler.backend.recipe.RecipeIngredientRepository;
import com.dietscheduler.backend.recipe.RecipeRepository;
import com.dietscheduler.backend.recipe.RecipeTag;
import com.dietscheduler.backend.recipe.RecipeTagRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Idempotent dev-data seed: common allergies/tastes plus a small starter recipe library. */
@Component
public class DataSeeder implements ApplicationRunner {

    private final AllergyRepository allergyRepository;
    private final TasteRepository tasteRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;

    public DataSeeder(AllergyRepository allergyRepository, TasteRepository tasteRepository,
                       IngredientRepository ingredientRepository, RecipeRepository recipeRepository,
                       RecipeIngredientRepository recipeIngredientRepository, RecipeTagRepository recipeTagRepository) {
        this.allergyRepository = allergyRepository;
        this.tasteRepository = tasteRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeTagRepository = recipeTagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAllergies();
        seedTastes();
        seedRecipes();
    }

    private void seedAllergies() {
        if (allergyRepository.count() > 0) {
            return;
        }
        List<String> names = List.of("Peanuts", "Tree Nuts", "Milk", "Eggs", "Wheat", "Soy", "Fish", "Shellfish", "Sesame", "Gluten");
        for (String name : names) {
            allergyRepository.save(Allergy.builder().name(name).category("Food").build());
        }
    }

    private void seedTastes() {
        if (tasteRepository.count() > 0) {
            return;
        }
        seedTasteType(TasteType.DIET, "Vegetarian", "Vegan", "Pescatarian", "Keto", "Low-Carb", "Pork", "Beef", "Alcohol");
        seedTasteType(TasteType.FLAVOR, "Spicy", "Sweet", "Sour", "Savory", "Cilantro");
        seedTasteType(TasteType.CUISINE, "Italian", "Mexican", "Indian", "Chinese", "Japanese", "Mediterranean", "American");
    }

    private void seedTasteType(TasteType type, String... names) {
        for (String name : names) {
            tasteRepository.save(Taste.builder().type(type).name(name).build());
        }
    }

    private void seedRecipes() {
        if (recipeRepository.count() > 0) {
            return;
        }

        record Ing(String name, String quantity, String unit) {
        }
        record Tag(TasteType type, String value) {
        }
        record RecipeDef(String name, RecipeCategory category, String instructions, int servings, int prepMin,
                          int cookMin, double calories, double protein, double carbs, double fat,
                          List<Ing> ingredients, List<Tag> tags) {
        }

        List<RecipeDef> defs = List.of(
                new RecipeDef("Spaghetti Aglio e Olio", RecipeCategory.MAIN,
                        "Cook spaghetti. Saute sliced garlic in olive oil with chili flakes until golden. Toss with pasta and parsley.",
                        2, 10, 15, 480, 12, 65, 18,
                        List.of(new Ing("Spaghetti", "200", "g"), new Ing("Garlic", "4", "clove"),
                                new Ing("Olive Oil", "60", "ml"), new Ing("Chili Flakes", "1", "tsp"), new Ing("Parsley", "10", "g")),
                        List.of(new Tag(TasteType.CUISINE, "Italian"), new Tag(TasteType.DIET, "Vegetarian"))),
                new RecipeDef("Margherita Pizza", RecipeCategory.MAIN,
                        "Spread tomato sauce on dough, top with mozzarella, bake at 250C until golden, finish with basil.",
                        2, 20, 12, 650, 24, 80, 22,
                        List.of(new Ing("Pizza Dough", "300", "g"), new Ing("Tomato Sauce", "150", "g"),
                                new Ing("Mozzarella", "200", "g"), new Ing("Basil", "10", "g")),
                        List.of(new Tag(TasteType.CUISINE, "Italian"), new Tag(TasteType.DIET, "Vegetarian"))),
                new RecipeDef("Chicken Tikka Masala", RecipeCategory.MAIN,
                        "Marinate chicken in yogurt and spices, grill, simmer in tomato cream sauce with garam masala.",
                        4, 30, 25, 520, 38, 20, 30,
                        List.of(new Ing("Chicken Breast", "600", "g"), new Ing("Yogurt", "150", "g"),
                                new Ing("Tomato", "400", "g"), new Ing("Cream", "100", "ml"), new Ing("Garam Masala", "2", "tbsp")),
                        List.of(new Tag(TasteType.CUISINE, "Indian"), new Tag(TasteType.FLAVOR, "Spicy"))),
                new RecipeDef("Beef Tacos", RecipeCategory.MAIN,
                        "Brown ground beef with taco seasoning, fill shells with beef, lettuce, cheese and salsa.",
                        4, 15, 15, 430, 26, 30, 22,
                        List.of(new Ing("Ground Beef", "500", "g"), new Ing("Taco Shells", "8", "unit"),
                                new Ing("Lettuce", "100", "g"), new Ing("Cheddar Cheese", "100", "g"), new Ing("Salsa", "100", "g")),
                        List.of(new Tag(TasteType.CUISINE, "Mexican"), new Tag(TasteType.DIET, "Beef"))),
                new RecipeDef("Vegetable Stir Fry", RecipeCategory.MAIN,
                        "Stir fry broccoli, carrot and bell pepper in hot oil, add ginger and soy sauce, cook until tender-crisp.",
                        2, 15, 10, 240, 8, 28, 9,
                        List.of(new Ing("Broccoli", "200", "g"), new Ing("Carrot", "100", "g"),
                                new Ing("Bell Pepper", "100", "g"), new Ing("Soy Sauce", "30", "ml"), new Ing("Ginger", "10", "g")),
                        List.of(new Tag(TasteType.CUISINE, "Chinese"), new Tag(TasteType.DIET, "Vegetarian"), new Tag(TasteType.DIET, "Vegan"))),
                new RecipeDef("Greek Salad", RecipeCategory.ENTREE,
                        "Chop cucumber and tomato, combine with feta, olives, drizzle with olive oil.",
                        2, 10, 0, 280, 8, 12, 22,
                        List.of(new Ing("Cucumber", "150", "g"), new Ing("Tomato", "150", "g"),
                                new Ing("Feta Cheese", "100", "g"), new Ing("Olives", "50", "g"), new Ing("Olive Oil", "30", "ml")),
                        List.of(new Tag(TasteType.CUISINE, "Mediterranean"), new Tag(TasteType.DIET, "Vegetarian"))),
                new RecipeDef("Peanut Butter Cookies", RecipeCategory.DESSERT,
                        "Cream peanut butter with sugar, mix in egg and flour, bake at 180C for 10 minutes.",
                        12, 15, 10, 150, 4, 15, 9,
                        List.of(new Ing("Peanut Butter", "250", "g"), new Ing("Sugar", "150", "g"),
                                new Ing("Egg", "1", "unit"), new Ing("Flour", "100", "g")),
                        List.of(new Tag(TasteType.FLAVOR, "Sweet"))),
                new RecipeDef("Shrimp Scampi", RecipeCategory.MAIN,
                        "Saute shrimp with garlic and butter, deglaze with white wine, toss with cooked linguine.",
                        2, 10, 12, 460, 30, 45, 16,
                        List.of(new Ing("Shrimp", "300", "g"), new Ing("Garlic", "3", "clove"),
                                new Ing("Butter", "40", "g"), new Ing("White Wine", "80", "ml"), new Ing("Linguine", "200", "g")),
                        List.of(new Tag(TasteType.CUISINE, "Italian"))),
                new RecipeDef("Vegan Buddha Bowl", RecipeCategory.ENTREE,
                        "Combine cooked quinoa, chickpeas, avocado and spinach, drizzle with tahini dressing.",
                        2, 15, 15, 420, 14, 55, 16,
                        List.of(new Ing("Quinoa", "150", "g"), new Ing("Chickpeas", "150", "g"),
                                new Ing("Avocado", "1", "unit"), new Ing("Spinach", "50", "g"), new Ing("Tahini", "30", "g")),
                        List.of(new Tag(TasteType.DIET, "Vegan"), new Tag(TasteType.DIET, "Vegetarian"))),
                new RecipeDef("Miso Soup", RecipeCategory.SNACK,
                        "Dissolve miso paste in hot dashi, add tofu cubes, seaweed and sliced green onion.",
                        2, 5, 10, 90, 6, 8, 3,
                        List.of(new Ing("Miso Paste", "40", "g"), new Ing("Tofu", "100", "g"),
                                new Ing("Seaweed", "5", "g"), new Ing("Green Onion", "10", "g")),
                        List.of(new Tag(TasteType.CUISINE, "Japanese"), new Tag(TasteType.DIET, "Vegetarian"))),
                new RecipeDef("Classic Pancakes", RecipeCategory.SNACK,
                        "Whisk flour, milk, egg and sugar into a batter, cook on a buttered pan until golden on both sides.",
                        4, 10, 15, 320, 9, 45, 11,
                        List.of(new Ing("Flour", "200", "g"), new Ing("Milk", "300", "ml"),
                                new Ing("Egg", "2", "unit"), new Ing("Sugar", "30", "g"), new Ing("Butter", "20", "g")),
                        List.of(new Tag(TasteType.FLAVOR, "Sweet"), new Tag(TasteType.CUISINE, "American"))),
                new RecipeDef("Guacamole", RecipeCategory.SNACK,
                        "Mash avocado, mix in lime juice, diced onion, chopped cilantro and salt.",
                        4, 10, 0, 160, 2, 9, 14,
                        List.of(new Ing("Avocado", "3", "unit"), new Ing("Lime", "1", "unit"),
                                new Ing("Onion", "50", "g"), new Ing("Cilantro", "10", "g"), new Ing("Salt", "2", "g")),
                        List.of(new Tag(TasteType.CUISINE, "Mexican"), new Tag(TasteType.DIET, "Vegan"), new Tag(TasteType.DIET, "Vegetarian")))
        );

        Map<String, Ingredient> ingredientsByName = new LinkedHashMap<>();

        for (RecipeDef def : defs) {
            Recipe recipe = recipeRepository.save(Recipe.builder()
                    .name(def.name())
                    .category(def.category())
                    .instructions(def.instructions())
                    .servings(def.servings())
                    .prepTimeMinutes(def.prepMin())
                    .cookTimeMinutes(def.cookMin())
                    .caloriesPerServing(def.calories())
                    .proteinPerServing(def.protein())
                    .carbsPerServing(def.carbs())
                    .fatPerServing(def.fat())
                    .createdByUserId(null)
                    .isPrivate(false)
                    .build());

            for (Ing ing : def.ingredients()) {
                Ingredient ingredient = ingredientsByName.computeIfAbsent(ing.name(), name ->
                        ingredientRepository.findByNameIgnoreCaseAndHouseholdIdIsNull(name)
                                .orElseGet(() -> ingredientRepository.save(Ingredient.builder().name(name).build())));
                recipeIngredientRepository.save(RecipeIngredient.builder()
                        .recipeId(recipe.getId())
                        .ingredientId(ingredient.getId())
                        .quantity(new BigDecimal(ing.quantity()))
                        .unit(ing.unit())
                        .build());
            }

            for (Tag tag : def.tags()) {
                recipeTagRepository.save(RecipeTag.builder()
                        .recipeId(recipe.getId())
                        .tagType(tag.type())
                        .value(tag.value())
                        .build());
            }
        }
    }
}
