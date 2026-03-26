package org.com.story.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.com.story.service.MailService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Hidden
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/test/mail")
@RequiredArgsConstructor
public class MailTestController {

    private final MailService mailService;

    @GetMapping
    public String testMail() {
        mailService.sendTextMail(
                "huyenthoaiphu@gmail.com",
                "Test mail từ Story Platform",
                "Mail gửi thành công từ huyenthoaiphu@gmail.com ✅"
        );
        return "OK";
    }
}