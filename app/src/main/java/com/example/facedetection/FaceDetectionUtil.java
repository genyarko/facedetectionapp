package com.example.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceDetectionUtil {

    private static final String TAG = "FaceDetectionUtil";

    public interface FaceDetectionListener {
        void onFaceDetected(List<Face> faces);

        void onFaceDetectionFailed(Exception e);
    }

    public static void detectFaces(Context context, Bitmap bitmap, FaceDetectionListener listener) {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        //.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setMinFaceSize(0.1f)
                        .enableTracking()
                        .build();

        FaceDetector faceDetector = FaceDetection.getClient(options);

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        listener.onFaceDetected(faces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onFaceDetectionFailed(e);
                    }
                });
    }
}

