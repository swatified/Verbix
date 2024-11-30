package com.example.verbix;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;
import android.graphics.Typeface;
import java.util.function.Function;
import android.animation.ObjectAnimator;
import android.view.View;

public class WritingPatternActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView paragraphText, resultText;
    private ImageView scannedImage;
    private FrameLayout scanButton;
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
        scanButton = findViewById(R.id.scanContainer);
        ImageView cameraIcon = findViewById(R.id.cameraIcon);
        scannedImage.setVisibility(View.GONE);
        cameraIcon.setVisibility(View.VISIBLE);
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

            // Add visibility logic here
            ImageView cameraIcon = findViewById(R.id.cameraIcon);
            scannedImage.setImageBitmap(imageBitmap);
            scannedImage.setVisibility(View.VISIBLE);
            cameraIcon.setVisibility(View.GONE);

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
        SpannableStringBuilder analysis = new SpannableStringBuilder();

        // Color-coded comparison
        String[] originalWords = originalText.split("\\s+");
        String[] scannedWords = scannedText.split("\\s+");
        addColorComparison(originalWords, scannedWords, analysis);
        analysis.append("\n\n");

        // Calculate accuracy
        int totalWords = originalWords.length;
        int correctWords = countCorrectWords(originalWords, scannedWords);
        float accuracy = (float) correctWords / totalWords * 100;

        // Maps for analysis
        Map<String, Integer> letterConfusions = new HashMap<>();
        Map<String, Integer> troubleLetters = new HashMap<>();
        Map<String, Integer> missingLetters = new HashMap<>();
        Map<String, Integer> extraLetters = new HashMap<>();
        Map<String, Integer> caseMistakes = new HashMap<>();
        Map<String, Integer> reversalPatterns = new HashMap<>();
        Map<String, Integer> soundPatterns = new HashMap<>();
        Map<String, Integer> sequenceErrors = new HashMap<>();
        Map<String, Integer> visualSimilarity = new HashMap<>();

        // Analyze patterns
        analyzeMistakes(originalWords, scannedWords, letterConfusions, troubleLetters,
                missingLetters, extraLetters, caseMistakes, reversalPatterns,
                soundPatterns, sequenceErrors, visualSimilarity);

        // Overall Analysis
        analysis.append(String.format("Accuracy: %.1f%%\n", accuracy));
        analysis.append(String.format("Correct words: %d/%d\n\n", correctWords, totalWords));

        // Basic Writing Analysis
        addStyledHeader(analysis, "Basic Writing Analysis", true);
        addSection(analysis, letterConfusions, "Letter Confusions:",
                e -> formatMistakeEntry(e, " times"));
        addSection(analysis, missingLetters, "Missing Letters:",
                e -> formatMistakeEntry(e, " times"));
        addSection(analysis, extraLetters, "Extra Letters:",
                e -> formatMistakeEntry(e, " times"));
        addSection(analysis, caseMistakes, "Case Mistakes:",
                e -> formatMistakeEntry(e, " times"));
        addSection(analysis, troubleLetters, "Trouble Letters:",
                e -> String.format("• Having trouble with '%s' (%d mistakes)", e.getKey(), e.getValue()));

        // Dyslexia Analysis
        addStyledHeader(analysis, "Dyslexia-Specific Analysis", true);
        addSection(analysis, reversalPatterns, "Letter Reversals:",
                e -> "• " + e.getKey());
        addSection(analysis, soundPatterns, "Sound Patterns:",
                e -> "• " + e.getKey());
        addSection(analysis, sequenceErrors, "Sequence Issues:",
                e -> "• " + e.getKey());
        addSection(analysis, visualSimilarity, "Visual Similarities:",
                e -> "• " + e.getKey());

        resultText.setText(analysis);
    }

    private void addColorComparison(String[] originalWords, String[] scannedWords,
                                    SpannableStringBuilder builder) {
        for (int i = 0; i < Math.min(originalWords.length, scannedWords.length); i++) {
            SpannableString wordSpan = new SpannableString(scannedWords[i]);
            int minLength = Math.min(originalWords[i].length(), scannedWords[i].length());

            for (int j = 0; j < scannedWords[i].length(); j++) {
                int color = (j < minLength && originalWords[i].charAt(j) == scannedWords[i].charAt(j))
                        ? Color.parseColor("#228B22") : Color.parseColor("#DC143C");
                wordSpan.setSpan(new ForegroundColorSpan(color), j, j + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            builder.append(wordSpan).append(" ");
        }
    }

    private void addStyledHeader(SpannableStringBuilder builder, String text, boolean large) {
        SpannableString header = new SpannableString(text);
        header.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (large) {
            header.setSpan(new RelativeSizeSpan(1.5f), 0, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            header.setSpan(new UnderlineSpan(), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(header).append("\n\n");
        } else {
            builder.append(header).append("\n");
        }
    }

    private void addSection(SpannableStringBuilder builder, Map<String, Integer> data,
                            String title, Function<Map.Entry<String, Integer>, String> formatter) {
        if (!data.isEmpty()) {
            addStyledHeader(builder, title, false);
            data.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(3)
                    .forEach(e -> builder.append(formatter.apply(e)).append("\n"));
            builder.append("\n");
        }
    }

    private String formatMistakeEntry(Map.Entry<String, Integer> entry, String suffix) {
        return String.format("• %s (%d%s)", entry.getKey(), entry.getValue(), suffix);
    }

    private int countCorrectWords(String[] original, String[] scanned) {
        int count = 0;
        for (int i = 0; i < Math.min(original.length, scanned.length); i++) {
            if (original[i].equals(scanned[i])) count++;
        }
        return count;
    }

    private void analyzeMistakes(String[] originalWords, String[] scannedWords,
                                 Map<String, Integer> letterConfusions, Map<String, Integer> troubleLetters,
                                 Map<String, Integer> missingLetters, Map<String, Integer> extraLetters,
                                 Map<String, Integer> caseMistakes, Map<String, Integer> reversalPatterns,
                                 Map<String, Integer> soundPatterns, Map<String, Integer> sequenceErrors,
                                 Map<String, Integer> visualSimilarity) {

        for (int i = 0; i < Math.min(originalWords.length, scannedWords.length); i++) {
            String original = originalWords[i];
            String scanned = scannedWords[i];
            String originalLower = original.toLowerCase();
            String scannedLower = scanned.toLowerCase();

            checkBasicMistakes(original, scanned, originalLower, scannedLower,
                    letterConfusions, troubleLetters, missingLetters, extraLetters, caseMistakes);
            checkReversals(originalLower, scannedLower, reversalPatterns);
            checkPhoneticPatterns(originalLower, scannedLower, soundPatterns);
            checkSequenceErrors(originalLower, scannedLower, sequenceErrors);
            checkVisualSimilarities(originalLower, scannedLower, visualSimilarity);
        }
    }

    private void checkBasicMistakes(String original, String scanned,
                                    String originalLower, String scannedLower,
                                    Map<String, Integer> letterConfusions, Map<String, Integer> troubleLetters,
                                    Map<String, Integer> missingLetters, Map<String, Integer> extraLetters,
                                    Map<String, Integer> caseMistakes) {

        // Case mistakes
        for (int j = 0; j < Math.min(original.length(), scanned.length()); j++) {
            if (originalLower.charAt(j) == scannedLower.charAt(j) &&
                    original.charAt(j) != scanned.charAt(j)) {
                caseMistakes.put(String.valueOf(original.charAt(j)),
                        caseMistakes.getOrDefault(String.valueOf(original.charAt(j)), 0) + 1);
            }
        }

        // Missing letters
        for (char c : original.toCharArray()) {
            if (scanned.indexOf(c) == -1) {
                missingLetters.put(String.valueOf(c),
                        missingLetters.getOrDefault(String.valueOf(c), 0) + 1);
            }
        }

        // Extra letters
        for (char c : scanned.toCharArray()) {
            if (original.indexOf(c) == -1) {
                extraLetters.put(String.valueOf(c),
                        extraLetters.getOrDefault(String.valueOf(c), 0) + 1);
            }
        }

        // Letter confusions
        int len = Math.min(original.length(), scanned.length());
        for (int j = 0; j < len; j++) {
            if (originalLower.charAt(j) != scannedLower.charAt(j)) {
                String confusion = originalLower.charAt(j) + " with " + scannedLower.charAt(j);
                letterConfusions.put(confusion, letterConfusions.getOrDefault(confusion, 0) + 1);
                troubleLetters.put(String.valueOf(originalLower.charAt(j)),
                        troubleLetters.getOrDefault(String.valueOf(originalLower.charAt(j)), 0) + 1);
            }
        }
    }

    private void checkReversals(String original, String scanned, Map<String, Integer> patterns) {
        String[][] commonReversals = {
                {"b", "d"}, {"p", "q"}, {"n", "u"}, {"m", "w"},
                {"g", "q"}, {"f", "t"}, {"6", "9"}, {"2", "5"}
        };

        for (String[] pair : commonReversals) {
            if (original.contains(pair[0]) && scanned.contains(pair[1]) ||
                    original.contains(pair[1]) && scanned.contains(pair[0])) {
                String key = "Reversing " + pair[0] + "/" + pair[1];
                patterns.put(key, patterns.getOrDefault(key, 0) + 1);
            }
        }
    }

    private void checkPhoneticPatterns(String original, String scanned, Map<String, Integer> patterns) {
        Map<String, String[]> phoneticGroups = new HashMap<>();
        phoneticGroups.put("f", new String[]{"ph", "v"});
        phoneticGroups.put("s", new String[]{"c", "z"});
        phoneticGroups.put("k", new String[]{"c", "q"});
        phoneticGroups.put("j", new String[]{"g", "ch"});
        phoneticGroups.put("ee", new String[]{"ea", "ie"});
        phoneticGroups.put("ai", new String[]{"ay", "ei"});

        for (Map.Entry<String, String[]> group : phoneticGroups.entrySet()) {
            for (String sound : group.getValue()) {
                if ((original.contains(group.getKey()) && scanned.contains(sound)) ||
                        (original.contains(sound) && scanned.contains(group.getKey()))) {
                    String key = "Confusing " + group.getKey() + " sound with " + sound;
                    patterns.put(key, patterns.getOrDefault(key, 0) + 1);
                }
            }
        }
    }

    private void checkSequenceErrors(String original, String scanned, Map<String, Integer> patterns) {
        for (int i = 0; i < original.length() - 1 && i < scanned.length() - 1; i++) {
            if (i + 1 < original.length() && i + 1 < scanned.length()) {
                String origPair = original.substring(i, i + 2);
                String scannedPair = scanned.substring(i, i + 2);
                if (origPair.equals(new StringBuilder(scannedPair).reverse().toString())) {
                    String key = "Swapping " + origPair;
                    patterns.put(key, patterns.getOrDefault(key, 0) + 1);
                }
            }
        }
    }

    private void checkVisualSimilarities(String original, String scanned, Map<String, Integer> patterns) {
        Map<Character, char[]> visuallySimilar = new HashMap<>();
        visuallySimilar.put('h', new char[]{'n'});
        visuallySimilar.put('m', new char[]{'n'});
        visuallySimilar.put('i', new char[]{'l', '1'});
        visuallySimilar.put('o', new char[]{'0', 'a'});
        visuallySimilar.put('s', new char[]{'5'});
        visuallySimilar.put('z', new char[]{'2'});

        for (Map.Entry<Character, char[]> entry : visuallySimilar.entrySet()) {
            for (char similar : entry.getValue()) {
                if ((original.indexOf(entry.getKey()) != -1 && scanned.indexOf(similar) != -1) ||
                        (original.indexOf(similar) != -1 && scanned.indexOf(entry.getKey()) != -1)) {
                    String key = "Confusing visually similar " + entry.getKey() + " with " + similar;
                    patterns.put(key, patterns.getOrDefault(key, 0) + 1);
                }
            }
        }
    }
}