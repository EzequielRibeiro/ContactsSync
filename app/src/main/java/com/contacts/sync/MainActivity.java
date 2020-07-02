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
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int RC_SIGN_IN = 0;
    public static final String TAG =  "Contacts Sync";
    private Button buttonSign;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        buttonSign = findViewById(R.id.buttonSign);
        buttonSign.setOnClickListener(this);




    }

    @Override
    public void onClick(View view) {
    //client id Hpq9CebJQDV43w23e1kJBofw
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("134624384841-tahcrouqs05li43ovtqu89cigd0cvb07.apps.googleusercontent.com")
                .requestServerAuthCode("134624384841-tahcrouqs05li43ovtqu89cigd0cvb07.apps.googleusercontent.com")
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

            if(account != null) {
                Log.w(TAG, "account e-mail=" + account.getEmail());
                Log.w(TAG, "token=" + account.getIdToken());
            }

            // Signed in successfully, show authenticated UI.
            updateUI(account,null);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null,e);
        }
    }

    private void updateUI(GoogleSignInAccount account, ApiException e){

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
        Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private String createFileToken(String token) throws IOException {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + File.separator + "token");
        boolean f= false;
        if(!file.exists())
            f = file.createNewFile();

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
        outputStreamWriter.write(token);
        outputStreamWriter.close();

        Log.w(TAG,readFromFile(file.getAbsoluteFile().toString()));
        return file.getParent();

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
