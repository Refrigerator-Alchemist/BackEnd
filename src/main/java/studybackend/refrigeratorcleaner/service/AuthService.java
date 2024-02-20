package studybackend.refrigeratorcleaner.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import studybackend.refrigeratorcleaner.dto.VerifyEmailRequestDto;
import studybackend.refrigeratorcleaner.dto.VerifyEmailResonseDto;
import studybackend.refrigeratorcleaner.entity.User;
import studybackend.refrigeratorcleaner.oauth.dto.Role;
import studybackend.refrigeratorcleaner.repository.UserRepository;
import studybackend.refrigeratorcleaner.util.EmailUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import static studybackend.refrigeratorcleaner.oauth.dto.SocialType.Refrigerator_Cleaner;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailUtil emailUtil;
    private final AmazonS3Client amazonS3Client;


    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("default.profile")
    private String defaultProfile;

    public VerifyEmailResonseDto sendEmail(String email) throws MessagingException {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        String randomNum = String.valueOf((new Random().nextInt(90000) + 10000));
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(10);

        emailUtil.sendEmail(email, randomNum);

        VerifyEmailResonseDto verifyEmailResonseDto = VerifyEmailResonseDto.
                builder()
                .randomNum(randomNum)
                .createTime(createTime)
                .expireTime(expireTime)
                .build();

        return verifyEmailResonseDto;

    }

    @Transactional
    public void verifyNickName(String nickName) {

        if (userRepository.existsByNickName(nickName)) {
            throw new RuntimeException("이미 존재하는 닉네임입니다.");
        }
        log.info("{}", nickName);
        User user=User.builder()
                .nickName(nickName)
                .role(Role.USER.getKey())
                .build();

        userRepository.save(user);
    }


    public void verifyEmail(VerifyEmailRequestDto verifyEmailRequestDto) {

        if (!verifyEmailRequestDto.getRandomNum().equals(verifyEmailRequestDto.getInputNum())) {
            throw new RuntimeException("인증번호가 틀렸습니다.");
        }
        if (verifyEmailRequestDto.getSendTime().isAfter(verifyEmailRequestDto.getExpireTime())) {
            throw new RuntimeException("인증번호가 만료되었습니다.");
        }
    }

    @Transactional
    public void signup(String email, String pw, MultipartFile multipartFile, String nickName) throws IOException {
        String socialId = UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        String password = passwordEncoder.encode(pw);
        String url;

        url = createProfileUrl(multipartFile);

        User user = userRepository.findByNickName(nickName).get();
        user.updateAll(email, password, socialId,url,Refrigerator_Cleaner.getKey());
        userRepository.save(user);
        userRepository.deleteEverything();
    }

    private String createProfileUrl(MultipartFile multipartFile) throws IOException {
        String url;
        if (multipartFile.isEmpty()) {
            url = defaultProfile;
        } else {
            String fileName = multipartFile.getOriginalFilename();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(multipartFile.getContentType());
            metadata.setContentLength(multipartFile.getSize());
            amazonS3Client.putObject(bucket, fileName, multipartFile.getInputStream(), metadata);
            url = amazonS3Client.getUrl(bucket, fileName).toString();
        }
        return url;
    }


}
