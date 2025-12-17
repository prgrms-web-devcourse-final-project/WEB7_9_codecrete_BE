package com.back.web7_9_codecrete_be.domain.auth.dto.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class GoogleUserResponse {

    private String id;
    private String email;
    private String name;

    @JsonProperty("picture")
    private String profileImageUrl;

    public GoogleUserInfo toUserInfo() {
        return new GoogleUserInfo(
                id,
                email,
                name,
                profileImageUrl
        );
    }
}
