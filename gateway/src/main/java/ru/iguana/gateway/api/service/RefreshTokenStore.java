package ru.iguana.gateway.api.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenStore {

    private final Map<String, String> storage = new ConcurrentHashMap<>();

    public void save(String sub, String refreshToken) {
        storage.put(sub, refreshToken);
    }

    public boolean isValid(String sub, String refreshToken) {
        return refreshToken.equals(storage.get(sub));
    }

    public void delete(String sub) {
        storage.remove(sub);
    }
}
