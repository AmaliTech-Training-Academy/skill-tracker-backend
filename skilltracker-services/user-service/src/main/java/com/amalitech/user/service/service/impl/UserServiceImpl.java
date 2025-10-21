package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.mapper.UserMapper;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.service.UserService;
import com.amalitech.user.service.util.PasswordEncoderUtil;
import com.amalitech.user.service.util.EmailUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final EmailUtil emailUtil;

    private Integer tempCode;

    public UserServiceImpl(UserRepository repo, EmailUtil emailUtil) {
        this.repo = repo;
        this.emailUtil = emailUtil;
        tempCode = 0;
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userdto) throws IllegalStateException {
        if (repo.existsByEmail(userdto.email())) {
            throw new IllegalStateException("A user already exists with this email.");
        }
        User user = UserMapper.toEntity(userdto);
        User savedUser = repo.save(user);
        notifyUser(
                System.getenv("APP_BASE_EMAIL"),
                savedUser.getEmail(),
                "Account created successfully!",
                "Enter this verification code to verify your identity: " + generateCode());
        return UserMapper.toDto(savedUser);
    }

    @Override
    public Optional<UserResponseDTO> verifyCode(String code, String email) {
        if(code.equals(tempCode.toString())){
            return repo.findByEmail(email)
                    .map(UserMapper::toDto);
        }
        return Optional.empty();
    }

    public void notifyUser(String toEmail, String subject, String message, String sender) {
        emailUtil.sendEmail(
                sender,
                toEmail,
                subject,
                message
        );
    }

    public Integer generateCode() {
        SecureRandom random = new SecureRandom();
        tempCode = 100000 + random.nextInt(900000);
        return tempCode;
    }

    @Override
    public void updatePassword(User user, String newPassword) {
        user.setPasswordHash(PasswordEncoderUtil.encodePassword(newPassword));
        repo.save(user);
    }
}