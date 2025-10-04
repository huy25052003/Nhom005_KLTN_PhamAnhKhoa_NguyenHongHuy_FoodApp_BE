package org.example.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PayReturnController {
    @Value("${app.frontend-url}" )
    private String frontendUrl;

    @GetMapping("/pay/result")
    public String payResultRedirect(HttpServletRequest req) {
        String query = req.getQueryString();
        return "redirect:" + frontendUrl + "/pay/result" + (query != null ? "?" + query : "");
    }

    @GetMapping("/pay/cancel")
    public String payCancelRedirect(HttpServletRequest req) {
        String query = req.getQueryString();
        return "redirect:" + frontendUrl + "/pay/cancel" + (query != null ? "?" + query : "");
    }


}

