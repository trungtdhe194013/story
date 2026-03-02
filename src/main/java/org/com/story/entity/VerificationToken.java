package org.com.story.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    // OTP 6 số (dùng cho OAuth2 và đổi mật khẩu)
    @Column(length = 6)
    private String otp;

    // Loại: EMAIL_VERIFY | OAUTH2_LOGIN | CHANGE_PASSWORD
    @Column(length = 30)
    private String otpType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    // Constructor dùng cho email verification (link cũ)
    public VerificationToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.otpType = "EMAIL_VERIFY";
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.used = false;
    }

    // Constructor dùng cho OTP 6 số
    public VerificationToken(String token, String otp, String otpType, User user, int expiryMinutes) {
        this.token = token;
        this.otp = otp;
        this.otpType = otpType;
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusMinutes(expiryMinutes);
        this.used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
