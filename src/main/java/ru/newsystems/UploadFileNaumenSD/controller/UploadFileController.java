package ru.newsystems.UploadFileNaumenSD.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriBuilder;
import ru.newsystems.UploadFileNaumenSD.service.SupportService;

@RestController
@RequestMapping(value = "/api/v1")
public class UploadFileController {
    private static final Logger logger = LoggerFactory.getLogger(UploadFileController.class);

    public UploadFileController() {
        logger.warn("Controller is up! HOST: localhost, PORT: 8080");
    }

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SupportService supportService;

    @Value("${host}")
    private String host;

    @Value("${port}")
    private String port;

    @Value("${restServices}")
    private String restServices;

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFileToNSD(@RequestParam("file") MultipartFile file) throws IOException {
        String checkStatusUrl = supportService.getCheckStatusUrl();
        if (!supportService.checkPingHost()) {
            logger.error("Host: " + host + ":" + port + " is not available, try later");
        } else {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(checkStatusUrl, String.class);
            HttpStatus statusCode = responseEntity.getStatusCode();
            String availableUUUIDParam = supportService.getUUIDServiceCall();

            if (statusCode == HttpStatus.OK) {
                int responseStatus = supportService.pushMultipartFile(file, availableUUUIDParam);
                logger.warn("File load to " + availableUUUIDParam + ", response status: " + responseStatus);
                return ResponseEntity.ok("Success: File load");

            } else {
                logger.error("Check status: " + restServices + " is not available");
                return ResponseEntity.badRequest().body("400 Bad Request");
            }
        }
        return ResponseEntity.internalServerError().body("500 Internal Server Error");
    }
}
