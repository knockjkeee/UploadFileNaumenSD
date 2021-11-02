package ru.newsystems.UploadFileNaumenSD.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ru.newsystems.UploadFileNaumenSD.domain.MessageResponse;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.List;

@Service
@Scope("prototype")
public class LocalPathFilesService {
    private static final Logger logger = LoggerFactory.getLogger(LocalPathFilesService.class);

    private final Environment environment;

    @Value("#{'${tmpDirFolder}'.split(',')}")
    private List<String> tmpDirFolder;

    public LocalPathFilesService(Environment environment) {
        this.environment = environment;
    }

    public String getPathResponseFileNameCheckOS(MessageResponse messageResponse) {
        String tmpdir = null;
        String osName = System.getProperty("os.name");
        if (osName.contains("Mac")) {
            tmpdir = environment.getProperty(tmpDirFolder.get(0));
        } else {
            tmpdir = tmpDirFolder.get(1);
        }
        return tmpdir + messageResponse.getFile();
    }

    public String getResponseTempFileContentType(File file) {
        String fileContentType = null;
        try {
            URLConnection connection = file.toURI().toURL().openConnection();
            fileContentType = connection.getContentType();
            connection.getInputStream().close();
        } catch (IOException e) {
            logger.error("Class:LocalPathFilesService, method:getTempFileContentType fail, msg: don`t get content type to url response file");
        }
        return fileContentType;
    }

    public void deleteLocalFile(File file, String path) {
        boolean delete = file.delete();
        if (delete) {
            System.out.println("File " + file.getName() + " deleted at the path " + path);
        } else {
            System.out.println("File " + file.getName() + " not deleted at the path " + path);
        }
    }
}
