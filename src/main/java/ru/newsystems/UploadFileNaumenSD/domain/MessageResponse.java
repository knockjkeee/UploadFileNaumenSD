package ru.newsystems.UploadFileNaumenSD.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageResponse {
    String rest;
    String accessKey;
    String uuid;
    String file;
    String fname;
    String url;
    String attr;

}

