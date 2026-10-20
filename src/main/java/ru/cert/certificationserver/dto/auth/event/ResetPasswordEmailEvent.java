package ru.cert.certificationserver.dto.auth.event;

public record ResetPasswordEmailEvent(String email, String code) { }
