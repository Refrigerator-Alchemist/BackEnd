package studybackend.refrigeratorcleaner.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import studybackend.refrigeratorcleaner.entity.User;
import studybackend.refrigeratorcleaner.error.CustomException;
import studybackend.refrigeratorcleaner.jwt.service.JwtService;
import studybackend.refrigeratorcleaner.repository.UserRepository;

import java.io.IOException;
import java.util.UUID;

import static studybackend.refrigeratorcleaner.error.ErrorCode.*;

@RequiredArgsConstructor
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

    private static final String LOGIN_CHECK_URL = "/login";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        String requestURI = request.getRequestURI();

        if (requestURI.equals(LOGIN_CHECK_URL) || pathMatcher.match("/auth/**", requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = jwtService.extractAccessToken(request)
                .filter(jwtService::isTokenValid)
                .orElse(null);

        if (accessToken != null) {
            checkAccessTokenAndAuthentication(request, response, filterChain);
            return;
        }

        if (accessToken == null) {
            throw new CustomException(NO_VALID_ACCESSTOKEN);
        }
    }

    public void checkAccessTokenAndAuthentication(HttpServletRequest request, HttpServletResponse response,
                                                  FilterChain filterChain) throws ServletException, IOException {
        jwtService.extractAccessToken(request)
                .filter(jwtService::isTokenValid)
                .ifPresentOrElse(accessToken -> {
                    // Token이 유효할 때
                    jwtService.extractEmail(accessToken)
                            .ifPresentOrElse(email -> {
                                        // Email 추출 성공 시f
                                        userRepository.findByEmail(email)
                                                .ifPresentOrElse(this::saveAuthentication,
                                                        () -> new CustomException(NO_EXIST_USER_EMAIL)
                                                        );
                                    },
                                    () -> new CustomException(NO_EXTRACT_EMAIL)
                            );
                }, () -> new CustomException(NO_EXTRACT_ACCESSTOKEN)
                );

        filterChain.doFilter(request, response);
    }


    public void saveAuthentication(User user) {
        String password = user.getPassword();
        if (password == null) {
            password = UUID.randomUUID().toString().replace("-","").substring(0,8);
        }

        UserDetails userDetailsUser = org.springframework.security.core.userdetails.User.builder()
                .username(user.getSocialId())
                .password(password)
                .roles(user.getRole())
                .build();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetailsUser, null,
                authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
