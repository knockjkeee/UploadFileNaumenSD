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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    public MultipartFile createMockMultiPartFile(MessageResponse messageResponse, String tempFileContentType, File file) {
        MultipartFile multipartFile = null;
        try {
            multipartFile = new MockMultipartFile(messageResponse.getFile(), messageResponse.getFname(),
                    tempFileContentType, new FileInputStream(file));
        } catch (IOException e) {
            logger.error("Class:LocalPathFilesService, method:createMockMultipartFile fail, msg: don`t create mock multi part file");
        }
        return multipartFile;
    }

    public int pushMultipartFileToNaumen(MultipartFile file, MessageResponse messageResponse) throws IOException {
        MultipartFile newMultipartFile = new MockMultipartFile(messageResponse.getFile(), messageResponse.getFname(), file.getContentType(), file.getInputStream());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = getRequestEntity(newMultipartFile, null);
        String serverUrl = getServerUrlToNaumen(messageResponse);
        ResponseEntity<String> response = null;
        try {
             response = restTemplate.postForEntity(serverUrl, requestEntity, String.class);
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        getLogging(messageResponse, newMultipartFile);
        assert response != null;
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

    private String getServerUrlToNaumen(MessageResponse messageResponse) {
        return (messageResponse.getAttr() == null)
                ? messageResponse.getUrl() + restServices + messageResponse.getRest() + "/" + messageResponse.getUuid() + "?accessKey=" + messageResponse.getAccessKey()
                : messageResponse.getUrl() + restServices + messageResponse.getRest() + "/" + messageResponse.getUuid() + "?accessKey=" + messageResponse.getAccessKey() + "&attrCode="+messageResponse.getAttr();
    }

    private void getLogging(MessageResponse messageResponse, MultipartFile newMultipartFile) {
        System.out.println("File pushed to Naumen SD with property: ");
        System.out.println("{   File original name: " + messageResponse.getFile());
        System.out.println("    File custom name: " + newMultipartFile.getOriginalFilename());
        System.out.println("    File content type: " + newMultipartFile.getContentType());
        System.out.println("    File size: " + newMultipartFile.getSize());
        System.out.println("    UUID param: " + messageResponse.getUuid());
        System.out.println("    Method push to Naumen SD: " + messageResponse.getRest() + "   }");
    }

    public String getUUIDServiceCall(MessageResponse messageResponse) {
        String serviceCallUrl = "http://" + host + ":" + port + restServices + "find/serviceCall/" + accessKey + "&title=INC7";
        ResponseEntity<JsonNode> serviceCallJson = restTemplate.getForEntity(serviceCallUrl, JsonNode.class);
        JsonNode mapServiceCall = serviceCallJson.getBody();
        assert mapServiceCall != null;
        List<String> availableUUUIDParam = mapServiceCall.findValuesAsText("UUID");
        return availableUUUIDParam.get(0);
    }

    public String getCheckStatusUrl(MessageResponse messageResponse) {
        return "http://" + host + ":" + port + restServices + postMethodCheckStatus;
    }

    public boolean checkPingHost() {
        return pingHost(host, Integer.parseInt(port), 1000);
    }

    private MultipartFile getNewMultipartFileCustomName(MultipartFile file, String dateTimeNow) throws IOException {
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
