package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import ru.lewis.leykabot.configuration.FragmentConfig;
import ru.lewis.leykabot.model.dto.fragment.FragmentApiResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FragmentPremiumService {
    private final FragmentConfig config;
    private final RestTemplate restTemplate;

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-API-KEY", config.getApiKey());
        return headers;
    }

    @Async
    public CompletableFuture<FragmentApiResponse> buyPremium(String username, int months) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String cleanUsername = username.startsWith("@") ? username.substring(1).trim() : username.trim();
                String url = config.getApiUrl() + "/premium/buy";

                Map<String, Object> body = new HashMap<>();
                body.put("username", cleanUsername);
                body.put("duration", months);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());

                ResponseEntity<FragmentApiResponse> response =
                        restTemplate.postForEntity(url, entity, FragmentApiResponse.class);

                return response.getBody();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.warn("Fragment Premium API returned error: {}", e.getResponseBodyAsString());
                try {
                    return e.getResponseBodyAs(FragmentApiResponse.class);
                } catch (Exception ex) {
                    FragmentApiResponse errorResp = new FragmentApiResponse();
                    errorResp.setOk(false);
                    errorResp.setMessage(e.getMessage());
                    return errorResp;
                }
            } catch (Exception e) {
                log.error("Error buying premium for username: {}", username, e);
                FragmentApiResponse errorResp = new FragmentApiResponse();
                errorResp.setOk(false);
                errorResp.setMessage(e.getMessage() != null ? e.getMessage() : "Noma'lum xatolik yuz berdi");
                return errorResp;
            }
        });
    }
}