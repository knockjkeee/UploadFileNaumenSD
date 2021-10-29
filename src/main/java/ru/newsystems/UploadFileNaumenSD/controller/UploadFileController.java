package ru.newsystems.UploadFileNaumenSD.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
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
import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
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

    @PostConstruct
    public void postConstructController() {
        String port = environment.getProperty("server.port");
        try {
            logger.warn("Controller is up! HOST: "+InetAddress.getLocalHost().getHostAddress()+", PORT: " + port);
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping(value = "/api/v1/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFileToNSD(@RequestPart("file") MultipartFile file,
                                                  @RequestPart("msg") MessageResponse messageResponse
    ) throws IOException {
        int responseStatus = multiPartSupportService.pushMultipartFileToNaumen(file, messageResponse);
        if (responseStatus < 300) {
            logger.warn("File load to " + messageResponse.getUuid() + ", response status: " + responseStatus);
            return ResponseEntity.ok("Success: File load");
        }
        logger.warn("Load file to " + messageResponse.getUuid() + " fail, response status: " + responseStatus);
        return ResponseEntity.badRequest().body("Unknown error, http code: " + responseStatus);
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
        pushedRequestToLocalService(messageResponse);
        return ResponseEntity.ok("ok");
    }

    public void pushedRequestToLocalService(MessageResponse messageResponse) {
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
        MultipartFile mockMultiPartFile = multiPartSupportService.createMockMultiPartFile(messageResponse, contentTypeLocalFile, localFileFromMessageResponse);

        localPathFilesService.deleteLocalFile(localFileFromMessageResponse, pathFileFromMessageResponse);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = multiPartSupportService.getRequestEntity(mockMultiPartFile, messageResponse);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    "http://" + InetAddress.getLocalHost().getHostAddress() + ":" +
                            environment.getProperty("server.port") + "/api/v1/upload",
                    requestEntity, String.class);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}
