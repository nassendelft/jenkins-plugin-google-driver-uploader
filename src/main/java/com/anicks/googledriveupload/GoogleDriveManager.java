package com.anicks.googledriveupload;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleDriveManager {

    private static final String APPLICATION_NAME = "Jenkins drive uploader";

    private static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private final Drive drive;

    public GoogleDriveManager(Credential credentials) throws GeneralSecurityException {
        drive = getDriveService(credentials);
    }

    private Drive getDriveService(Credential credential) throws GeneralSecurityException {
        return new Drive.Builder(
                HTTP_TRANSPORT, new JacksonFactory(), credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void store(String file, String remoteDir) throws IOException {
        java.io.File tmpFile = new java.io.File(file);
        insertFile(tmpFile.getName(), "", remoteDir, "", tmpFile);
    }

    /**
     * Insert new file.
     *
     * @param title Title of the file to insert, including the extension.
     * @param description Description of the file to insert.
     * @param parentId Optional parent folder's ID.
     * @param mimeType MIME type of the file to insert.
     * @param file The file to insert.
     * @return Inserted file metadata if successful, {@code null} otherwise.
     */
    private File insertFile(String title, String description, String parentId, String mimeType, java.io.File file)
            throws IOException {
        File body = new File();
        body.setTitle(title);
        body.setDescription(description);
        body.setMimeType(mimeType);

        if (parentId != null && parentId.length() > 0) {
            body.setParents(Collections.singletonList(new ParentReference().setId(parentId)));
        }

        FileContent mediaContent = new FileContent(mimeType, file);
        return drive.files().insert(body, mediaContent).execute();
    }
}
