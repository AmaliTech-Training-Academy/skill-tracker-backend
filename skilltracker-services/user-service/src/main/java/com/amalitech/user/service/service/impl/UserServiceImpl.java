package com.amalitech.user.service.service.impl;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.enums.roleEnum;
import com.amalitech.user.service.enums.stateEnum;
import com.amalitech.user.service.enums.tierEnum;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.repository.UserRepository;
import com.amalitech.user.service.service.UserService;
import com.amalitech.user.service.util.EmailUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private EmailUtil emailUtil;

    private Integer tempCode;

    @Override
    @Transactional
    public User createUser(User userdto) {

        // Convert DTO -> Entity
        User user = new User();
        user.setEmail(userdto.getEmail());
        user.setPasswordHash(userdto.getPasswordHash()); // update this with encoder when security arrives
        user.setLanguage("en");
        user.setTimezone("UTC");

        User savedUser = repo.save(user);


        // Convert Entity → DTO
        UserResponseDTO userResponse =  new UserResponseDTO(
                savedUser.getId(),
//                savedUser.getUsername(),
                savedUser.getEmail(),
//                savedUser.getRole(),
//                savedUser.getState(),
//                savedUser.getPremiumTier(),
                savedUser.getLanguage(),
                savedUser.getTimezone());
//                savedUser.getLastLoginAt(),
//                savedUser.getUpdatedAt());

        notifyUser(savedUser.getEmail(), generateCode()); // send verification code before returning registered user

        return new User().builder()
                .email(savedUser.getEmail())
                .language(savedUser.getLanguage())
                .timezone(savedUser.getTimezone())
                .build();
    }

    public User verifyCode(String code, String email){
        if(code.equals(tempCode.toString())){
           return repo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found by stated email"));
        }
        return new User();
    }



    public String notifyUser(String toEmail, int code) {
        emailUtil.sendEmail(
                toEmail,
                "Account created successfully!",
                "Your account has been created successfully.\n Here is your 6-digit verification code: \n " +
                        "Past this link in your address bar: " + "https://dcb46541311e.ngrok-free.app/api/v1/auth/verify/"+code+"/"+toEmail);

        return "Email sent successfully";
    }

    public Integer generateCode() {
        SecureRandom random = new SecureRandom();
        tempCode = 100000 + random.nextInt(900000);
        return tempCode; // generates a 6-digit number between 100000–999999
    }
}
