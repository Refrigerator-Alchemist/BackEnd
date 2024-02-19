package studybackend.refrigeratorcleaner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studybackend.refrigeratorcleaner.oauth.dto.Role;


@Getter
@NoArgsConstructor
@Entity
@Builder
@Table(name = "USER")
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String email; // 이메일
    private String password; // 비밀번호
    private String nickName; // 닉네임
    private String imageUrl; // 프로필 이미지

    private String role;

    private String socialType; // KAKAO, NAVER, GOOGLE

    @Column(name = "social_id",unique = true)
    private String socialId; // 로그인한 소셜 타입의 식별자 값

    private String refreshToken; // 리프레시 토큰

    public void authorizeUser() {
        this.role = Role.USER.getKey();
    }

    public void updateNickname(String updateNickname) {
        this.nickName = updateNickname;
    }

    public void updatePassword(String updatePassword) {
        this.password = updatePassword;
    }
    public void updateRefreshToken(String updateRefreshToken) {
        this.refreshToken = updateRefreshToken;
    }


    public void updateAll(String email, String pw, String socialId,String url,String socialType) {
        this.email = email;
        this.password = pw;
        this.socialId = socialId;
        this.imageUrl = url;
        this.socialType = socialType;
    }
}
