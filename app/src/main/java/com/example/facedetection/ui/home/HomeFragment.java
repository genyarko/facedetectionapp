package com.example.facedetection.ui.home;

import static androidx.camera.core.impl.utils.ContextUtil.getApplicationContext;

import android.Manifest;
import android.content.ContentValues;
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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;

import com.example.facedetection.R;
import com.example.facedetection.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
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

/*
 * HomeFragment: This class represents the main fragment of the application for handling camera functionality
 * and image recognition. It uses the CameraX library for camera integration and Firebase ML Kit for text
 * and face recognition.
 */

public class HomeFragment extends Fragment {

    // Set of required permissions for camera and storage access
    private static final Set<String> REQUIRED_PERMISSIONS = new HashSet<>(Arrays.asList(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
            // Add other permissions as needed
    ));

    // UI elements
    private TextView recognizedTextView, recognizeFaceView;
    private Preview preview;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private static final String TAG = "TextRecognition";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private PreviewView cameraPreview;
    private Button recognizeTextButton;

    // Permission launcher for camera permissions
    private final ActivityResultLauncher<String[]> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean permissionGranted = true;
                        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                            if (Arrays.asList(REQUIRED_PERMISSIONS).contains(entry.getKey()) && !entry.getValue()) {
                                permissionGranted = false;
                            }
                        }
                        if (!permissionGranted) {
                            Toast.makeText(getContext(), "Permission request denied", Toast.LENGTH_SHORT).show();
                        } else {
                            startCamera();
                        }
                    });

    // Launcher for selecting images from the gallery
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            // Check if the selected URI is not null
                            if (uri != null) {
                                // Use the selected URI for text recognition or any other processing
                                recognizeTextFromUri(uri);
                                recognizeFacesFromUri(uri); // Optionally call face recognition
                            } else {
                                // Handle the case when the selected URI is null
                                Log.e(TAG, "Selected URI from gallery is null.");
                            }
                        }
                    });

    private FragmentHomeBinding binding;

    // Fragment's onCreateView method
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // ViewModel setup
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        // Hide the status bar
        requireActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Binding setup
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ImageView previewImage = root.findViewById(R.id.previewImage);
        recognizedTextView = root.findViewById(R.id.recognizedTextView);
        Button cameraButton = root.findViewById(R.id.cameraButton);
        Button choosePicButton = root.findViewById(R.id.choosePic);
        cameraPreview = root.findViewById(R.id.cameraPreview);
        recognizeTextButton = root.findViewById(R.id.recognizeButton);
        recognizeFaceView = root.findViewById(R.id.recognizedFaceView);

        // Request camera and storage permissions
        requestCameraPermission();

        // Set click listener for the camera button
        cameraButton.setOnClickListener(v -> takePhoto());

        // Set click listener for the choosePic button
        choosePicButton.setOnClickListener(v -> choosePictureFromGallery());

        // Hide action bar
        hideActionBar();



        // Set a click listener on the button
        recognizeTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create a Toast with the message "Automatic!"
                Toast toast = Toast.makeText(requireContext(), "Automatic!", Toast.LENGTH_SHORT);

                // Show the Toast
                toast.show();

            }
        });

        return root;
    }

    // Hide action bar on top function
    private void hideActionBar() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    // Camera setup and initialization
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // Capture a photo using the camera
    private void takePhoto() {
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) {
            return;
        }

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = createImageContentValues(name);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                requireContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e(TAG, "Photo capture failed: " + exc.getMessage(), exc);
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = createSavedUri(output);
                        String msg = "Photo capture succeeded: " + savedUri;
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);

                        // Recognize text from the captured image
                        recognizeTextFromUri(savedUri);
                        recognizeFacesFromUri(savedUri);


                    }
                }
        );
    }

    // Request camera permission
    private void requestCameraPermission() {
        cameraPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
    }

    // Choose picture from the gallery
    private void choosePictureFromGallery() {
        galleryLauncher.launch("image/*");
    }

    // Create ContentValues for storing image metadata
    private ContentValues createImageContentValues(String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YourAppName");
        }
        return contentValues;
    }

    // Create URI for the saved image
    private Uri createSavedUri(ImageCapture.OutputFileResults output) {
        return Uri.fromFile(new File(Objects.requireNonNull(Objects.requireNonNull(output.getSavedUri()).getPath())));
    }

    // Recognize text from a given URI using Firebase ML Kit
    private void recognizeTextFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Rotate the bitmap if needed
            int rotationDegree = 0; // You should calculate this based on the device orientation

            InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);

            // Use TextRecognizer for different languages
            TextRecognizer recognizerEnglish = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            // Perform text recognition
            recognizeText(image, recognizerEnglish);


        } catch (IOException e) {
            Log.e(TAG, "Error reading input stream", e);
        }
    }
    private void recognizeFacesFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Rotate the bitmap if needed
            int rotationDegree = 0; // You should calculate this based on the device orientation

            InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);

            // Use FaceDetector for face detection
            FaceDetectorOptions faceDetectorOptions =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .build();

            FaceDetector faceDetector = FaceDetection.getClient(faceDetectorOptions);

            // Perform face detection
            detectFaces(image, faceDetector);

        } catch (IOException e) {
            Log.e(TAG, "Error reading input stream", e);
        }
    }

    // Detect faces in the image using Firebase ML Kit
    private void detectFaces(InputImage image, FaceDetector faceDetector) {
        // Process the detected faces
        faceDetector.process(image)
                .addOnSuccessListener(this::processDetectedFaces)
                .addOnFailureListener(e -> Log.e(TAG, "Face detection failed", e));
    }

    // Recognize text in the image using Firebase ML Kit
    private void recognizeText(InputImage image, TextRecognizer recognizer) {
        recognizer.process(image)
                .addOnSuccessListener(this::processRecognizedText)
                .addOnFailureListener(e -> Log.e(TAG, "Text recognition failed", e));
    }

    // Process recognized text from Firebase ML Kit
    private void processRecognizedText(Text visionText) {
        // Extract and handle recognized text as needed
        String resultText = visionText.getText();
        recognizedTextView.setText(resultText);

        // Iterate through blocks, lines, elements, and symbols if needed
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            String blockText = block.getText();
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
    }

    // Process detected faces from Firebase ML Kit
    private void processDetectedFaces(List<Face> faces) {
        if (faces.isEmpty()) {
            Log.d(TAG, "No faces detected");
            return;
        }

        StringBuilder classificationResult = new StringBuilder();

        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
            float rotY = face.getHeadEulerAngleY();
            float rotZ = face.getHeadEulerAngleZ();

            // Landmark detection
            for (int landmarkType : Arrays.asList(
                    FaceLandmark.LEFT_EYE,
                    FaceLandmark.RIGHT_EYE,
                    FaceLandmark.MOUTH_RIGHT,
                    FaceLandmark.MOUTH_LEFT,
                    FaceLandmark.MOUTH_BOTTOM,
                    FaceLandmark.NOSE_BASE,
                    FaceLandmark.LEFT_CHEEK,
                    FaceLandmark.RIGHT_CHEEK,
                    FaceLandmark.LEFT_EAR,
                    FaceLandmark.RIGHT_EAR
            )) {
                FaceLandmark landmark = face.getLandmark(landmarkType);
                if (landmark != null) {
                    PointF landmarkPos = landmark.getPosition();
                    String landmarkName = getLandmarkName(landmarkType);
                    classificationResult.append(landmarkName)
                            .append(" Position: ")
                            .append(landmarkPos)
                            .append("\n");
                }
            }

            // Classification
            if (face.getSmilingProbability() != null) {
                float smileProb = face.getSmilingProbability();
                classificationResult.append("Smile Probability: ").append(smileProb).append("\n");
            }

            if (face.getRightEyeOpenProbability() != null) {
                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                classificationResult.append("Right Eye Open Probability: ").append(rightEyeOpenProb).append("\n");
            }
            if (face.getLeftEyeOpenProbability() != null) {
                float leftEyeOpenProb = face.getLeftEyeOpenProbability();
                classificationResult.append("Right Eye Open Probability: ").append(leftEyeOpenProb).append("\n");
            }

            // Continue processing other face features or perform additional actions as needed
        }

        // Update the recognizedFaceView with the classification result
        recognizeFaceView.setText(classificationResult.toString());
    }

    // Helper method to get landmark name based on landmark type
    private String getLandmarkName(int landmarkType) {
        switch (landmarkType) {
            case FaceLandmark.LEFT_EYE:
                return "Left Eye";
            case FaceLandmark.RIGHT_EYE:
                return "Right Eye";
            case FaceLandmark.MOUTH_LEFT:
                return "Left Mouth";
            case FaceLandmark.MOUTH_RIGHT:
                return "Right Mouth";
            case FaceLandmark.NOSE_BASE:
                return "Nose Base";
            case FaceLandmark.LEFT_CHEEK:
                return "Left Cheek";
            case FaceLandmark.RIGHT_CHEEK:
                return "Right Cheek";
            case FaceLandmark.LEFT_EAR:
                return "Left Ear";
            case FaceLandmark.RIGHT_EAR:
                return "Right Ear";
            case FaceLandmark.MOUTH_BOTTOM:
                    return "Mouth bottom";
            default:
                return "Unknown Landmark";
        }
    }


}
