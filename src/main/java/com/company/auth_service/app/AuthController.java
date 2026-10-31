package com.company.auth_service.app;

import com.company.auth_service.domain.LoginService;
import com.company.auth_service.domain.AuthService;
import com.company.auth_service.infrastructure.web.*;
import com.company.auth_service.infrastructure.web.mapper.UserWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserWebMapper userMapper;
    private final LoginService loginService;


    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(userMapper.toUser(request), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new LoginResponse(loginService.login(request.email(), request.password())));
    }

}