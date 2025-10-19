package com.amalitech.user.service.security;

import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.util.JwtUtil;
import com.amalitech.user.service.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final String refreshPrefix;
    private final long refreshExpiration;
    private final ObjectMapper objectMapper;

    public OAuth2SuccessHandler(RedisUtil redisUtil,
                                JwtUtil jwtUtil,
                                UserRepository userRepository,
                                @Value("${app.refresh-token-prefix}") String refreshPrefix,
                                @Value("${jwt.refresh-expiration-ms}") long refreshExpiration) {
        this.redisUtil = redisUtil;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshPrefix = refreshPrefix;
        this.refreshExpiration = refreshExpiration;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(name != null ? name : email.split("@")[0]);
                    newUser.setRole(Role.USER);
                    newUser.setState(UserState.REGISTERED);
                    return userRepository.save(newUser);
                });


        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String refreshToken = UUID.randomUUID().toString();
        redisUtil.set(refreshPrefix + refreshToken, user.getEmail(), refreshExpiration / 1000);

        AuthResponse tokens = new AuthResponse(accessToken, refreshToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(tokens));
        response.getWriter().flush();
    }
}
