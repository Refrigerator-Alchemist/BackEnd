package studybackend.refrigeratorcleaner.service.auth.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import studybackend.refrigeratorcleaner.error.CustomException;
import studybackend.refrigeratorcleaner.error.ErrorCode;
import studybackend.refrigeratorcleaner.jwt.dto.response.TokenResponse;
import studybackend.refrigeratorcleaner.jwt.service.JwtService;
import studybackend.refrigeratorcleaner.redis.entity.RefreshToken;
import studybackend.refrigeratorcleaner.redis.repository.BlackListRepository;
import studybackend.refrigeratorcleaner.redis.repository.RefreshTokenRepository;
import studybackend.refrigeratorcleaner.repository.UserRepository;
import studybackend.refrigeratorcleaner.service.AuthService;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokenUpdateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;
    @Mock
    private BlackListRepository blackListRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그인된 socialId가 존재하지 않으면 로그아웃 못함")
    void canNotLogoutWhenDoesNotExistSocialId() {
        when(userRepository.existsBySocialId("hello")).thenReturn(false);

        String token = jwtService.generateAccessToken("hello");
        assertThatCode(() -> authService.logout(token, "hello"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NOT_EXIST_USER_SOCIALID.getMessage());
    }

    @Test
    @DisplayName("Access토큰이 blacklist에 저장되있으면 로그아웃 못함")
    void canNotLogoutWhenAccessTokenExistsInBlackList() {
        String token = jwtService.generateAccessToken("hello");
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(userRepository.existsBySocialId("hello")).thenReturn(true);
        when(blackListRepository.existsByAccessToken(token)).thenReturn(true);
        when(jwtService.extractSocialId(token).get()).thenReturn("hello");
        assertThatCode(() -> authService.logout(token, "hello"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.EXIST_ACCESSTOKEN_BLACKLIST.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 할 때 Access토큰이 blacklist에 저장되있으면 재발급 못함")
    void canNotReIssueTokenWhenAccessTokenExistsInBlackList() {
        String accessToken = jwtService.generateAccessToken(Mockito.anyString());
        String refreshToken = jwtService.generateRefreshToken(Mockito.anyString());
        when(jwtService.isTokenValid(accessToken)).thenReturn(true);
        when(blackListRepository.existsByAccessToken(accessToken)).thenReturn(true);
        assertThatCode(() -> authService.validateToken(accessToken,refreshToken, Mockito.anyString()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.EXIST_ACCESSTOKEN_BLACKLIST.getMessage());

    }

}