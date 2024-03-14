package studybackend.refrigeratorcleaner.oauth.handler;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import studybackend.refrigeratorcleaner.dto.Role;
import studybackend.refrigeratorcleaner.entity.User;
import studybackend.refrigeratorcleaner.error.CustomException;
import studybackend.refrigeratorcleaner.jwt.service.JwtService;
import studybackend.refrigeratorcleaner.oauth.dto.CustomOAuth2User;
import studybackend.refrigeratorcleaner.repository.TokenRepository;
import studybackend.refrigeratorcleaner.repository.UserRepository;
import studybackend.refrigeratorcleaner.service.AuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static studybackend.refrigeratorcleaner.error.ErrorCode.NOT_EXIST_USER_SOCIALID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공!");
        try {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

            if(oAuth2User.getRole() == Role.GUEST.getKey()) {
                User user = userRepository.findBySocialId(oAuth2User.getSocialId()).orElseThrow(() -> new CustomException(NOT_EXIST_USER_SOCIALID));
                String accessToken = jwtService.generateAccessToken(oAuth2User.getSocialId());
                String refreshToken = jwtService.generateRefreshToken(oAuth2User.getSocialId());

                String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/main")
                        .queryParam("email",oAuth2User.getEmail())
                        .queryParam("socialType",oAuth2User.getSocialType())
                        .queryParam("url",user.getImageUrl())
                        .queryParam("nickName",user.getNickName())
                        .queryParam("accessToken", accessToken)
                        .build()
                        .encode(StandardCharsets.UTF_8)
                        .toUriString();

                response.setHeader("Authorization-Access", accessToken);
                response.addCookie(jwtService.createCookie("Authorization-Refresh", refreshToken));
                response.setStatus(HttpStatus.OK.value());
                response.sendRedirect(targetUrl);


            } else {
                loginSuccess(response, oAuth2User); // 로그인에 성공한 경우 access, refresh 토큰 생성
            }
        } catch (Exception e) {
            throw e;
        }

    }

    private void loginSuccess(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {

        String accessToken = jwtService.generateAccessToken(oAuth2User.getSocialId());
        String refreshToken = jwtService.generateRefreshToken(oAuth2User.getSocialId());

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/main")
                .queryParam("email",oAuth2User.getEmail())
                .queryParam("socialType",oAuth2User.getSocialType())
                .queryParam("socialId",oAuth2User.getSocialId())
                .queryParam("accessToken", accessToken)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        response.setHeader("Authorization-Access", accessToken);
        response.addCookie(jwtService.createCookie("Authorization-Refresh", refreshToken));
        response.setStatus(HttpStatus.OK.value());
        response.sendRedirect(targetUrl);


    }


}
