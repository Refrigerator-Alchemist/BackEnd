package studybackend.refrigeratorcleaner.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import studybackend.refrigeratorcleaner.dto.Role;
import studybackend.refrigeratorcleaner.dto.request.*;
import studybackend.refrigeratorcleaner.dto.response.VerifyEmailResonse;
import studybackend.refrigeratorcleaner.entity.Token;
import studybackend.refrigeratorcleaner.entity.User;
import studybackend.refrigeratorcleaner.error.CustomException;
import studybackend.refrigeratorcleaner.jwt.dto.response.TokenResponse;
import studybackend.refrigeratorcleaner.jwt.service.JwtService;
import studybackend.refrigeratorcleaner.repository.TokenRepository;
import studybackend.refrigeratorcleaner.repository.UserRepository;
import studybackend.refrigeratorcleaner.util.EmailUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static studybackend.refrigeratorcleaner.error.ErrorCode.*;
import static studybackend.refrigeratorcleaner.oauth.dto.SocialType.Refrigerator_Cleaner;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailUtil emailUtil;
    private final AmazonS3Client amazonS3Client;
    private final JwtService jwtService;


    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${default.profile}")
    private String defaultProfile;

    public VerifyEmailResonse sendEmail(EmailRequest emailRequest) throws MessagingException {

        validateEmail(emailRequest);

        String randomNum = String.valueOf((new Random().nextInt(9000) + 1000));
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(10);

        emailUtil.sendEmail(emailRequest.getEmail(), randomNum);

        VerifyEmailResonse verifyEmailResonse = VerifyEmailResonse.
                builder()
                .randomNum(randomNum)
                .createTime(createTime)
                .expireTime(expireTime)
                .build();

        return verifyEmailResonse;

    }

    public void validateEmail(EmailRequest emailRequest) {
        if (userRepository.existsByEmailAndSocialType(emailRequest.getEmail(),emailRequest.getSocialType()) && emailRequest.getEmailType().equals("sign-up")) {
            throw new CustomException(EXIST_USER_EMAIL_SOCIALTYPE);
        }
        else if (!userRepository.existsByEmailAndSocialType(emailRequest.getEmail(),emailRequest.getSocialType()) && emailRequest.getEmailType().equals("reset-password")) {
            throw new CustomException(NOT_EXIST_USER_EMAIL_SOCIALTYPE);
        }
    }

    @Transactional
    public void verifyNickName(String nickName) {
        userRepository.deleteEverything();
        if (userRepository.existsByNickName(nickName)) {
            throw new CustomException(EXIST_USER_NICKNAME);
        }
        User user=User.builder()
                .nickName(nickName)
                .role(Role.USER.getKey())
                .build();

        userRepository.save(user);
    }


    public void verifyEmail(VerifyEmailRequest verifyEmailRequest) {

        if (userRepository.existsByEmailAndSocialType(verifyEmailRequest.getEmail(), verifyEmailRequest.getSocialType())
                && verifyEmailRequest.getEmailType().equals("sign-up")) {
            throw new CustomException(EXIST_USER_EMAIL_SOCIALTYPE);
        }
        else if (!userRepository.existsByEmailAndSocialType(verifyEmailRequest.getEmail(), verifyEmailRequest.getSocialType())
                && verifyEmailRequest.getEmailType().equals("reset-password")) {
            throw new CustomException(NOT_EXIST_USER_EMAIL);
        }
        if (!verifyEmailRequest.getRandomNum().equals(verifyEmailRequest.getInputNum())) {
            throw new CustomException(WRONG_CERTIFICATION_NUMBER);
        }
        if (verifyEmailRequest.getSendTime().isAfter(verifyEmailRequest.getExpireTime())) {
            throw new CustomException(EXPIRE_CERTIFICATION_NUMBER);
        }
    }

    @Transactional
    public void signup(UserRequest userRequest) {
        String socialId = UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        String password = passwordEncoder.encode(userRequest.getPassword());
        String url;

        url = defaultProfile;

        User user = userRepository.findByNickName(userRequest.getNickName()).orElseThrow(()->new CustomException(EXIST_USER_NICKNAME));

        user.updateAll(userRequest.getEmail(), password, socialId,url,Refrigerator_Cleaner.getKey());
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public String updateProfileUrl(MultipartFile multipartFile,String nickName) throws IOException {

        User user = userRepository.findByNickName(nickName).orElseThrow(() -> new CustomException(NOT_EXIST_USER_NICKNAME));

        String fileName = multipartFile.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(multipartFile.getContentType());
        metadata.setContentLength(multipartFile.getSize());
        amazonS3Client.putObject(bucket, fileName, multipartFile.getInputStream(), metadata);
        String url = amazonS3Client.getUrl(bucket, fileName).toString();

        user.updateProfile(url);
        userRepository.save(user);

        return url;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {

        if (!resetPasswordRequest.getPassword().equals(resetPasswordRequest.getRePassword())) {
            throw new CustomException(WRONG_PASSWORD);
        }

        if (!resetPasswordRequest.getSocialType().equals(Refrigerator_Cleaner.getKey())) {
            throw new CustomException(NOT_REFRIGERATOR_SOCIALTYPE);
        }

        userRepository.findBySocialTypeAndEmail(resetPasswordRequest.getSocialType(),resetPasswordRequest.getEmail())
                .ifPresentOrElse(user -> {
                    user.updatePassword(passwordEncoder.encode(resetPasswordRequest.getPassword()));
                    userRepository.save(user);
                },() -> new CustomException(NOT_EXIST_USER_EMAIL_SOCIALTYPE));
    }

    public TokenResponse validateToken(String refreshToken, String accessTokenSocialId) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new CustomException(NOT_VALID_REFRESHTOKEN);
        }

        if (tokenRepository.existsByRefreshToken(refreshToken)) {
            throw new CustomException(EXIST_REFRESHTOKEN_BLACKLIST);
        }

        String newAccessToken = jwtService.generateAccessToken(accessTokenSocialId);
        String newRefreshToken = jwtService.generateRefreshToken(accessTokenSocialId);

        TokenResponse tokenResponse=TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
        return tokenResponse;
    }

    @Transactional
    public void changeNickName(ValidateNickNameRequest validateNickNameRequest) {
        Optional<User> existNickName = userRepository.findByNickName(validateNickNameRequest.getPresentNickName());
        Optional<User> changeNickName = userRepository.findByNickName(validateNickNameRequest.getChangeNickName());

        if (existNickName.isEmpty()) {
            throw new CustomException(NOT_EXIST_USER_NICKNAME);
        }
        if (changeNickName.isEmpty()) {
            User user = existNickName.get();
            user.updateNickname(validateNickNameRequest.getChangeNickName());
            userRepository.save(user);

            return;
        }

        throw new CustomException(EXIST_USER_NICKNAME);

    }

    public void logout(String refreshToken, String socialId) {

        User user = userRepository.findBySocialId(socialId).orElseThrow(() -> new CustomException(NOT_EXIST_USER_SOCIALID));
        Token token = new Token();

        user.assignToken(token);

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new CustomException(NOT_VALID_REFRESHTOKEN);
        }

        String tokenSocialId = jwtService.extractSocialId(refreshToken).get();

        if (!tokenSocialId.equals(socialId)) {
            throw new CustomException(NOT_EQUAL_EACH_TOKEN_SOCIALID);
        }

        if (tokenRepository.existsByRefreshToken(refreshToken)) {
            throw new CustomException(EXIST_REFRESHTOKEN_BLACKLIST);
        }

        token.updateTokens(refreshToken);

        tokenRepository.saveAndFlush(token);
    }
}
