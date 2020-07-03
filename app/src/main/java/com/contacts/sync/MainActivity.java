package com.contacts.sync;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.io.*;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int RC_SIGN_IN = 0;
    public static final String TAG =  "Contacts Sync";
    private static String TOKENS_DIRECTORY_PATH = "";
    private Button buttonSign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSign = findViewById(R.id.buttonSign);
        buttonSign.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        String client_id = "134624384841-2dh7goh46sbhftu99da7h043bo27qcjk.apps.googleusercontent.com";


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(client_id)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "ApiException code=" + e.getStatusCode());

        }
    }

    private void updateUI(GoogleSignInAccount account){

        if(account != null) {
            Log.w(TAG, "account e-mail=" + account.getEmail());
            Log.w(TAG, "token=" + account.getIdToken());

            try {
                new DriveSync(getApplicationContext(), account,createFileToken(account.getIdToken())).execute();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (GeneralSecurityException ex) {
                ex.printStackTrace();
            }
        }
        else{
        Log.w(TAG, "GoogleSignInAccount= " + "isNull");
        }
    }

    private String createFileToken(String token) throws IOException {
        File fileRoot = new File( Environment.getExternalStorageDirectory()+File.separator, "tokens");

        if(!fileRoot.exists())
            fileRoot.mkdirs();

        File tokenFile = new File(fileRoot,"token");
        FileWriter writer = new FileWriter(tokenFile,true);
        writer.write(token);
        writer.flush();
        writer.close();
        Log.w(TAG,"Token path="+tokenFile.getAbsoluteFile().toString());
        Log.w(TAG,"Token file="+readFromFile(tokenFile.getAbsoluteFile().toString()));

        return tokenFile.getParent();

    }

    private String readFromFile(String path) {

        String ret = "";

        try {
            InputStream inputStream = new FileInputStream(
                    new File(path));

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

}
