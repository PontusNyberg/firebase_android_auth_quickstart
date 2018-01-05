package com.example.pny.sso_auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookActivity;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;

import java.util.List;

/**
 * A login screen that offers login via CustomIdp, Google and Facebook.
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener{
    private static final String TAG = "SignInActivity";
    private static final int GOOGLE_SIGN_IN = 100;
    private int FACEBOOK_SIGN_IN = 0;

    private FirebaseAuth mAuth;

    private CallbackManager mCallbackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private String mCustomToken;
    private TokenBroadcastReceiver mTokenReceiver;

    private TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Configure Facebook login
        FACEBOOK_SIGN_IN = FacebookSdk.getCallbackRequestCodeOffset();
        mCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = findViewById(R.id.button_facebook_login);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                updateUI(null);
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                updateUI(null);
            }
        });

        // Configure google sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Configure customIdp login (using tokenReceiver in demo purposes)
        mTokenReceiver = new TokenBroadcastReceiver() {
            @Override
            public void onNewToken(String token) {
                Log.d(TAG, "onNewToken:" + token);
                setCustomToken(token);
            }
        };

        //Views
        mStatusTextView = findViewById(R.id.status_textview);
        // Button listeners
        findViewById(R.id.button_google_login).setOnClickListener(this);
        findViewById(R.id.signOutButton).setOnClickListener(this);
        findViewById(R.id.button_custom_login).setOnClickListener(this);

        // Configure IDP login
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();

        FacebookSdk.getClientToken();

        // Check if user is signed in (non-null) and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        //Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if(requestCode == GOOGLE_SIGN_IN) {
            System.out.println("LoginData: " + data.toString());
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authneticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI
                Log.w(TAG, "Google sign in failed", e);
                updateUI(null);
            }
        }
        else if(requestCode == FACEBOOK_SIGN_IN) {
            // Pass the activity result back to the Facebook SDK
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        showProgressDialog();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseSignIn(credential);
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handelFacebookAccessToken:" + token);

        findViewById(R.id.button_facebook_login).setActivated(false);
        showProgressDialog();

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseSignIn(credential);
    }

    private void firebaseSignIn(AuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.",
                                    Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        hideProgressDialog();
                    }
                });
    }

    private void customSignIn() {
        mAuth.signInWithCustomToken(mCustomToken)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCustomToken:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        }
                        else {
                            Log.w(TAG, "signInWithCustomToken:failure", task.getException());
                            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.",
                                    Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void setCustomToken(String token) {
        mCustomToken = token;

        String status;
        if(mCustomToken != null) {
            status = "Token: " + mCustomToken;
        }
        else {
            status = "Token: null";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only implemented for customIdp for now
        registerReceiver(mTokenReceiver, TokenBroadcastReceiver.getFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Only implemented for customIdp for now
        unregisterReceiver(mTokenReceiver);
    }

    private void signOut() {
        // Find active providers and sign them out
        if(mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            mAuth.fetchProvidersForEmail(mAuth.getCurrentUser().getEmail()).addOnCompleteListener(this, new OnCompleteListener<ProviderQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                    System.out.println("providers: " + task.getResult().getProviders());
                    List<String> providers = task.getResult().getProviders();
                    if (providers != null) {
                        for (String provider : providers) {
                            signOutProviders(provider);
                        }
                    }

                    // Firebase sign out after providers have been signed out
                    mAuth.signOut();
                }
            });
        }
        else {
            mAuth.signOut();
        }
        updateUI(null);
    }

    private void signOutProviders(String provider) {
        if(provider.equals("google.com")){
            // Google sign out
            mGoogleSignInClient.signOut().addOnCompleteListener(this,
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            updateUI(null);
                        }
                    });
        }
        else if(provider.equals("facebook.com")) {
            // Facebook sign out
            LoginManager.getInstance().logOut();
        }
    }

    private void revokeAccess() {
        signOut();
    }

    private void updateUI(FirebaseUser user) {
        if(user != null) {
            if(user.getEmail() != null && user.getUid() != null) {
                mStatusTextView.setText("Firebase User: " + user.getEmail() + "\nUser ID: " + user.getUid());
            }
            else if(user.getUid() != null) {
                mStatusTextView.setText("User ID: " + user.getUid());
            }

            findViewById(R.id.button_facebook_login).setVisibility(View.GONE);
            findViewById(R.id.button_google_login).setVisibility(View.GONE);
            findViewById(R.id.button_custom_login).setVisibility(View.GONE);
            findViewById(R.id.sign_out_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.signOutButton).setVisibility(View.VISIBLE);
        }
        else {
            mStatusTextView.setText("Signed Out");

            findViewById(R.id.button_facebook_login).setVisibility(View.VISIBLE);
            findViewById(R.id.button_google_login).setVisibility(View.VISIBLE);
            findViewById(R.id.button_custom_login).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_layout).setVisibility(View.GONE);
            findViewById(R.id.signOutButton).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if(i == R.id.button_google_login) {
            googleSignIn();
        }
        else if(i == R.id.button_custom_login) {
            customSignIn();
        }
        else if(i == R.id.signOutButton) {
            signOut();
        }
        else {
            revokeAccess();
        }
    }
}