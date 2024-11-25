package com.example.verbix;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView mascotImage;
    private ImageView verbixLogo;
    private ProgressBar loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        mascotImage = findViewById(R.id.Mascot);
        verbixLogo = findViewById(R.id.Logo);
        loading = findViewById(R.id.progress);

        // Set initial alpha to 0 (completely transparent)
        mascotImage.setAlpha(0f);
        verbixLogo.setAlpha(0f);
        loading.setAlpha(0f);

        // Set progress to 0
        loading.setProgress(0);

        // Start the animation sequence
        startAnimations();
    }

    private void startAnimations() {
        // Mascot animations
        ObjectAnimator mascotFadeIn = ObjectAnimator.ofFloat(mascotImage, "alpha", 0f, 1f);
        ObjectAnimator mascotSlideDown = ObjectAnimator.ofFloat(mascotImage, "translationY", -200f, 0f);

        // Verbix logo animations
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(verbixLogo, "alpha", 0f, 1f);
        ObjectAnimator logoSlideUp = ObjectAnimator.ofFloat(verbixLogo, "translationY", 200f, 0f);

        // Progress bar animations
        ObjectAnimator progressFadeIn = ObjectAnimator.ofFloat(loading, "alpha", 0f, 1f);
        ObjectAnimator progressAnimation = ObjectAnimator.ofInt(loading, "progress", 0, 100);
        progressAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        // Create animation set
        AnimatorSet animatorSet = new AnimatorSet();

        // Play mascot animations together
        AnimatorSet mascotAnim = new AnimatorSet();
        mascotAnim.playTogether(mascotFadeIn, mascotSlideDown);

        // Play logo animations together
        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(logoFadeIn, logoSlideUp);

        // Play progress animations
        AnimatorSet progressAnim = new AnimatorSet();
        progressAnim.playSequentially(progressFadeIn, progressAnimation);

        // Sequence all animations
        animatorSet.play(mascotAnim) // Start with mascot
                .before(logoAnim);    // Then show logo
        animatorSet.play(progressAnim).after(logoAnim); // Finally show and animate progress

        // Set duration for all animations
        mascotAnim.setDuration(1000);
        logoAnim.setDuration(800);
        progressFadeIn.setDuration(400);
        progressAnimation.setDuration(1500); // Progress animation takes 1.5 seconds

        // Add listener to start main activity
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Animation started
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Wait for a moment after animations complete
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startMainActivity();
                    }
                }, 300); // Shorter delay since we have progress animation
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Animation cancelled
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Animation repeated
            }
        });

        // Start the animations
        animatorSet.start();
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);

        // Add exit transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Close splash activity
        finish();
    }

    // Optional: Handle back button
    @Override
    public void onBackPressed() {
        // Disable back button during splash screen
        return;
    }
}