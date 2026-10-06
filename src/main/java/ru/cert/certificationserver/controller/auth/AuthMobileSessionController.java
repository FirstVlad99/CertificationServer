package ru.cert.certificationserver.controller.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.cert.certificationserver.controller.auth.http.AuthHttpHeaders;
import ru.cert.certificationserver.dto.auth.request.*;
import ru.cert.certificationserver.dto.auth.response.TokensMobileResponse;
import ru.cert.certificationserver.model.redis.TokenPair;
import ru.cert.certificationserver.service.AuthSessionService;

@RestController
@RequestMapping("/auth/mobile")
public class AuthMobileSessionController {
  private final AuthSessionService authService;

  public AuthMobileSessionController(AuthSessionService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<TokensMobileResponse> login(
      @Valid @RequestBody LoginRequest loginRequest,
      @RequestHeader(value = AuthHttpHeaders.USER_AGENT, required = false) String userAgent
  ) {
    TokenPair pair = authService.authenticate(
        loginRequest,
        userAgent
    );

    return ResponseEntity.ok(new TokensMobileResponse(pair.accessToken(), pair.refreshToken()));
  }

  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokensMobileResponse> refreshTokens(
      @Valid @RequestBody RefreshMobileRequest refreshRequest
  ) {
    TokenPair pair = authService.refreshSession(refreshRequest.refreshToken());

    return ResponseEntity.ok(new TokensMobileResponse(pair.accessToken(), pair.refreshToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @Valid @RequestBody LogoutMobileRequest request
  ) {
    authService.invalidateSession(request.refreshToken());

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/activate")
  public ResponseEntity<TokensMobileResponse> activateAccount(
      @Valid @RequestBody ActivateAccountRequest request,
      @RequestHeader(value = "User-Agent", required = false) String userAgent
  ) {
    TokenPair pair = authService.activateAccount(request, userAgent, request.getCode());

    return ResponseEntity.ok(new TokensMobileResponse(pair.accessToken(),pair.refreshToken()));
  }

  @PostMapping("/activate/resend")
  public ResponseEntity<Void> activateAccount(
      @Valid @RequestBody ResendActivationRequest request
  ) {
    authService.resendActivationCode(request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/activate/verify")
  public ResponseEntity<Void> verifyActivationCode(
      @Valid @RequestHeader(value = AuthHttpHeaders.ACTIVATE_CODE) @NotBlank(message="X-Activation-Code must not be blank") String code
  ) {
    authService.verifyActivationCode(code);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request
  ) {
    authService.forgotPassword(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request
  ) {
    authService.resetPassword(request, request.code());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/reset-password/verify")
  public ResponseEntity<Void> verifyResetCode(
      @Valid @RequestHeader(value = AuthHttpHeaders.RESET_PASSWORD_CODE) @NotBlank(message="X-Reset-Password-Code must not be blank") String code
  ) {
    authService.verifyResetCode(code);
    return ResponseEntity.noContent().build();
  }
}
