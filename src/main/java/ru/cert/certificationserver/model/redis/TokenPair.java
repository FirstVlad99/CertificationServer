package ru.cert.certificationserver.model.redis;

public record TokenPair(String accessToken, String refreshToken) { }
