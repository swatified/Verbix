package com.example.verbix;

import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.HashMap;

import com.google.firebase.firestore.FirebaseFirestore;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SpeechPatternActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView paragraphText, resultText, statusText;
    private Button startButton;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_pattern);

        if (checkPermission()) {
            initializeApp();
        } else {
            requestPermission();
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, 1);
    }

    private void initializeApp() {
        initViews();
        setupSpeechRecognizer();
        setupListeners();
        db = FirebaseFirestore.getInstance();
        getRandomParagraph();
    }

    private void initViews() {
        paragraphText = findViewById(R.id.paragraphText);
        resultText = findViewById(R.id.resultText);
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                statusText.setText("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float v) {}

            @Override
            public void onBufferReceived(byte[] bytes) {}

            @Override
            public void onEndOfSpeech() {
                statusText.setText("Processing...");
            }

            @Override
            public void onError(int i) {
                statusText.setText("Error occurred. Try again.");
                startButton.setText("Start Speaking");
                isListening = false;
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    compareTexts(spokenText);
                }
                startButton.setText("Start Speaking");
                statusText.setText("Press button and start speaking");
                isListening = false;
            }

            @Override
            public void onPartialResults(Bundle bundle) {}

            @Override
            public void onEvent(int i, Bundle bundle) {}
        });
    }

    private void setupListeners() {
        startButton.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            } else {
                stopListening();
            }
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.startListening(intent);
        startButton.setText("Stop Listening");
        isListening = true;
    }

    private void stopListening() {
        speechRecognizer.stopListening();
        startButton.setText("Start Speaking");
        statusText.setText("Press button and start speaking");
        isListening = false;
    }

    private void getRandomParagraph() {
        db.collection("practicepara")
                .document("speechpat")
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
                    Toast.makeText(SpeechPatternActivity.this,
                            "Error loading paragraph: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void compareTexts(String spokenText) {
        String originalText = paragraphText.getText().toString();
        StringBuilder resultBuilder = new StringBuilder();

        // Split into words
        String[] originalWords = originalText.toLowerCase().split("\\s+");
        String[] spokenWords = spokenText.toLowerCase().split("\\s+");

        int totalWords = originalWords.length;
        int correctWords = 0;
        List<String> mistakes = new ArrayList<>();
        List<String> phoneticMistakes = new ArrayList<>();

        // Compare words and analyze phonetics
        for (int i = 0; i < Math.min(originalWords.length, spokenWords.length); i++) {
            if (originalWords[i].equals(spokenWords[i])) {
                correctWords++;
            } else {
                mistakes.add(spokenWords[i] + " → " + originalWords[i]);
                analyzePhoneticMistakes(originalWords[i], spokenWords[i], phoneticMistakes);
            }
        }

        // Calculate accuracy
        float accuracy = (float) correctWords / totalWords * 100;

        // Build result
        resultBuilder.append("Accuracy: ").append(String.format("%.1f%%", accuracy))
                .append("\n\n");
        resultBuilder.append("Correct words: ").append(correctWords)
                .append("/").append(totalWords).append("\n\n");

        if (!mistakes.isEmpty()) {
            resultBuilder.append("Word Mistakes:\n");
            for (String mistake : mistakes) {
                String[] parts = mistake.split(" → ");
                // Swap the order and skip if they're the same
                if (!parts[0].equals(parts[1].replace(".", ""))) {
                    resultBuilder.append("• ").append(parts[1].replace(".", ""))
                            .append(" → ").append(parts[0]).append("\n");
                }
            }
            resultBuilder.append("\n");
        }

        if (!phoneticMistakes.isEmpty()) {
            resultBuilder.append("Sound Issues:\n");
            for (String phoneticMistake : phoneticMistakes) {
                resultBuilder.append("• ").append(phoneticMistake).append("\n");
            }
        }

        resultText.setText(resultBuilder.toString());
    }

    private void analyzePhoneticMistakes(String original, String spoken, List<String> phoneticMistakes) {
        Map<String, String> soundPatterns = new HashMap<>();
        // Common pronunciation patterns
        soundPatterns.put("th", "t,d,f");      // 'think' → 'tink'
        soundPatterns.put("r", "w,l");         // 'red' → 'wed'
        soundPatterns.put("l", "r,w");         // 'light' → 'right'
        soundPatterns.put("v", "b,f");         // 'very' → 'berry'
        soundPatterns.put("w", "v");           // 'wait' → 'vait'
        soundPatterns.put("sh", "s");          // 'ship' → 'sip'
        soundPatterns.put("ch", "sh,t");       // 'chair' → 'share'

        // Vowel sounds
        soundPatterns.put("ee", "i");          // 'sheep' → 'ship'
        soundPatterns.put("ea", "ee,i");       // 'beat' → 'bit'
        soundPatterns.put("ai", "ay,e");       // 'rain' → 'ren'

        for (Map.Entry<String, String> pattern : soundPatterns.entrySet()) {
            String sound = pattern.getKey();
            if (original.contains(sound) && !spoken.contains(sound)) {
                String[] alternates = pattern.getValue().split(",");
                for (String alt : alternates) {
                    if (spoken.contains(alt)) {
                        phoneticMistakes.add("Confused '" + sound + "' sound with '" + alt + "'");
                        break;
                    }
                }
            }
        }

        // Check for dropped endings
        if (original.length() > spoken.length() &&
                original.substring(0, spoken.length()).equals(spoken)) {
            phoneticMistakes.add("Dropped ending in '" + original + "'");
        }

        // Check for added sounds
        if (spoken.length() > original.length() &&
                spoken.substring(0, original.length()).equals(original)) {
            phoneticMistakes.add("Added extra sound in '" + spoken + "'");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}