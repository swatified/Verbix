package com.example.verbix;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private LinearLayout loginContainer, welcomeContainer;
    private TextView nameText, waitText, authText;
    private ShapeableImageView profileImage;
    private FirebaseAuth auth;
    private SignInButton signInButton;
    private GoogleSignInClient googleSignInClient;
    private Handler handler = new Handler();

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupGoogleSignIn();
        // Initially hide views
        authText.setAlpha(0f);
        signInButton.setAlpha(0f);
        startAnimations();
    }

    private void initializeViews() {
        loginContainer = findViewById(R.id.loginContainer);
        welcomeContainer = findViewById(R.id.welcomeContainer);
        nameText = findViewById(R.id.nameText);
        profileImage = findViewById(R.id.welcomeProfileImage);
        waitText = findViewById(R.id.waitText);
        authText = findViewById(R.id.authText);

        signInButton = findViewById(R.id.signIn);
        signInButton.setOnClickListener(v -> signIn());
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        auth = FirebaseAuth.getInstance();
    }

    private void startAnimations() {
        // Typing animation for wait text
        animatedText("Wait a sec..", waitText, () -> {
            // Slide down and fade in for auth text
            authText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down));
            authText.setAlpha(1f);

            // Pop animation for sign in button after delay
            new Handler().postDelayed(() -> {
                signInButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pop_up));
                signInButton.setAlpha(1f);
            }, 1500);
        });
    }

    private void animatedText(String text, TextView textView, Runnable onComplete) {
        StringBuilder currentText = new StringBuilder();
        int[] currentIndex = {0};

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentIndex[0] < text.length()) {
                    currentText.append(text.charAt(currentIndex[0]));
                    textView.setText(currentText.toString());
                    currentIndex[0]++;
                    handler.postDelayed(this, 100);
                } else if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        onSignInSuccess();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onSignInSuccess() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            loginContainer.setVisibility(View.GONE);
            welcomeContainer.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .into(profileImage);

            animateText("", nameText, () ->
                    animateText(user.getDisplayName(), nameText, () ->
                            handler.postDelayed(() -> {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                finish();
                            }, 2000)
                    )
            );
        }
    }

    private void animateText(String text, TextView textView, Runnable onComplete) {
        StringBuilder currentText = new StringBuilder();
        int[] currentIndex = {0};

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (currentIndex[0] < text.length()) {
                    currentText.append(text.charAt(currentIndex[0]));
                    textView.setText(currentText.toString());
                    currentIndex[0]++;
                    handler.postDelayed(this, 100);
                } else if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Disable back button
        super.onBackPressed();
        return;
    }
}