package ru.newsystems.UploadFileNaumenSD.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.newsystems.UploadFileNaumenSD.domain.MessageResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Scope("prototype")
public class MultiPartAndRestService {
    private static final Logger logger = LoggerFactory.getLogger(MultiPartAndRestService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${host}")
    private String host;

    @Value("${port}")
    private String port;

    @Value("${restServices}")
    private String restServices;

    @Value("${postMethodCheckStatus}")
    private String postMethodCheckStatus;

    @Value("${accessKey}")
    private String accessKey;

    @Value("${getMethodAddFile}")
    private String getMethodAddFile;


    public String getUUIDServiceCall() {
        String serviceCallUrl = "http://" + host + ":" + port + restServices + "find/serviceCall/" + accessKey + "&title=INC7";
        ResponseEntity<JsonNode> serviceCallJson = restTemplate.getForEntity(serviceCallUrl, JsonNode.class);
        JsonNode mapServiceCall = serviceCallJson.getBody();
        assert mapServiceCall != null;
        List<String> availableUUUIDParam = mapServiceCall.findValuesAsText("UUID");
        return availableUUUIDParam.get(0);
    }

    public String getCheckStatusUrl() {
        return "http://" + host + ":" + port + restServices + postMethodCheckStatus;
    }

    public boolean checkPingHost() {
        return pingHost(host, Integer.parseInt(port), 1000);
    }

    public int pushMultipartFile(MultipartFile file, String uUIDParam) throws IOException {
        String dateTimeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy"));
        MultipartFile newMultipartFile = getNewMultipartFile(file, dateTimeNow);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = getRequestEntity(newMultipartFile, null);

        //        String serverUrl = "http://192.168.246.76:8080/sd/services/rest/add-file/serviceCall$1615707?accessKey=b2921f00-1f46-4eaf-a90c-c8633f9bf8ca";
        String serverUrl = "http://" + host + ":" + port + restServices + getMethodAddFile + "/" + uUIDParam + accessKey;
        ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);
        getLogging(file, uUIDParam, newMultipartFile);
        return response.getStatusCodeValue();
    }

    public HttpEntity<MultiValueMap<String, Object>> getRequestEntity(MultipartFile newMultipartFile, MessageResponse messageResponse) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        Resource invoicesResource = newMultipartFile.getResource();
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.set("file", invoicesResource);
        if (messageResponse != null) {
            HttpHeaders requestHeadersJSON = new HttpHeaders();
            requestHeadersJSON.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MessageResponse> messageResponseHttpEntity = new HttpEntity<>(messageResponse, requestHeadersJSON);
            body.set("msg", messageResponseHttpEntity);
        }
        return new HttpEntity<>(body, headers);
    }

    private void getLogging(MultipartFile file, String uUIDParam, MultipartFile newMultipartFile) {
        logger.warn("File original name: " + file.getOriginalFilename());
        logger.warn("File custom name: " + newMultipartFile.getOriginalFilename());
        logger.warn("File content type: " + newMultipartFile.getContentType());
        logger.warn("File size: " + newMultipartFile.getSize());
        logger.warn("UUID param: " + uUIDParam);
        logger.warn("Method push to Naumen SD: " + getMethodAddFile);
    }

    private MultipartFile getNewMultipartFile(MultipartFile file, String dateTimeNow) throws IOException {
        String originalMultipartFileName = file.getOriginalFilename();
        assert originalMultipartFileName != null;
        String subMultipartFileName = originalMultipartFileName.substring(0, file.getOriginalFilename().length() - 4);
        String endSubMultipartFileName = originalMultipartFileName.substring(file.getOriginalFilename().length() - 4, file.getOriginalFilename().length());
        String newsMultipartFileName = subMultipartFileName.concat("_" + dateTimeNow + endSubMultipartFileName);
        MultipartFile newMultipartFile = new MockMultipartFile(file.getName(), newsMultipartFileName, file.getContentType(), file.getInputStream());
        return newMultipartFile;
    }

    private static boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
