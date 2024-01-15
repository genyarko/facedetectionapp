package com.example.facedetection;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;

import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class TextRecognitionUtil {

    private static final String TAG = "TextRecognitionUtil";

    public static void processImageForTextRecognition(Bitmap bitmap, TextView recognizedTextView) {
        Log.d(TAG, "Starting text recognition process...");
        // Create an InputImage object from the Bitmap
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // There seems to be a limit on the amount of language instances created. do not exceed 2.
        // Create a TextRecognizer instance for English text
        com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        // Create a TextRecognizer instance for Chinese text
        com.google.mlkit.vision.text.TextRecognizer recognizerChinese = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        // When using Devanagari script library
        //com.google.mlkit.vision.text.TextRecognizer recognizerDevangari = TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
        // When using Japanese script library
        //com.google.mlkit.vision.text.TextRecognizer recognizerJapanese = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
        // When using Korean script library
        //com.google.mlkit.vision.text.TextRecognizer recognizerKorean = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        // Process the image for Latin text recognition
        Task<Text> result = recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        // Handle the success case for English text
                        Log.d(TAG, "English Text recognition succeeded!");
                        processRecognizedText(visionText, recognizedTextView);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle the failure case for English text
                        Log.e(TAG, "English Text recognition failed", e);
                    }
                });


        // Process the image for Chinese text recognition
        Task<Text> resultChinese = recognizerChinese.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        // Handle the success case for Chinese text
                        Log.d(TAG, "Chinese Text recognition succeeded!");
                        processRecognizedText(visionText, recognizedTextView);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle the failure case for Chinese text
                        Log.e(TAG, "Chinese Text recognition failed", e);
                    }
                });






    }

    private static void processRecognizedText(Text visionText, TextView recognizedTextView) {
        StringBuilder resultText = new StringBuilder();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            resultText.append(block.getText()).append("\n");
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (Text.Element element : line.getElements()) {
                    String elementText = element.getText();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                    for (Text.Symbol symbol : element.getSymbols()) {
                        String symbolText = symbol.getText();
                        Point[] symbolCornerPoints = symbol.getCornerPoints();
                        Rect symbolFrame = symbol.getBoundingBox();
                    }
                }
            }
        }

        // Update the TextView with the recognized text
        recognizedTextView.setText(resultText.toString());
    }
}
