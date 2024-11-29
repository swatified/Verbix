package com.example.verbix;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.content.Intent;
import android.graphics.Color;
import android.speech.RecognitionListener;
import android.text.SpannableString;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import java.util.Map;
import java.util.Random;

public class TrainSpeechActivity extends AppCompatActivity {
    private TextView wordTextView;
    private Button speakButton;
    private TextView highlightedTextView;
    private SpeechRecognizer speechRecognizer;
    private FirebaseFirestore db;
    private Button tryAnotherButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_speech);

        wordTextView = findViewById(R.id.wordTextView);
        highlightedTextView = findViewById(R.id.highlightedTextView);
        speakButton = findViewById(R.id.speakButton);
        db = FirebaseFirestore.getInstance();
        tryAnotherButton = findViewById(R.id.tryAnotherButton);


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Called when the speech recognizer is ready to start listening
            }

            @Override
            public void onBeginningOfSpeech() {
                // Called when the user starts speaking
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Called when the volume of the speech input changes
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Called when more sound has been received
            }

            @Override
            public void onEndOfSpeech() {
                // Called when the user stops speaking
            }

            @Override
            public void onError(int error) {
                // Called when an error occurs during speech recognition
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    compareAndHighlight(spokenText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Called when partial recognition results are available
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Called when a recognition event occurs
            }
        });

        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognition();
            }
        });

        tryAnotherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWordFromFirestore(); // Fetch a new random word from Firestore
            }
        });

        getWordFromFirestore();
    }




    private void getWordFromFirestore() {
        db.collection("practicepara")
                .document("words")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get all fields from the document
                        Map<String, Object> fields = documentSnapshot.getData();
                        if (fields != null && !fields.isEmpty()) {
                            // Convert field values to a list
                            List<String> words = new ArrayList<>();
                            for (Object value : fields.values()) {
                                words.add(value.toString());
                            }
                            // Select a random word
                            String randomWord = words.get(new Random().nextInt(words.size()));
                            wordTextView.setText(randomWord);
                        } else {
                            Log.e("Firestore", "No words found in the document");
                        }
                    } else {
                        Log.e("Firestore", "Document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting document", e);
                });
    }



    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }

    private void compareAndHighlight(String spokenText) {
        String targetWord = wordTextView.getText().toString();  // Keep original case
        spokenText = spokenText.substring(0, 1).toUpperCase() + spokenText.substring(1).toLowerCase();

        SpannableString spannableString = new SpannableString(spokenText);

        for (int i = 0; i < spokenText.length(); i++) {
            if (i < targetWord.length() && spokenText.charAt(i) == targetWord.charAt(i)) {
                spannableString.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#228B22")),
                        i, i + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                spannableString.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#DC143C")),
                        i, i + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        highlightedTextView.setText(spannableString);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }
}