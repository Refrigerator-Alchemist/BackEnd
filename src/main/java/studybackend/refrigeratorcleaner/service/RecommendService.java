package studybackend.refrigeratorcleaner.service;

import studybackend.refrigeratorcleaner.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class RecommendService {

    @Value("${openai.model.chat}")
    private String chatModel;

    @Value("${openai.model.image}")
    private String imageModel;

    @Value("${openai.api.url.chat}")
    private String chatApiURL;

    @Value("${openai.api.url.image}")
    private String imageApiURL;

    @Autowired
    private RestTemplate template;

    //명령문을 전달받아 gpt 실행시키고 gpt 대답 리턴.
    public String chat(String prompt){
        ChatRequest request = new ChatRequest(chatModel, prompt);
        ChatResponse response = template.postForObject(chatApiURL, request, ChatResponse.class);

        return response.getChoices().get(0).getMessage().getContent();
    }
    
    //prompt에 맞는 이미지 생성
    public String image(String prompt){
        ImageRequest request = ImageRequest.builder()
                .model(imageModel)
                .prompt(prompt)
                .build();

        ImageResponse response = template.postForObject(imageApiURL, request, ImageResponse.class);

        return response.getData().get(0).getUrl();
    }

    public RecommendDto recommend(List<String> ingredients){

        //재료 문자열 처리
        StringBuilder sb = new StringBuilder();

        for(String str : ingredients){
            sb.append(str);

            //마지막 재료는 콤마 생략
            if(str != ingredients.get(ingredients.size()-1))
                sb.append(", ");
        }
        String strIngredient = sb.toString();

        //음식 이름&재료 얻기
        String prompt = "재료: " + strIngredient + "\n이 재료들을 포함해서 만들 수 있는 요리 이름을 딱 한 가지 말하고 콜론(:)을 붙인 다음 그 요리에 필요한 재료들을 콤마(,)로 구분해서 알려줘. 모든 재료를 사용하지 않아도 괜찮아.\n"
                + " 예를 들어서 만들 수 있는 요리의 이름이 볶음밥이고 볶음밥에 필요한 재료들이 파, 밥, 마늘, 양파, 당근, 양배추면 \"볶음밥: 파, 밥, 마늘, 양파, 당근, 양배추\" 이렇게 출력해줘. 내가 지정한 형식으로만 출력하고 다른 건 아무것도 출력하지마.";
        String[] result = chat(prompt).split(":"); //result[0] : "요리 이름", result[1] : "재료1, 재료2, 재료3, ... "
        String foodName = result[0];
        List<String> foodIngredients = new ArrayList<>();
        for(String ingredient : result[1].split(",")){
            foodIngredients.add(ingredient.trim());
        }

        //레시피 얻기
        String recipePrompt = "자 이제 " + foodName + " 레시피를 알려줘. 가지고 있는 재료는 " + result[1] + "이야. 레시피를 알려줄 때는 최대한 자세하게 알려주고 만드는 순서에 따라 문장 앞에 번호를 붙여 출력해줘. 그리고 마지막에 \"맛있게 드세요!\"로 출력을 마무리 해줘."
                + "예를 들어 \"1.마늘을 다지고 파를 썰어준다 2.계란을 후라이팬에 반쯤 익히고 다진 마늘과 썬 파를 넣는다 3.진간장을 3숟갈 계란 후라이에 넣는다 4.맛있게 드세요!\" 이렇게 해 줘. 레시피를 알려줄 때, 내가 지정한 형식으로만 출력하고 다른 문장은 절대 출력하지마.";
        String[] recipeArr = chat(recipePrompt).split("\n");
        List<String> recipe = Arrays.asList(recipeArr);

        //음식 사진 얻기
        String imgPrompt = "자 이제 " + result[1] + "을 사용해서 만든 " + foodName +"의 사진을 생성해줘.";
        String foodImgURL = image(imgPrompt);

        RecommendDto recommendDto = RecommendDto.builder()
                .recipe(recipe)
                .foodName(foodName)
                .ingredients(foodIngredients)
                .imgUrl(foodImgURL)
                .build();

        return recommendDto;
    }
}
