package com.wp.org.controller;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wp.org.LoginRequest;
import com.wp.org.dto.UserDto;
import com.wp.org.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/user-service")
public class UserController {

    private static Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationProvider authenticationProvider;

    @PostMapping("/user")
    public Long createUser(@RequestBody @Valid UserDto userDto) throws ExecutionException, InterruptedException {
        LOGGER.info("Processing UserCreation Request");
        return userService.createUser(userDto);
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateUser(@RequestBody LoginRequest loginRequest) {
    	try {
    		Authentication authentication = authenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), 
                            loginRequest.getPassword()
                        ));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return ResponseEntity.ok("Authenticated");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
