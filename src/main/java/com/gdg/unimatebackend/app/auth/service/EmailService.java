package com.gdg.unimatebackend.app.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 발신자 이메일 주소를 설정 파일에서 주입받습니다.
     * 일부 SMTP 서버에서는 발신자 주소가 없을 경우 오류가 발생할 수 있어 명시적으로 설정합니다.
     */
    @Value("${spring.mail.username}")
    private String from;

    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public void sendVerificationEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(from); // 발신자 주소 미설정 시 "can't determine local email address" 오류 발생
        message.setSubject("[Travodo] 이메일 인증 코드");
        message.setText(
                "안녕하세요.\n\n" +
                        "Travodo 이메일 인증 코드입니다.\n\n" +
                        "인증 코드: " + code + "\n\n" +
                        "이 코드는 10분간 유효합니다.\n\n" +
                        "본인이 요청한 것이 아니라면 이 메일을 무시하세요."
        );

        mailSender.send(message);
    }
}
