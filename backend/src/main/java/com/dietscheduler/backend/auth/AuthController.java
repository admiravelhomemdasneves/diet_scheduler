package com.dietscheduler.backend.auth;

import com.dietscheduler.backend.auth.dto.AuthResponse;
import com.dietscheduler.backend.auth.dto.GoogleSignInRequest;
import com.dietscheduler.backend.auth.dto.UserResponse;
import com.dietscheduler.backend.user.AuthProvider;
import com.dietscheduler.backend.user.User;
import com.dietscheduler.backend.user.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(GoogleTokenVerifierService googleTokenVerifierService, JwtService jwtService, UserRepository userRepository) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/google")
    public AuthResponse signInWithGoogle(@Valid @RequestBody GoogleSignInRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(request.idToken());

        String subject = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByAuthProviderAndProviderSubjectId(AuthProvider.GOOGLE, subject)
                .map(existing -> {
                    existing.setEmail(email);
                    if (name != null) {
                        existing.setDisplayName(name);
                    }
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .authProvider(AuthProvider.GOOGLE)
                        .providerSubjectId(subject)
                        .email(email)
                        .displayName(name != null ? name : email)
                        .build());
        user = userRepository.save(user);

        String token = jwtService.issueToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UUID userId) {
        return UserResponse.from(userRepository.findRequiredById(userId));
    }
}
