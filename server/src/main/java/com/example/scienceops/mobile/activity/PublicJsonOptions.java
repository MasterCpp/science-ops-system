package com.example.scienceops.mobile.activity;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

final class PublicJsonOptions {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private PublicJsonOptions() {
    }

    static List<String> parse(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            Object value = OBJECT_MAPPER.readValue(optionsJson, Object.class);
            if (value instanceof String wrappedJson) {
                return OBJECT_MAPPER.readValue(wrappedJson, STRING_LIST);
            }
            return OBJECT_MAPPER.convertValue(value, STRING_LIST);
        } catch (Exception exception) {
            return List.of();
        }
    }
}
