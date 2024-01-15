package com.example.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PoseDetectionUtil {

    private static final String TAG = "PoseDetection";
    private final Context context;
    private final PoseDetector poseDetector;
    private PoseDetectionListener poseDetectionListener;

    public interface PoseDetectionListener {
        void onPoseDetected(Pose pose);
    }

    public PoseDetectionUtil(Context context, PoseDetectionListener listener) {
        this.context = context;
        this.poseDetectionListener = listener;

        // Initialize PoseDetector
        PoseDetectorOptions options =
                new PoseDetectorOptions.Builder()
                        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                        .build();
        poseDetector = PoseDetection.getClient(options);
    }

    public void processImage(Uri imageUri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Rotate the bitmap based on the device orientation
            int rotationDegree = getRotationDegree(imageUri);
            bitmap = rotateBitmap(bitmap, rotationDegree);

            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // Perform pose detection
            detectPose(image);

        } catch (IOException e) {
            Log.e(TAG, "Error reading input stream", e);
            // Notify the listener about the error
            if (poseDetectionListener != null) {
                poseDetectionListener.onPoseDetected(null);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
        }
    }

    private void detectPose(InputImage image) {
        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    // Notify the listener about the detected pose
                    if (poseDetectionListener != null) {
                        poseDetectionListener.onPoseDetected(pose);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Pose detection failed", e);
                    // Notify the listener about the error
                    if (poseDetectionListener != null) {
                        poseDetectionListener.onPoseDetected(null);
                    }
                });
    }

    private int getRotationDegree(Uri imageUri) throws IOException {
        ExifInterface exifInterface = new ExifInterface(imageUri.getPath());
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);

            try {
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle();
                }
                return rotatedBitmap;
            } catch (OutOfMemoryError e) {
                e.printStackTrace(); // Handle the error as needed
            }
        }
        return bitmap;
    }

    public void releaseResources() {
        // Release any resources held by PoseDetector or PoseDetectionUtil
        if (poseDetector != null) {
            poseDetector.close();
        }
    }

}





