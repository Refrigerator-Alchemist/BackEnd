package studybackend.refrigeratorcleaner.login.handler;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.transaction.annotation.Transactional;
import studybackend.refrigeratorcleaner.entity.User;
import studybackend.refrigeratorcleaner.error.CustomException;
import studybackend.refrigeratorcleaner.jwt.service.JwtService;
import studybackend.refrigeratorcleaner.repository.TokenRepository;
import studybackend.refrigeratorcleaner.repository.UserRepository;
import studybackend.refrigeratorcleaner.service.AuthService;

import static studybackend.refrigeratorcleaner.error.ErrorCode.NOT_EXIST_USER_SOCIALID;


@Slf4j
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;


    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String socialId = extractUsername(authentication);

        User userInfo = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new CustomException(NOT_EXIST_USER_SOCIALID));
        String email = userInfo.getEmail();

        String accessToken = jwtService.generateAccessToken(socialId);
        String refreshToken = jwtService.generateRefreshToken(socialId);


        log.info("로그인에 성공하였습니다. 이메일 : {}", email);
        log.info("로그인에 성공하였습니다. AccessToken : {}", accessToken);

        response.setHeader("Authorization-Access", accessToken);
        response.setHeader("url", userInfo.getImageUrl());
        response.addCookie(jwtService.createCookie("Authorization-Refresh", refreshToken));
        response.setStatus(HttpStatus.OK.value());

    }


    private String extractUsername(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

}
