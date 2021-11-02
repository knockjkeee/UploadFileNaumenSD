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

    public UploadFileController() {
    }

    @PostConstruct
    public void postConstructController() {
        String port = environment.getProperty("server.port");
        try {
//            logger.warn("Controller is up! HOST: "+InetAddress.getLocalHost().getHostAddress()+", PORT: " + port);
            System.out.println("Controller is up! HOST: " + InetAddress.getLocalHost().getHostAddress() + ", PORT: " + port);
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
            System.out.println("File load to " + messageResponse.getUuid() + ", response status from Rest Naumen: " + responseStatus);
            return ResponseEntity.ok("Success: File load");
        }
        System.err.println("Load file to " + messageResponse.getUuid() + " fail, response status from Rest Naumen: " + responseStatus);
        return ResponseEntity.badRequest().body("Unknown error, http code: " + responseStatus);
    }

    @GetMapping(value = "/")
    public ResponseEntity<String> getMetaDataForm(@RequestParam("rest") String rest,
                                                  @RequestParam("accessKey") String accessKey,
                                                  @RequestParam("uuid") String uuid,
                                                  @RequestParam("file") String file,
                                                  @RequestParam("fname") String fname,
                                                  @RequestParam("url") String url,
                                                  @RequestParam(value = "attr", required = false) String attr
    ) {

        MessageResponse messageResponse = new MessageResponse(rest, accessKey, uuid, file, fname, url, attr);
        pushedRequestToLocalService(messageResponse);
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/status")
    public String status() {
        String port = environment.getProperty("server.port");
        try {
            return "App started, HOST: " + InetAddress.getLocalHost().getHostAddress() + ", PORT: " + port;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "Unknown Error";
    }

    public void pushedRequestToLocalService(MessageResponse messageResponse) {
        String pathFileFromMessageResponse = localPathFilesService.getPathResponseFileNameCheckOS(messageResponse);
        File localFileFromMessageResponse = new File(pathFileFromMessageResponse);
//        while (!localFileFromMessageResponse.exists()) {
//            try {
//                Thread.sleep(1000);
//                logger.error("File " + messageResponse.getFile() + " at the path " + pathFileFromMessageResponse + " not exist");
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        try {
            Thread.sleep(2000);
            if (!localFileFromMessageResponse.exists()) {
                System.err.println("File " + messageResponse.getFile() + " at the path " + pathFileFromMessageResponse + " not exist");
            } else {
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
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }
}
