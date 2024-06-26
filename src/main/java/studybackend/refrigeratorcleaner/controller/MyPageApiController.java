package studybackend.refrigeratorcleaner.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import studybackend.refrigeratorcleaner.entity.User;
import studybackend.refrigeratorcleaner.service.MyPageService;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class MyPageApiController {  //회원 정보 수정
    private final MyPageService myPageService;
    @GetMapping(value= "/updateRole")
    public  void updateRole(){
        String nickName = "hello";
        List<User> u =  myPageService.getUser(nickName);
        User user = u.get(0);
        user.updateRole("hello");
        myPageService.updateUser(user);
    }
    @GetMapping (value ="/updateNickname")
    public  void updateNickname(){
        String nickName = "hello";
        List<User> u =  myPageService.getUser(nickName);
        User user = u.get(0);
        String updateNick = "hello";
        user.updateNickname(updateNick);
        myPageService.updateUser(user);
    }
    @GetMapping (value = "/updatePassword")
    public void  updatePassword(){
        String nickName = "hello";
        List<User> u =  myPageService.getUser(nickName);
        User user = u.get(0);
        String pass = "1234";
        user.updatePassword(pass);
        myPageService.updateUser(user);
    }

//    @GetMapping (value = "/updateRefreshToken")
//    public  void updateRefreshToken(){
//        String nickName = "hello";
//        List<User> u =  myPageService.getUser(nickName);
//        User user = u.get(0);
//
//        String Token = "edjwnsfkdf";
//        user.updateRefreshToken(Token);
//        myPageService.updateUser(user);
//    }



}
