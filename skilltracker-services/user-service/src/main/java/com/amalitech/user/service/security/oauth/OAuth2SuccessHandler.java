package com.amalitech.user.service.security.oauth;

import com.amalitech.user.service.model.User;
import com.amalitech.user.service.model.UserProfile;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final String refreshPrefix;
    private final long refreshExpiration;
    private final CookieUtil cookieUtil;
    @Value("${app.frontend-url}")
    private String frontendUrl;

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
        this.cookieUtil = cookieUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        User user = findOrCreateUser(registrationId, attributes);
        handleTokensAndRedirect(response, user);
    }


    private User findOrCreateUser(String registrationId, Map<String, Object> attributes) {
        String email = resolveEmail(registrationId, attributes);
        String name = resolveName(registrationId, attributes);

        return userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name));
    }

    private String resolveEmail(String registrationId, Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        if ("github".equalsIgnoreCase(registrationId) && (email == null || email.isBlank())) {
            String login = (String) attributes.get("login");
            email = login + "@github.local";
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OAuth2 provider did not return an email");
        }
        return email;
    }

    private String resolveName(String registrationId, Map<String, Object> attributes) {
        String name = (String) attributes.get("name");
        if ("github".equalsIgnoreCase(registrationId) && (name == null || name.isBlank())) {
            name = (String) attributes.get("login");
        }
        return name;
    }

    private User createNewUser(String email, String name) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(name != null ? name : email.split("@")[0]);
        newUser.setRole(Role.USER);
        newUser.setState(UserState.REGISTERED);

        UserProfile profile = new UserProfile();
        profile.setEmailNotifications(true);
        profile.setPushNotifications(true);
        newUser.setProfile(profile);

        return userRepository.save(newUser);
    }

    private void handleTokensAndRedirect(HttpServletResponse response, User user) throws IOException {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole(), user.getId());
        String refreshToken = UUID.randomUUID().toString();

        redisUtil.set(refreshPrefix + refreshToken, user.getEmail(), refreshExpiration / 1000);

        cookieUtil.setSecureCookie(response, "accessToken", accessToken,
                jwtUtil.getExpirationSeconds(accessToken));
        cookieUtil.setSecureCookie(response, "refreshToken", refreshToken, refreshExpiration / 1000);

        response.sendRedirect(frontendUrl + "/dashboard");
    }
}
