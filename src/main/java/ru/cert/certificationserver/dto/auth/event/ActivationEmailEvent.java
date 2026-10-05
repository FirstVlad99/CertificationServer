package ru.cert.certificationserver.dto.auth.event;

public record ActivationEmailEvent(String email, String code) { }
