package com.example.project.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class OtpRequest {
    private String telefono;
    private String otp;
}
