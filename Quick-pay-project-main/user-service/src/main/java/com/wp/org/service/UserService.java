package com.wp.org.service;



import com.wp.org.UserCreatedPayload;
import com.wp.org.dto.CustomUserDetails;
import com.wp.org.dto.UserDto;
import com.wp.org.entity.User;
import com.wp.org.repo.UserRepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class UserService implements UserDetailsService {

    private static Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private KafkaTemplate<String,Object> kafkaTemplate;

    @Value("${user.created.topic}")
    private String userCreatedTopic;
    
    @Lazy
    @Autowired
    PasswordEncoder passwordEncoder;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email);
        if(user==null)
        	throw new UsernameNotFoundException("User not found with email: " + email);
        return new CustomUserDetails(user.getEmail(), user.getPassword());
    }

    public Long createUser(UserDto userDto) throws ExecutionException, InterruptedException {
        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setKycNumber(userDto.getKycNumber());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user = userRepo.save(user);
        UserCreatedPayload userCreatedPayload = new UserCreatedPayload();
        userCreatedPayload.setUserEmail(user.getEmail());
        userCreatedPayload.setUserId(user.getId());
        userCreatedPayload.setUserName(user.getName());
        userCreatedPayload.setRequestId(MDC.get("requestId"));
        Future<SendResult<String,Object>> future  = kafkaTemplate.send(userCreatedTopic, userCreatedPayload.getUserEmail(),userCreatedPayload);
        LOGGER.info("Pushed userCreatedPayload to kafka: {}",future.get());
        return user.getId();
    }
}
