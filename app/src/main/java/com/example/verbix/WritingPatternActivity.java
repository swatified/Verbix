package com.example.verbix;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WritingPatternActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView paragraphText, resultText;
    private ImageView scannedImage;
    private Button scanButton;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writing_pattern);

        initViews();
        db = FirebaseFirestore.getInstance();
        getRandomParagraph();

        scanButton.setOnClickListener(v -> startCamera());
    }

    private void initViews() {
        paragraphText = findViewById(R.id.paragraphText);
        resultText = findViewById(R.id.resultText);
        scannedImage = findViewById(R.id.scannedImage);
        scanButton = findViewById(R.id.scanButton);
    }

    private void getRandomParagraph() {
        db.collection("practicepara")
                .document("writingprac")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Log.d("Firebase", "Data: " + data);

                        if (data != null && !data.isEmpty()) {
                            // Convert Object values to String
                            List<String> paragraphs = new ArrayList<>();
                            for (Object value : data.values()) {
                                paragraphs.add(value.toString());
                            }

                            Log.d("Firebase", "Paragraphs: " + paragraphs);

                            int randomIndex = new Random().nextInt(paragraphs.size());
                            String selectedParagraph = paragraphs.get(randomIndex);
                            Log.d("Firebase", "Selected: " + selectedParagraph);

                            paragraphText.setText(selectedParagraph);
                        } else {
                            Log.e("Firebase", "Data is null or empty");
                        }
                    } else {
                        Log.e("Firebase", "Document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error getting document", e);
                    Toast.makeText(WritingPatternActivity.this,
                            "Error loading paragraph: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Camera not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            scannedImage.setImageBitmap(imageBitmap);
            recognizeText(imageBitmap);
        }
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(text -> compareTexts(text.getText()))
                .addOnFailureListener(e -> Toast.makeText(this, "Text recognition failed", Toast.LENGTH_SHORT).show());
    }

    private void compareTexts(String scannedText) {
        String originalText = paragraphText.getText().toString();
        StringBuilder resultBuilder = new StringBuilder();

        // Split into words
        String[] originalWords = originalText.toLowerCase().split("\\s+");
        String[] scannedWords = scannedText.toLowerCase().split("\\s+");

        int totalWords = originalWords.length;
        int correctWords = 0;
        List<String> mistakes = new ArrayList<>();

        // Compare each word
        for (int i = 0; i < originalWords.length && i < scannedWords.length; i++) {
            if (originalWords[i].equals(scannedWords[i])) {
                correctWords++;
            } else {
                mistakes.add(scannedWords[i] + " → " + originalWords[i]);
            }
        }

        // Calculate accuracy
        float accuracy = (float) correctWords / totalWords * 100;

        // Build detailed result
        resultBuilder.append(String.format("Accuracy: %.1f%%\n\n", accuracy));
        resultBuilder.append(String.format("Correct words: %d/%d\n\n", correctWords, totalWords));

        if (!mistakes.isEmpty()) {
            resultBuilder.append("Mistakes found:\n");
            for (String mistake : mistakes) {
                resultBuilder.append("• ").append(mistake).append("\n");
            }
        }

        // Show character-level differences for incorrect words
        if (!mistakes.isEmpty()) {
            resultBuilder.append("\nCharacter-level analysis:\n");
            for (int i = 0; i < originalWords.length && i < scannedWords.length; i++) {
                if (!originalWords[i].equals(scannedWords[i])) {
                    resultBuilder.append("Word: ").append(originalWords[i]).append("\n");
                    resultBuilder.append("Your writing: ").append(scannedWords[i]).append("\n");
                    resultBuilder.append("Differences: ");

                    // Show character differences
                    String word1 = originalWords[i];
                    String word2 = scannedWords[i];
                    int len1 = word1.length();
                    int len2 = word2.length();

                    for (int j = 0; j < Math.max(len1, len2); j++) {
                        char c1 = j < len1 ? word1.charAt(j) : '_';
                        char c2 = j < len2 ? word2.charAt(j) : '_';
                        if (c1 != c2) {
                            resultBuilder.append(c2).append("→").append(c1).append(" ");
                        }
                    }
                    resultBuilder.append("\n\n");
                }
            }
        }

        resultText.setText(resultBuilder.toString());
    }

    private float calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0f;
        return (maxLength - levenshteinDistance(s1, s2)) / (float) maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i][j - 1], dp[i - 1][j]));
                }
            }
        }

        return dp[m][n];
    }
}