package com.example.scienceops.admin.auth;

import com.example.scienceops.security.AdminAuthRepository;
import com.example.scienceops.security.AdminPrincipal;
import com.example.scienceops.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminAuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthService(AdminAuthRepository authRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String username, String password) {
        AdminAuthRepository.AdminAccountRecord account = authRepository.findAccountByUsername(username)
                .orElseThrow(() -> new LoginFailedException("Invalid username or password"));
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw new LoginFailedException("Invalid username or password");
        }
        if (!"ENABLED".equals(account.status())) {
            throw new DisabledAdminException("Admin account is disabled");
        }

        AdminPrincipal principal = authRepository.loadPrincipal(username);
        return new LoginResponse(jwtService.createToken(principal), AdminProfileResponse.from(principal));
    }

    public static class LoginFailedException extends RuntimeException {
        public LoginFailedException(String message) {
            super(message);
        }
    }

    public static class DisabledAdminException extends RuntimeException {
        public DisabledAdminException(String message) {
            super(message);
        }
    }
}
