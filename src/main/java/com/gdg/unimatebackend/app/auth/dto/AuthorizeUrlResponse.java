package com.gdg.unimatebackend.app.auth.dto;

public record AuthorizeUrlResponse(
        String authorizeUrl,
        String state
) {}
