package ru.newsystems.UploadFileNaumenSD.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.newsystems.UploadFileNaumenSD.domain.MessageResponse;
import ru.newsystems.UploadFileNaumenSD.service.SupportService;

import javax.annotation.PostConstruct;

@RestController
//@RequestMapping(value = "/api/v1")
//@Scope("prototype")
public class UploadFileController {
    private static final Logger logger = LoggerFactory.getLogger(UploadFileController.class);

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

    @Value("${tempDirOS}")
    private String tempDirOS;

    @Autowired
    Environment environment;

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
        System.out.println("messageResponse!!!!!!!! = " + messageResponse);
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
        System.out.println("messageResponse = " + messageResponse);

        String tmpdir = environment.getProperty(tempDirOS);
        String pathFile = tmpdir + messageResponse.getFile();

        System.out.println("tmpdir = " + tmpdir);

        File tempFile = new File(pathFile);
        String tempFileContentType = null;
        MultipartFile newMultipartFile = null;


        while (!tempFile.exists()) {
            try {
                Thread.sleep(1000);
                logger.error("File " + messageResponse.getFile() + " at the path " + tmpdir + " not exist");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("tempFile = " + tempFile.getName());
        System.out.println("length = " + tempFile.length());
        System.out.println("tempFile out while= " + tempFile.exists());

        try {
            tempFileContentType = tempFile.toURI().toURL().openConnection().getContentType();
            newMultipartFile = new MockMultipartFile(file, fname, tempFileContentType,
                    new FileInputStream(tempFile));

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("tempFileContentType= " + tempFileContentType);

        boolean delete = tempFile.delete();
        if (delete) {
            logger.warn("File " + tempFile.getName() + "deleted at the path " + tmpdir);
        } else {
            logger.warn("File " + tempFile.getName() + "not deleted at the path " + tmpdir);
        }

        System.out.println("------------------");

        assert newMultipartFile != null;
        System.out.println("newMultipartFile = " + newMultipartFile.getName());
        System.out.println("newMultipartFile = " + newMultipartFile.getOriginalFilename());
        System.out.println("newMultipartFile = " + newMultipartFile.getContentType());
        System.out.println("newMultipartFile = " + newMultipartFile.getSize());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = supportService.getRequestEntity(newMultipartFile, messageResponse);

        MessageResponse msg = new MessageResponse();
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity("http://localhost:1980/api/v1/upload", requestEntity, String.class);
            System.out.println("responseEntity = " + responseEntity.getStatusCodeValue());
        } catch (Exception e) {
            System.err.println("ERRROR");
        }


        return ResponseEntity.ok("ok");
    }

}
