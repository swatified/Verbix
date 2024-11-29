package com.example.verbix;

import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.HashMap;

import com.google.firebase.firestore.FirebaseFirestore;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

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
        SpannableStringBuilder fullTextSpan = new SpannableStringBuilder();

        String[] originalWords = originalText.toLowerCase().split("\\s+");
        String[] spokenWords = spokenText.toLowerCase().split("\\s+");

        // Basic statistics
        int totalWords = originalWords.length;
        int correctWords = 0;
        List<String> wordMistakes = new ArrayList<>();
        Map<String, Integer> generalPatterns = new HashMap<>();
        List<String> phoneticMistakes = new ArrayList<>();

        // Dyslexia-specific patterns
        Map<String, Integer> soundConfusions = new HashMap<>();
        Map<String, Integer> syllablePatterns = new HashMap<>();
        Map<String, Integer> endingPatterns = new HashMap<>();

        // Compare words and collect patterns
        for (int i = 0; i < Math.min(originalWords.length, spokenWords.length); i++) {
            String original = originalWords[i];
            String spoken = spokenWords[i];

            if (original.equals(spoken)) {
                correctWords++;
            } else {
                wordMistakes.add(spoken + " → " + original);

                // General pattern analysis
                analyzeGeneralPatterns(original, spoken, generalPatterns);
                analyzePhoneticMistakes(original, spoken, phoneticMistakes);

                // Dyslexia-specific analysis
                analyzeSoundConfusions(original, spoken, soundConfusions);
                analyzeSyllablePatterns(original, spoken, syllablePatterns);
                analyzeEndingPatterns(original, spoken, endingPatterns);
            }
        }

        // Build comprehensive analysis
        float accuracy = (float) correctWords / totalWords * 100;
        StringBuilder analysis = new StringBuilder();

        // Basic Statistics
        SpannableString overallHeader = new SpannableString("Overall Analysis");
        overallHeader.setSpan(new StyleSpan(Typeface.BOLD), 0, overallHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        overallHeader.setSpan(new RelativeSizeSpan(1.5f), 0, overallHeader.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        analysis.append(overallHeader);
        analysis.append("\n\n");

        analysis.append(String.format("Accuracy: %.1f%%\n", accuracy));
        analysis.append(String.format("Correct words: %d/%d\n\n", correctWords, totalWords));

        // General Patterns
        if (!generalPatterns.isEmpty()) {
            analysis.append("Common Patterns:\n");
            generalPatterns.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(e -> analysis.append("• ").append(e.getKey())
                            .append(" (").append(e.getValue()).append(" times)\n"));
            analysis.append("\n");
        }

        // Word Mistakes
        if (!wordMistakes.isEmpty()) {
            analysis.append("Word Mistakes:\n");
            wordMistakes.forEach(mistake -> analysis.append("• ").append(mistake).append("\n"));
            analysis.append("\n");
        }

        // Phonetic Analysis
        if (!phoneticMistakes.isEmpty()) {
            analysis.append("Sound Issues:\n");
            phoneticMistakes.forEach(mistake -> analysis.append("• ").append(mistake).append("\n"));
            analysis.append("\n");
        }

        // Dyslexia-Specific Analysis
        SpannableString header = new SpannableString("Speech Pattern Analysis");
        header.setSpan(new StyleSpan(Typeface.BOLD), 0, header.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        header.setSpan(new RelativeSizeSpan(1.5f), 0, header.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        analysis.append(header);
        analysis.append("\n\n");

        if (!soundConfusions.isEmpty()) {
            analysis.append("Sound Confusions:\n");
            soundConfusions.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(e -> analysis.append("• ").append(e.getKey()).append("\n"));
        }

        if (!syllablePatterns.isEmpty()) {
            analysis.append("\nSyllable Patterns:\n");
            syllablePatterns.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(e -> analysis.append("• ").append(e.getKey()).append("\n"));
        }

        if (!endingPatterns.isEmpty()) {
            analysis.append("\nWord Ending Patterns:\n");
            endingPatterns.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(e -> analysis.append("• ").append(e.getKey()).append("\n"));
        }

        resultText.setText(analysis.toString());
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

        // Check dropped endings
        if (original.length() > spoken.length() &&
                original.substring(0, spoken.length()).equals(spoken)) {
            phoneticMistakes.add("Dropped ending in '" + original + "'");
        }

        // Check added sounds
        if (spoken.length() > original.length() &&
                spoken.substring(0, original.length()).equals(original)) {
            phoneticMistakes.add("Added extra sound in '" + spoken + "'");
        }
    }

    private void analyzeGeneralPatterns(String original, String spoken, Map<String, Integer> patterns) {
        // Word length differences
        if (Math.abs(original.length() - spoken.length()) > 2) {
            String pattern = original.length() > spoken.length() ?
                    "Shortening words" : "Adding extra sounds";
            patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
        }

        // Repeated sounds
        if (containsRepeatedSounds(spoken) && !containsRepeatedSounds(original)) {
            patterns.put("Adding repeated sounds", patterns.getOrDefault("Adding repeated sounds", 0) + 1);
        }

        // Initial sound mistakes
        if (!spoken.isEmpty() && !original.isEmpty() && spoken.charAt(0) != original.charAt(0)) {
            patterns.put("Changing initial sounds",
                    patterns.getOrDefault("Changing initial sounds", 0) + 1);
        }
    }

    private void analyzeSoundConfusions(String original, String spoken,
                                        Map<String, Integer> confusions) {
        Map<String, String[]> commonConfusions = new HashMap<>();
        commonConfusions.put("th", new String[]{"f", "t", "d"});  // think → fink
        commonConfusions.put("r", new String[]{"w", "l"});        // red → wed
        commonConfusions.put("s", new String[]{"th", "f"});       // sink → think
        commonConfusions.put("ch", new String[]{"sh", "t"});      // chair → share
        commonConfusions.put("v", new String[]{"b", "f"});        // very → berry

        for (Map.Entry<String, String[]> entry : commonConfusions.entrySet()) {
            String target = entry.getKey();
            if (original.contains(target)) {
                for (String confusion : entry.getValue()) {
                    if (spoken.contains(confusion)) {
                        String key = "Replacing '" + target + "' with '" + confusion + "'";
                        confusions.put(key, confusions.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }
    }

    private void analyzeSyllablePatterns(String original, String spoken,
                                         Map<String, Integer> patterns) {
        // Count syllables (simplified method)
        int originalSyllables = countSyllables(original);
        int spokenSyllables = countSyllables(spoken);

        if (originalSyllables != spokenSyllables) {
            String pattern = originalSyllables > spokenSyllables ?
                    "Dropping syllables" : "Adding syllables";
            patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
        }
    }

    private void analyzeEndingPatterns(String original, String spoken,
                                       Map<String, Integer> patterns) {
        String[] commonEndings = {"ing", "ed", "s", "es", "ly"};

        for (String ending : commonEndings) {
            if (original.endsWith(ending) && !spoken.endsWith(ending)) {
                String pattern = "Missing '" + ending + "' ending";
                patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
            }
            if (!original.endsWith(ending) && spoken.endsWith(ending)) {
                String pattern = "Adding extra '" + ending + "' ending";
                patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
            }
        }
    }

    private boolean containsRepeatedSounds(String word) {
        for (int i = 0; i < word.length() - 1; i++) {
            if (word.charAt(i) == word.charAt(i + 1)) return true;
        }
        return false;
    }

    private int countSyllables(String word) {
        word = word.toLowerCase();
        int count = 0;
        boolean isPreviousVowel = false;
        for (int i = 0; i < word.length(); i++) {
            boolean isVowel = "aeiou".indexOf(word.charAt(i)) != -1;
            if (isVowel && !isPreviousVowel) count++;
            isPreviousVowel = isVowel;
        }
        return Math.max(1, count);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}