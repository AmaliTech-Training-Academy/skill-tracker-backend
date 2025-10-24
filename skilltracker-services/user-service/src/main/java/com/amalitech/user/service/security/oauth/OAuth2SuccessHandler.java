package com.amalitech.user.service.security.oauth;

import com.amalitech.common.security.dto.response.ApiError;
import com.amalitech.user.service.dto.response.AuthResponse;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.enums.Role;
import com.amalitech.user.service.model.enums.UserState;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.security.util.JwtUtil;
import com.amalitech.user.service.util.CookieUtil;
import com.amalitech.user.service.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.amalitech.common.security.dto.response.ApiResponse;
import java.io.IOException;
import java.util.List;
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
    private final CookieUtil cookieUtil;

    public OAuth2SuccessHandler(RedisUtil redisUtil,
                                JwtUtil jwtUtil,
                                UserRepository userRepository,
                                @Value("${app.refresh-token-prefix}") String refreshPrefix,
                                @Value("${jwt.refresh-expiration-ms}") long refreshExpiration,
                                CookieUtil cookieUtil) {
        this.redisUtil = redisUtil;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshPrefix = refreshPrefix;
        this.refreshExpiration = refreshExpiration;
        this.objectMapper = new ObjectMapper();
        this.cookieUtil = cookieUtil;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        ApiError apiError = ApiError.of(
                400,
                "OAuth2 provider did not return an email address",
                "The OAuth2 login flow requires an email address, but none was provided by the provider.", // detail
                "/auth/oauth2/callback",
                List.of(),
                ""
        );

        if (email == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(
                    apiError
            ));
            response.getWriter().flush();
            return;
        }

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

        cookieUtil.setSecureCookie(response, "accessToken", accessToken,
                jwtUtil.getExpirationSeconds(accessToken));
        cookieUtil.setSecureCookie(response, "refreshToken", refreshToken, refreshExpiration / 1000);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");

        AuthResponse authResponse = new AuthResponse("OAuth2 login successful");

        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(
                "OAuth2 login successful", authResponse, null
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.getWriter().flush();
    }
}
