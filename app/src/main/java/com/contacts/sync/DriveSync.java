package com.contacts.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;


import static com.contacts.sync.MainActivity.TAG;

public class DriveSync extends AsyncTask<String, Void, Void> {
    private static final String APPLICATION_NAME = "Contacts Sync";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /* Global instance of the scopes required by this quickstart.
      If modifying these scopes, delete your previously saved tokens/ folder.*/
    private static String TOKENS_DIRECTORY_PATH;
    private Drive driveService;
    private static GoogleSignInAccount account;
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static String DB_FILE_PATH;
    private static Context context;
    private static GoogleTokenResponse tokenResponse;
    private static final String mimeType =  "application/octet-stream";


    public DriveSync(Context context, GoogleSignInAccount account, String pathToken) throws IOException, GeneralSecurityException{

        this.context = context;
        this.account = account;
        TOKENS_DIRECTORY_PATH = pathToken;
        Log.w(TAG,"TOKENS_DIRECTORY_PATH="+TOKENS_DIRECTORY_PATH);

        java.io.File fileRoot = new java.io.File( Environment.getExternalStorageDirectory()+ java.io.File.separator, "Contacts");

        if(!fileRoot.exists())
            fileRoot.mkdirs();
        DB_FILE_PATH = fileRoot.getParent();


    }


    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {

        InputStream in = context.getAssets().open("credentials.json");
        if (in == null) {
            throw new FileNotFoundException("credentials.json not found");
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        Log.w(TAG,"ClientID="+clientSecrets.getDetails().getClientId());
        GoogleAuthorizationCodeFlow authorizationCodeFlow = new GoogleAuthorizationCodeFlow
                .Builder(HTTP_TRANSPORT,JSON_FACTORY,clientSecrets.getDetails().getClientId(),clientSecrets.getDetails().getClientSecret(),
                SCOPES)
                .setJsonFactory(new JacksonFactory().createJsonParser(new InputStreamReader(in)).getFactory())
              //  .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))

                .build();
        String userId = account.getId();
        Log.w(TAG,"userid="+userId);
        Log.w(TAG,"CodeFlowClientId="+authorizationCodeFlow.getClientId());
        Credential credential = authorizationCodeFlow.loadCredential(userId);
        if (credential == null) {
            GoogleAuthorizationCodeRequestUrl authorizationUrl = authorizationCodeFlow.newAuthorizationUrl();
            authorizationUrl.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI);
            GoogleAuthorizationCodeTokenRequest tokenRequest = authorizationCodeFlow.newTokenRequest(account.getServerAuthCode());
            tokenRequest.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI);
            tokenResponse = tokenRequest.execute();
            credential = authorizationCodeFlow.createAndStoreCredential(tokenResponse, account.getId());
        }

        return credential;

    }
    File file = null;
    String folderId;
    String fileId;

    private void createFolder() throws IOException {

     if(!folderExist()) {

         File fileMetadata = new File();
         fileMetadata.setName("ContactsSync");
         fileMetadata.setDescription("Backup created by Contacts Sync.");
         fileMetadata.setMimeType("application/vnd.google-apps.folder");
         try {
             file = driveService.files().create(fileMetadata)
                     .setFields("id")
                     .execute();
             folderId = file.getId();
             Log.w(TAG, "Folder Created ID: " + file.getId());
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
      Log.w(TAG,"File exist: "+ Boolean.valueOf(fileExist()));


    }

    public void uploadFile(String pathFile, String description,String mimeType, String fileName) throws  IOException{

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setDescription(description);
        fileMetadata.setParents(Collections.singletonList(folderId));
        java.io.File filePath = new java.io.File(pathFile);
        FileContent mediaContent = new FileContent(mimeType, filePath);
        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();
        fileId = file.getId();
        Log.w(TAG,"File ID: " + file.getId());

    }

    private boolean folderExist() throws IOException {

        String pageToken = null;
        FileList result;
        do {
             result = driveService.files().list()
                    .setQ("mimeType = 'application/vnd.google-apps.folder' and name='ContactsSync'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();

            for (File file : result.getFiles()) {
                Log.w(TAG,"Folder " + file.getName() +" id=" + file.getId());
                if(file.getName().equals("ContactsSync")) {
                    folderId = file.getId();
                    return true;
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

       return false;
    }

    private boolean fileExist() throws IOException {

        String pageToken = null;
        do {
            FileList result = driveService.files().list()
                    .setQ("mimeType = 'application/vnd.google-apps.file' and name = 'contactsdb'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                Log.w(TAG, "Found file: " + file.getName() + "id=" + file.getId());
                if (file.getName().equals("contactsdb")) {
                    fileId = file.getId();
                    return true;
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return  false;

    }

    private File updateFile(Drive service, String fileId, String newTitle,
                                   String newDescription, String newMimeType, String newFilename, boolean newRevision) {
        try {

            File file = new File();
            file.setName(newTitle);
            file.setDescription(newDescription);
            file.setMimeType(newMimeType);
            java.io.File fileContent = new java.io.File(newFilename);
            FileContent mediaContent = new FileContent(newMimeType, fileContent);
            File updatedFile = service.files().update(fileId, file, mediaContent).execute();
            return updatedFile;
        } catch (IOException e) {
            Log.w(TAG,"An error occurred with updateFile: " + e);
            return null;
        }
    }

    private void getFileFromDrive() throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);

        OutputStream output = new FileOutputStream(DB_FILE_PATH + "contactsdb.db");
        outputStream.writeTo(output);

        GoogleIdToken idToken = tokenResponse.parseIdToken();
        GoogleIdToken.Payload payload = idToken.getPayload();
        String userId = payload.getSubject();  // Use this value as a key to identify a user.
        String email = payload.getEmail();
        boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String locale = (String) payload.get("locale");
        String familyName = (String) payload.get("family_name");
        String givenName = (String) payload.get("given_name");

        Log.w(TAG,"File SIZE="+ outputStream.size());

        Log.w(TAG,userId+email+emailVerified+name+pictureUrl+locale+familyName+givenName);


    }

    private void permissionFileGrant(String fileId){

        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                // Handle error
                Log.w(TAG,"Permission error="+e.getMessage());
            }

            @Override
            public void onSuccess(Permission permission,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                Log.e(TAG,"Permission ID: " + permission.getId());
            }
        };
        BatchRequest batch = driveService.batch();
        Permission userPermission = new Permission()
                .setType("user")
                .setRole("writer")
                .setEmailAddress(account.getEmail());
        try {
            driveService.permissions().create(fileId, userPermission)
                    .setFields("id")
                    .queue(batch, callback);
            Permission domainPermission = new Permission()
                    .setType("user")
                    .setRole("reader")
                    .setDomain("example.com");
            driveService.permissions().create(fileId, domainPermission)
                    .setFields("id")
                    .queue(batch, callback);

            batch.execute();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected Void doInBackground(String... strings) {

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        try {
            driveService =
                    new Drive.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), getCredentials(HTTP_TRANSPORT))
                            .setApplicationName("Contacts Sync")
                            .build();

        createFolder();

        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    protected void onPostExecute(String string) {

        FileList result = null;
        try {
            result = driveService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            Log.w(TAG,"No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
               Log.w(TAG,"File name= "+file.getName() + " id= "+file.getId());
            }
        }
    }

}

