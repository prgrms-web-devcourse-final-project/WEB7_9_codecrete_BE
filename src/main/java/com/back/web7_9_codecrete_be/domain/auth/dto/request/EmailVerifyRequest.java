package com.back.web7_9_codecrete_be.domain.auth.dto.request;

import lombok.Getter;

@Getter
public class EmailVerifyRequest {
    private String email;
    private String code;
}
