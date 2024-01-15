package com.example.facedetection.ui.dashboard;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;

import com.example.facedetection.DetectLabelUtil;
import com.example.facedetection.LiveLabelingUtil;
import com.example.facedetection.PoseDetectionUtil;
import com.example.facedetection.R;
import com.example.facedetection.databinding.FragmentDashboardBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * DashboardFragment: This class represents the fragment responsible for the dashboard view, which includes
 * live object detection using the device camera. It utilizes CameraX for camera functionality and a custom
 * LiveLabelingUtil for live object labeling.
 */

public class DashboardFragment extends Fragment {

    // Binding for the fragment
    private static FragmentDashboardBinding binding;

    // Object detector for live labeling
    private ObjectDetector objectDetector;

    // Executor service for camera operations
    private ExecutorService cameraExecutor;

    // Utility for live labeling
    private LiveLabelingUtil liveLabelingUtil;

    // TextView for displaying detected labels
    TextView detectedLabel;

    // Camera selector for choosing the back camera
    private final CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build();

    // Utility for detecting labels in still images
    private DetectLabelUtil detectLabelUtil;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout using data binding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set the activity to full-screen mode
        requireActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Initialize background executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize LiveLabelingUtil for live labeling
        liveLabelingUtil = new LiveLabelingUtil(requireContext());
        detectedLabel = binding.detetectedLabel;

        // Setup the camera
        setupCamera();

        // Initialize DetectLabelUtil for still image labeling
        detectLabelUtil = new DetectLabelUtil();

        // Use detectLabelUtil.LABELS as needed
        String[] labels = DetectLabelUtil.LABELS;
        for (String label : labels) {
            // Do something with each label
            System.out.println(label);
        }

        // Hide the action bar
        hideActionBar();

        return root;
    }


    // Setup camera for live object detection
    private void setupCamera() {
        PreviewView previewView = binding.previewView;

        // Create a future for obtaining the camera provider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        // Store the lifecycle owner reference
        LifecycleOwner lifecycleOwner = this;

        // Declare analyzer as final to use in the listener
        final ImageAnalysis.Analyzer[] analyzer = {null};

        // Add a listener to the camera provider future
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                if (cameraProvider != null) {
                    // Start live labeling
                    analyzer[0] = new ImageAnalysis.Analyzer() {
                        @Override
                        public void analyze(@NonNull ImageProxy imageProxy) {
                            // Implement your image analysis logic here
                            if (analyzer[0] != null) {
                                liveLabelingUtil.processImage(imageProxy, analyzer[0], detectedLabel);
                            }
                        }
                    };

                    // Start live labeling using the LiveLabelingUtil
                    liveLabelingUtil.startLiveLabeling(previewView, analyzer[0], lifecycleOwner);
                }
            } catch (Exception e) {
                // Handle exceptions
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // Method to update the classificationTextView
    public static void updateClassificationTextView(String label) {
        if (binding != null && isFragmentAttached()) {
            TextView classificationTextView = binding.classificationTextView;
            classificationTextView.setText(label);
        } else {
            Log.e(TAG, "Binding or classificationTextView is null or fragment is not attached");
        }
    }

    // Helper method to check if the fragment is attached to an activity
    private static boolean isFragmentAttached() {
        return binding != null && binding.getRoot().getContext() != null;
    }



    // Bind the preview and other camera use cases
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        PreviewView previewView = binding.previewView;

        // Create Preview use case
        Preview preview = new Preview.Builder().build();

        // Create ImageAnalysis use case for object detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Set an analyzer for the ImageAnalysis use case
        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                // Implement your image analysis logic here
            }
        });

        // Bind the camera use cases
        Camera camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalysis);

        // Set the surface provider for the preview
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    // Hide the action bar
    private void hideActionBar() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    // Cleanup resources on fragment destruction
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        cameraExecutor.shutdown();
    }
}

