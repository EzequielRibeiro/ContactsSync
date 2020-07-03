package com.contacts.sync;

import android.content.Context;
import android.os.AsyncTask;
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
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private Drive service;
    private static GoogleSignInAccount account;
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH ="" ;
    private static Context context;
    private static GoogleTokenResponse tokenResponse;

    public DriveSync(Context context, GoogleSignInAccount account, String pathToken) throws IOException, GeneralSecurityException{

        this.context = context;
        this.account = account;
        TOKENS_DIRECTORY_PATH = pathToken;
        Log.w(TAG,"TOKENS_DIRECTORY_PATH="+TOKENS_DIRECTORY_PATH);
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
       // InputStream raw = context.getAssets().open("credentials.json");
        // Load client secrets.
        InputStream in = context.getAssets().open("credentials.json");
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
     /*  GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

      /*  try {
            // Make your Google API call
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            // Print out the message and errors
        }

       */
/*
       tokenResponse  =
                new GoogleAuthorizationCodeTokenRequest(
                        new NetHttpTransport(),
                        JSON_FACTORY,
                        "https://oauth2.googleapis.com/token",
                        clientSecrets.getDetails().getClientId(),
                        clientSecrets.getDetails().getClientSecret(),
                        account.getServerAuthCode(),
                        "")  // Specify the same redirect URI that you use with your web
                        // app. If you don't have a web version of your app, you can
                        // specify an empty string.
                        .setScopes(SCOPES)
                        .execute();
        String accessToken = tokenResponse.getAccessToken();
       return  new GoogleCredential().setAccessToken(accessToken);

/*
        return new GoogleCredential.Builder().setTransport(new NetHttpTransport())
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientSecrets)
                .build()
                .setFromTokenResponse(tokenResponse);

 */

      //  return credential;
        Log.w(TAG,"ClientID="+clientSecrets.getDetails().getClientId());
        GoogleAuthorizationCodeFlow authorizationCodeFlow = new GoogleAuthorizationCodeFlow
                .Builder(HTTP_TRANSPORT,JSON_FACTORY,clientSecrets.getDetails().getClientId(),clientSecrets.getDetails().getClientSecret(),
                SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .build();
        String userId = account.getId();
        Log.w(TAG,"userid="+userId);
        Credential credential = authorizationCodeFlow.loadCredential(userId);
        if (credential == null) {
            GoogleAuthorizationCodeRequestUrl authorizationUrl = authorizationCodeFlow.newAuthorizationUrl();
            authorizationUrl.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI);
            GoogleAuthorizationCodeTokenRequest tokenRequest = authorizationCodeFlow.newTokenRequest(account.getServerAuthCode());
            tokenRequest.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI);
            GoogleTokenResponse tokenResponse = tokenRequest.execute();
            credential = authorizationCodeFlow.createAndStoreCredential(tokenResponse, account.getId());
        }

        return credential;

    }


    public void uploadFile(String url, String fileName) throws  IOException{

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        java.io.File filePath = new java.io.File(url);
        FileContent mediaContent = new FileContent("image/jpeg", filePath);

     if(service != null) {
         File file = service.files().create(fileMetadata, mediaContent)
                 .setFields("id")
                 .execute();
         System.out.println("File ID: " + file.getId());
     }

    }

    @Override
    protected Void doInBackground(String... strings) {

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
        try {
            Drive drive =
                    new Drive.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), getCredentials(HTTP_TRANSPORT))
                            .setApplicationName("Contacts Sync")
                            .build();
            File file = drive.files().get("apk/app-release-noad.apk").execute();
            Log.w("Tamanho: ",String.valueOf(file.getSize()));
            // Get profile info from ID token
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


        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    protected void onPostExecute(String string) {

        FileList result = null;
        try {
            result = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }
    }

}

