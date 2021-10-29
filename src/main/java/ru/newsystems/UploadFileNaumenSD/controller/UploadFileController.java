package ru.newsystems.UploadFileNaumenSD.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.newsystems.UploadFileNaumenSD.domain.MessageResponse;
import ru.newsystems.UploadFileNaumenSD.service.LocalPathFilesService;
import ru.newsystems.UploadFileNaumenSD.service.MultiPartAndRestService;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
//@RequestMapping(value = "/api/v1")
//@Scope("prototype")
public class UploadFileController {
    private static final Logger logger = LoggerFactory.getLogger(UploadFileController.class);

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    Environment environment;

    @Autowired
    private MultiPartAndRestService multiPartSupportService;

    @Autowired
    private LocalPathFilesService localPathFilesService;

    @Value("${host}")
    private String host;

    @Value("${port}")
    private String port;

    @Value("${restServices}")
    private String restServices;

    @Value("${tempDirOS}")
    private String tempDirOS;

    @Value("#{'${tmpDirFolder}'.split(',')}")
    private List<String> tmpDirFolder;

    @PostConstruct
    public void loadEntity() {
        String port = environment.getProperty("server.port");
        logger.warn("Controller is up! HOST: localhost, PORT: " + port);
    }

    @ResponseBody
    @PostMapping(value = "/api/v1/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFileToNSD(@RequestPart("file") MultipartFile file,
                                                  @RequestPart("msg") MessageResponse messageResponse
    ) throws IOException {

        String checkStatusUrl = multiPartSupportService.getCheckStatusUrl();
        if (!multiPartSupportService.checkPingHost()) {
            logger.error("Host: " + host + ":" + port + " is not available, try later");
        } else {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(checkStatusUrl, String.class);
            HttpStatus statusCode = responseEntity.getStatusCode();
            String availableUUUIDParam = multiPartSupportService.getUUIDServiceCall();

            if (statusCode == HttpStatus.OK) {
                int responseStatus = multiPartSupportService.pushMultipartFile(file, availableUUUIDParam);
                logger.warn("File load to " + availableUUUIDParam + ", response status: " + responseStatus);
                return ResponseEntity.ok("Success: File load");

            } else {
                logger.error("Check status: " + restServices + " is not available");
                return ResponseEntity.badRequest().body("400 Bad Request");
            }
        }
        return ResponseEntity.badRequest().body("500 Internal Server Error");
    }

    @GetMapping(value = "/")
    public ResponseEntity<String> getMetaDataForm(@RequestParam("rest") String rest,
                                                  @RequestParam("accessKey") String accessKey,
                                                  @RequestParam("uuid") String uuid,
                                                  @RequestParam("file") String file,
                                                  @RequestParam("fname") String fname,
                                                  @RequestParam("url") String url
    ) {

        MessageResponse messageResponse = new MessageResponse(rest, accessKey, uuid, file, fname, url);
        String pathFileFromMessageResponse = localPathFilesService.getPathResponseFileNameCheckOS(messageResponse);
        File localFileFromMessageResponse = new File(pathFileFromMessageResponse);

        while (!localFileFromMessageResponse.exists()) {
            try {
                Thread.sleep(1000);
                logger.error("File " + messageResponse.getFile() + " at the path " + pathFileFromMessageResponse + " not exist");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String contentTypeLocalFile = localPathFilesService.getResponseTempFileContentType(localFileFromMessageResponse);
        MultipartFile mockMultiPartFile = localPathFilesService.createMockMultiPartFile(messageResponse, contentTypeLocalFile, localFileFromMessageResponse);
        localPathFilesService.deleteLocalFile(localFileFromMessageResponse, pathFileFromMessageResponse);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = multiPartSupportService.getRequestEntity(mockMultiPartFile, messageResponse);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity("http://localhost:" + environment.getProperty("server.port") + "/api/v1/upload", requestEntity, String.class);
        } catch (Exception e) {
            System.err.println("ERRROR");
        }
        return ResponseEntity.ok("ok");
    }

}
