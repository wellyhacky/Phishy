package com.example.gmailplatform;

import android.accounts.Account;
import android.app.UiAutomation;
import android.content.Intent;
import android.os.AsyncTask;
//import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.*;


//import com.google.api.services.people.v1.model.ListConnectionsResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.text.similarity.LevenshteinDistance;


public class MainActivity extends AppCompatActivity  implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    //gui
    private LinearLayout layoutCompat;
    private SignInButton signInButton;
    private TextView emailDisp, output;
    private FloatingActionButton searchButton;
    private EditText input;
    //google
    private GoogleApiClient googleApiClient;
    private GoogleSignInClient googleSignInClient;
    private GoogleSignInAccount account;
    private static final int REQ_CODE_SIGNIN = 9001;
    private static final int REQUEST_AUTH = 9002;
    private String[] listScopes = {
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.MAIL_GOOGLE_COM
    };
    private final List SCOPES = Arrays.asList(listScopes);
    private List<String> trustedDomains = new ArrayList<>();
    boolean isLoggedIn = false;
    //var for debug
    private final String state = "STATE";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layoutCompat = (LinearLayout)findViewById(R.id.layout);
        signInButton = (SignInButton)findViewById(R.id.login);
        emailDisp = (TextView)findViewById(R.id.name);
        emailDisp.setText(R.string.pre_signin);
        output = (TextView)findViewById(R.id.output);
        output.setMovementMethod(new ScrollingMovementMethod());
        searchButton = (FloatingActionButton)findViewById(R.id.floatingActionButton);
        searchButton.setVisibility(View.GONE);
        input = (EditText)findViewById(R.id.edit_text);
        input.setVisibility(View.GONE);

        signInButton.setOnClickListener(this);
        searchButton.setOnClickListener(this);


        //google stuff
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this,gso);
/*
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .addScope()
                //.addApi(Gmail.)
                .build();
*/
        Log.d(state,"Post oncreate");



    }

    @Override
    public void onStart(){
        Log.d(state,"OnStart");

        super.onStart();
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct!=null){
            if (acct.getAccount()!=null)
            {
                Log.d(state, "account is not null");
                account = acct;
                isLoggedIn = true;
                updateUI(UIState.isLoggedIn);
            }

            else{
                updateUI(UIState.notLoggedIn);
            }

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.login:
                signIn();
                break;
            case R.id.floatingActionButton:
                if (isLoggedIn){
                    //searchButton.setVisibility(View.GONE);
                    trustedDomains.clear();
                    String[] inputTrusted = input.getText().toString().toLowerCase().split("\n");

                    trustedDomains.addAll(Arrays.asList(inputTrusted));
                    Log.d("TRUSTED",inputTrusted.toString());

                    new Networker(account.getAccount(),trustedDomains).execute();
                    updateUI(UIState.working);
                    //searchButton.setClickable(false);
                }

        }

    }

    @Override
    public void onConnectionFailed( ConnectionResult connectionResult) {

    }
    private void signIn(){
        //method to sign in to google account
        Intent signInIntent =  googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQ_CODE_SIGNIN);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == REQ_CODE_SIGNIN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }



    private void handleSignInResult(Task<GoogleSignInAccount> result){
        try {
            account = result.getResult(ApiException.class);
            if (account != null) {
                // Signed in successfully, show authenticated UI.

                updateUI(UIState.isLoggedIn);
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            emailDisp.setText(e.toString());
            updateUI(UIState.notLoggedIn);
        }

    }
    enum UIState {
        isLoggedIn,
        notLoggedIn,
        working,
        done

    }
    private void updateUI(UIState ui){
        switch (ui) {
            case isLoggedIn:
                isLoggedIn = true;

                signInButton.setVisibility(View.GONE);
                Log.d("STATE", "IS LOGGED IN");
                emailDisp.setText(account.getDisplayName());
                input.setVisibility(View.VISIBLE);
                searchButton.setVisibility(View.VISIBLE);
                //trustedDomains.add("welllngtonjags.org");
                //trustedDomains.add("unsplosh.com");
                //new Networker(account.getAccount(),trustedDomains).execute();
                break;
            case notLoggedIn:
                isLoggedIn = false;
                signInButton.setVisibility(View.VISIBLE);
                break;
            case working:
                searchButton.setVisibility(View.GONE);
                output.setText("Working!");
                break;
            case done:
                searchButton.setClickable(true);
                searchButton.setVisibility(View.VISIBLE);
                break;

        }
    }


    /** Global instance of the HTTP transport. */
    //AndroidHttp.newCompatibleTransport();
    private static HttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    //List<com.google.api.services.gmail.model.Message>
    private class Networker extends AsyncTask<Void, Void, String> {
        //in nanos
        long startTime;
        //in seconds
        double elapsedTime;
        //number of messages returned
        long numReturned;

        Account mAccount;
        //List of domain strings
        HashMap<String,String> domains;
        List<String> trustedDomains;
        HashMap<String,String> untrustedDomains;
        //below this in differences between words.
        int thresholdLD = 3;
        Soundex soundex = new Soundex();

        String phishDomains = "";

        public Networker(Account account,List<String> trustedDomains) {
            startTime = System.nanoTime();
            Log.d(state, "constructor for async");
            mAccount = account;
            domains = new HashMap<>();
            //this should be user submitted
            this.trustedDomains = trustedDomains;
            untrustedDomains = new HashMap<>();
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.d(state, "DoInBackground");

            List<com.google.api.services.gmail.model.Message> messages;
            try {

                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                MainActivity.this,
                                SCOPES
                        );
                credential.setSelectedAccount(mAccount);
                Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("REST API sample")
                        .build();

                Log.d("GMAIL", "past service");


                ListMessagesResponse response = service.users().messages().list("me").setMaxResults((long)50).execute();
                messages = response.getMessages();
                Log.d("GMAIL","retrieved");
                numReturned = response.getResultSizeEstimate();
                Message actualMessage;
                String[] fromFragments;
                String domainStr;
                LevenshteinDistance ld = new LevenshteinDistance(thresholdLD);
                String messId;
                //this is annoying - the id of the from header changes email to email when requesting more than one header.
                int fromId = 0;
                for (com.google.api.services.gmail.model.Message message : messages) {
                    //List<String> neededHeaders = Arrays.asList("From","Subject");
                    List<String> neededHeaders = Collections.singletonList("From");

                    actualMessage = service.users().messages().get("me", message.getId())
                            .setFormat("METADATA").setMetadataHeaders(neededHeaders).execute();
                    if(actualMessage == null){
                        Log.e("error in 216", "message is null");
                    }
                    //Log.d("GMAIL", actualMessage.toPrettyString());

                    MessagePartHeader fromHeader = actualMessage.getPayload().getHeaders().get(fromId);
                    fromFragments = fromHeader.getValue().split("@|>");

                    messId = actualMessage.getId();

                    domainStr = fromFragments[1];
                    //log and add just the domain
                    //Log.d("GMAIL",domainStr);
                    domains.put(messId,domainStr);
                    for(String d:trustedDomains) {
                        //if exactly the same, stop scanning and take off untrusted list, if similar, put on untrusted list
                        if(d.equals(domainStr)){
                            untrustedDomains.remove(messId);

                            break;
                        }
                        /*
                        Log.d("GMAIL",domainStr);
                        Log.d("TRUSTED", d);
                        Log.d("SOUNDEX",Integer.toString(soundex.difference(d,domainStr)));
                        Log.d("DIF", Integer.toString(org.apache.commons.lang3.StringUtils.compare(d,domainStr)));
                        Log.d("LD",Integer.toString(ld.apply(d,domainStr)));
                        */

                        if(ld.apply(d,domainStr)>-1){
                            untrustedDomains.put(messId,domainStr);
                            phishDomains= phishDomains+domainStr+"\n";
                        }


                    }
                    //Log.d("GMAIl",actualMessage.getPayload().getHeaders().get(17).getName());

                }
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTH);
                // Other non-recoverable exceptions.
            } catch (Exception e){
                Log.e("Error","Error In Async:",e);
                phishDomains = "There was an error! (sorry)\n";

            }
            return phishDomains;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            elapsedTime = (System.nanoTime()-startTime)/1000000000.0;//returns in seconds
            String phishAndTime = new StringBuilder(s)
                    .append(numReturned).append(" messages scanned, retreived in: ").append(Double.toString(elapsedTime)).toString();
            output.setText(phishAndTime);
            updateUI(UIState.done);
            untrustedDomains = new HashMap<>();
            phishDomains = "";
            return;
        }
    }


}






