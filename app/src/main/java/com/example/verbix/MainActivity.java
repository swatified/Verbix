package com.example.verbix;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView screeningCard, writingPatternCard, speechPatternCard,
            practiceCard, trainSpeechCard, logoutCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        setupStatusBar();
    }

    private void initializeViews() {
        screeningCard = findViewById(R.id.screeningCard);
        writingPatternCard = findViewById(R.id.writingPatternCard);
        speechPatternCard = findViewById(R.id.speechPatternCard);
        practiceCard = findViewById(R.id.practiceCard);
        trainSpeechCard = findViewById(R.id.trainSpeechCard);
        logoutCard = findViewById(R.id.logoutCard);

        // Make cards clickable and focusable
        CardView[] cards = {screeningCard, writingPatternCard, speechPatternCard,
                practiceCard, trainSpeechCard, logoutCard};
        for(CardView card : cards) {
            card.setClickable(true);
            card.setFocusable(true);
        }
    }

    private void setupClickListeners() {
        screeningCard.setOnClickListener(this);
        writingPatternCard.setOnClickListener(this);
        speechPatternCard.setOnClickListener(this);
        practiceCard.setOnClickListener(this);
        trainSpeechCard.setOnClickListener(this);

        logoutCard.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.client_id))
                    .requestEmail()
                    .build();

            GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(task -> {
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
        });
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.gray_background));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;

        if (v.getId() == R.id.screeningCard) {
            intent = new Intent(this, ScreeningActivity.class);
        } else if (v.getId() == R.id.writingPatternCard) {
            intent = new Intent(this, WritingPatternActivity.class);
        } else if (v.getId() == R.id.speechPatternCard) {
            intent = new Intent(this, SpeechPatternActivity.class);
        } else if (v.getId() == R.id.practiceCard) {
            intent = new Intent(this, PracticeWritingActivity.class);
        } else if (v.getId() == R.id.trainSpeechCard) {
            intent = new Intent(this, TrainSpeechActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button
        super.onBackPressed();
        return;
    }
}