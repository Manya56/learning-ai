package com.learningai.backend.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    // Optional — only update what's provided
    private String preferredLanguage;
    private String goalDescription;
    private String learningStyle;
}