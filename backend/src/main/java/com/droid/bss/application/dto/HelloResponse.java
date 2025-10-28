package com.droid.bss.application.dto;

import java.util.List;

public record HelloResponse(String message, String subject, List<String> roles) {
}
