package studybackend.refrigeratorcleaner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studybackend.refrigeratorcleaner.dto.Role;


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

    @Column(unique = true)
    private String socialId; // 로그인한 소셜 타입의 식별자 값

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Token token;


    public void authorizeUser() {
        this.role = Role.USER.getKey();
    }

    public void updateNickname(String updateNickname) { this.nickName = updateNickname; }
    public void updateProfile(String updateProfile) { this.imageUrl = updateProfile; }

    public void updatePassword(String updatePassword) {
        this.password = updatePassword;
    }

    public void updateRole(String role) {this.role = role;}

    public void updateAll(String email, String pw, String socialId,String url,String socialType) {
        this.email = email;
        this.password = pw;
        this.socialId = socialId;
        this.imageUrl = url;
        this.socialType = socialType;
    }

    public void assignToken(Token token) {
        this.token = token;
        token.setUser(this); // Token 클래스에도 setUser 메서드가 필요합니다.
    }

    public void setToken(Token token) {
        this.token = token;
    }
}
