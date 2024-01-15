package com.example.facedetection;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.example.facedetection.ui.dashboard.DashboardFragment;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.widget.TextView;

public class LiveLabelingUtil {

    private final String TAG = "LiveLabelingUtil";
    private final Context context;
    private final ExecutorService cameraExecutor;
    private ImageLabeler imageLabeler;
    private CameraSelector cameraSelector;

    public LiveLabelingUtil(Context context) {
        this.context = context;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        initImageLabeler();
        initCameraSelector();
    }

    private void initImageLabeler() {
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.65f)
                .build();
        imageLabeler = ImageLabeling.getClient(options);
    }

    private void initCameraSelector() {
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
    }

    public void startLiveLabeling(PreviewView preview, ImageAnalysis.Analyzer analyzer, LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                if (cameraProvider != null) {
                    bindPreview(cameraProvider, preview, analyzer, lifecycleOwner);
                } else {
                    Log.e(TAG, "Error: Camera provider is null");
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting camera provider: " + e.getMessage(), e);

                if (e.getCause() instanceof MlKitException) {
                    // Handle ML Kit initialization error
                } else {
                    // Handle other errors
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider, PreviewView previewView, ImageAnalysis.Analyzer analyzer, LifecycleOwner lifecycleOwner) {
        Preview preview = new Preview.Builder().build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer);

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);

        // Set the surface provider to the PreviewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }




    public void stopLiveLabeling() {
        cameraExecutor.shutdown();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    public void processImage(ImageProxy imageProxy, ImageAnalysis.Analyzer analyzer, TextView detectedLabelTextView) {
        // Check if the ImageProxy is still valid
        if (imageProxy == null || imageProxy.getImage() == null) {
            return;
        }
        InputImage image = InputImage.fromMediaImage(
                Objects.requireNonNull(imageProxy.getImage()),
                imageProxy.getImageInfo().getRotationDegrees());

        imageLabeler.process(image)
                .addOnSuccessListener(labels -> {
                    for (ImageLabel label : labels) {
                        String text = label.getText();
                        String detectedLabel = label.getText();
                        // Handle label information as needed
                        // You can update the UI here or take other actions based on labels
                        DashboardFragment.updateClassificationTextView(text);
                        // Check if the detected label matches any label in DetectLabelUtil
                        if (Arrays.asList(DetectLabelUtil.LABELS).contains(detectedLabel)) {
                            // Update the TextView with the detected label
                            updateDetectedLabelTextView(detectedLabel, detectedLabelTextView);
                        }
                    }
                    // Close the ImageProxy after processing
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    // Handle image labeling failure
                    // Close the ImageProxy in case of failure
                    imageProxy.close();
                });
    }

    private void updateDetectedLabelTextView(String label, TextView detectedLabelTextView) {
        // Update the TextView with the detected label
        detectedLabelTextView.setText(label);
    }


    public boolean arePermissionsGranted() {
        // Check if camera and write external storage permissions are granted
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
}
