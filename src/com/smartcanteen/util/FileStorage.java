package com.smartcanteen.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Saves an uploaded image's bytes to disk under uploads/<subFolder>/ and
 * returns the URL path the frontend can use in an <img> tag (served by
 * StaticFileHandler mapped at "/uploads").
 */
public class FileStorage {

    public static final String UPLOAD_DIR = "uploads";

    public static String saveFile(byte[] data, String originalFilename, String subFolder) {
        try {
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID() + extension;

            File folder = new File(UPLOAD_DIR, subFolder);
            folder.mkdirs();

            File target = new File(folder, fileName);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(data);
            }

            return "/uploads/" + subFolder + "/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}
