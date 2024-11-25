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

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView screeningCard, writingPatternCard, speechPatternCard,
            practiceCard, trainSpeechCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize cards
        screeningCard = findViewById(R.id.screeningCard);
        writingPatternCard = findViewById(R.id.writingPatternCard);
        speechPatternCard = findViewById(R.id.speechPatternCard);
        practiceCard = findViewById(R.id.practiceCard);
        trainSpeechCard = findViewById(R.id.trainSpeechCard);

        // Set click listeners
        screeningCard.setOnClickListener(this);
        writingPatternCard.setOnClickListener(this);
        speechPatternCard.setOnClickListener(this);
        practiceCard.setOnClickListener(this);
        trainSpeechCard.setOnClickListener(this);

        // Add ripple effect to cards
        screeningCard.setClickable(true);
        writingPatternCard.setClickable(true);
        speechPatternCard.setClickable(true);
        practiceCard.setClickable(true);
        trainSpeechCard.setClickable(true);

        screeningCard.setFocusable(true);
        writingPatternCard.setFocusable(true);
        speechPatternCard.setFocusable(true);
        practiceCard.setFocusable(true);
        trainSpeechCard.setFocusable(true);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.gray_background));

        // Make status bar icons dark
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
}