package com.gdg.unimatebackend.auth.dto;

public record AuthorizeUrlResponse(
        String authorizeUrl,
        String state
) {}
