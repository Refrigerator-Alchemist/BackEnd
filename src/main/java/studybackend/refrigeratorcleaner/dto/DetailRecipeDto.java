package studybackend.refrigeratorcleaner.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetailRecipeDto {

    private String foodName;

    private List<String> ingredients;

    private List<String> recipe;

    private String imgURL;

    @Builder
    public DetailRecipeDto(String foodName, List<String> ingredients, List<String> recipe, String imgURL) {
        this.foodName = foodName;
        this.ingredients = ingredients;
        this.recipe = recipe;
        this.imgURL = imgURL;
    }


}
