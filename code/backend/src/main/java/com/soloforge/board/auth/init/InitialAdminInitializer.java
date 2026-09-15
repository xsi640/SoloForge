package com.soloforge.board.auth.init;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.soloforge.board.auth.service.AuthService;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private final AuthService authService;

    public InitialAdminInitializer(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.ensureInitialAdmin();
    }
}
