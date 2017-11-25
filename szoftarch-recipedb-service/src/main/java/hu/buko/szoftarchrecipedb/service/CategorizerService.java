package hu.buko.szoftarchrecipedb.service;

import hu.buko.szoftarchrecipedb.model.Category;
import hu.buko.szoftarchrecipedb.model.Ingredient;
import hu.buko.szoftarchrecipedb.model.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CategorizerService {

    private RecipeService recipeService;

    private String[] meatSearchTerms  = new String[]{
            "csirke", "pulyka", "marha", "liba", "disznó" ,"hús"
    };

    private String[] pastaSearchTerms = new String[]{
            "tészta", "spagetti", "lasagne", "cannelloni"
    };

    private String[] pastaStarterSearchTerms  = new String[]{
            "liszt", "tojás", "élesztő"
    };

    private String[] soupSearchTerms = new String[]{
            "víz", "sósav"
    };

    private String[] sweetnessSearchTerms = new String[]{
            "cukor", "édesítő"
    };

    @Autowired
    public CategorizerService(RecipeService recipeService){
        this.recipeService = recipeService;
    }


    private boolean isPasta(Recipe recipe){
        boolean containsFlour = false;
        boolean containsEggs = false;
        boolean containsYeast = false;

        boolean containsPasta = false;
        Stream<String> stream = Arrays.stream(pastaSearchTerms);

        for(Ingredient ingredient : recipe.getIngredients()){
            String ingredientName = ingredient.getName().toLowerCase();
            if(containsPasta)
                break;

            if(stream.anyMatch(ingredientName::contains)){
                containsPasta = true;
            }
            else if(ingredientName.contains(pastaStarterSearchTerms[0])){
                containsFlour = true;
            }
            else if(ingredientName.contains(pastaStarterSearchTerms[1])){
                containsEggs = true;
            }
            else if(ingredientName.contains(pastaStarterSearchTerms[2])){
                containsYeast = true;
            }

            if(containsFlour && containsEggs && containsYeast){
                containsPasta = true;
            }
        }
        stream.close();

        return containsPasta;
    }

    private boolean isMeatDish(Recipe recipe){
        boolean containsMeat = false;
        Stream<String> stream = Arrays.stream(meatSearchTerms);

        for(Ingredient ingredient : recipe.getIngredients()){
            String ingredientName = ingredient.getName().toLowerCase();

            if(containsMeat)
                break;

            if(stream.anyMatch(ingredientName::contains)){
                containsMeat = true;
            }
        }

        stream.close();

        return containsMeat;
    }

    private boolean isSweetness(Recipe recipe){
        boolean isSweetness = false;
        boolean containsSweetness = false;
        boolean containsMeat = isMeatDish(recipe);
        Stream<String> stream = Arrays.stream(sweetnessSearchTerms);

        for(Ingredient ingredient : recipe.getIngredients()){
            String ingredientName = ingredient.getName().toLowerCase();
            if(isSweetness)
                break;

            if(stream.anyMatch(ingredientName::contains)){
                containsSweetness = true;
            }

            if(containsSweetness && !containsMeat){
                isSweetness = true;
            }
        }
        stream.close();

        return isSweetness;
    }

    private boolean isVegetarian(Recipe recipe){
        return !isMeatDish(recipe);
    }

    private boolean isSoup(Recipe recipe){
        boolean isSoup = false;
        boolean containsLiquid = false;
        boolean isSweetness = isSweetness(recipe);
        Stream<String> stream = Arrays.stream(soupSearchTerms);

        for(Ingredient ingredient : recipe.getIngredients()){
            String ingredientName = ingredient.getName().toLowerCase();
            if(containsLiquid)
                break;

            if(stream.anyMatch(ingredientName::contains)){
                containsLiquid = true;
            }

            if(containsLiquid && !isSweetness){
                isSoup = true;
            }
        }
        stream.close();

        return isSoup;
    }

    @Async
    public void categorize(Recipe recipe) {
        List<Category> categories = new ArrayList<>();

        if(isMeatDish(recipe)){
            categories.add(Category.MEATDISH);
        }
        if(isPasta(recipe)){
            categories.add(Category.PASTA);
        }
        if(isSoup(recipe)){
            categories.add(Category.SOUP);
        }
        if(isSweetness(recipe)){
            categories.add(Category.SWEETNESS);
        }
        if(isVegetarian(recipe)){
            categories.add(Category.VEGA);
        }
        if(categories.isEmpty()){
            categories.add(Category.UNKNOWN);
        }

        recipe.setCategories(categories);
        recipeService.updateRecipe(recipe);
    }
}
