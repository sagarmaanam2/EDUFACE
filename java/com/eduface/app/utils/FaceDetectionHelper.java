package com.eduface.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

/**
 * Helper class for face detection using ML Kit
 */
public class FaceDetectionHelper implements ImageAnalysis.Analyzer {

    private static final String TAG = "FaceDetectionHelper";
    
    private final FaceDetector faceDetector;
    private final FaceDetectionListener listener;
    
    /**
     * Interface for face detection callbacks
     */
    public interface FaceDetectionListener {
        void onFaceDetected(Face face, Rect boundingBox, float confidence);
        void onFaceDetectionFailed(Exception e);
        void onNoFaceDetected();
    }
    
    /**
     * Constructor with FaceDetectionListener
     */
    public FaceDetectionHelper(Context context, FaceDetectionListener listener) {
        this.listener = listener;
        
        // Configure face detector options
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f) // Minimum face size 15% of the image
                .enableTracking()
                .build();
        
        // Create the face detector
        faceDetector = FaceDetection.getClient(options);
    }
    
    /**
     * Process bitmap for face detection
     */
    public void processBitmap(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        detectFaces(image);
    }
    
    /**
     * Process image from camera for face detection
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        // Convert the ImageProxy to InputImage for ML Kit
        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());
        
        // Process the image for face detection
        detectFaces(inputImage, imageProxy);
    }
    
    /**
     * Detect faces in the input image
     */
    private void detectFaces(InputImage image) {
        faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.isEmpty()) {
                            // No faces detected
                            listener.onNoFaceDetected();
                            return;
                        }
                        
                        // Get the first face (assuming one person)
                        Face face = faces.get(0);

                        // Calculate confidence based on tracking ID existence
                        // ML Kit 16.1.5 doesn't have getTrackingConfidence(), so we use a fixed confidence
                        float confidence = face.getTrackingId() != null ? 0.9f : 0.8f;
                        
                        // Get bounding box
                        Rect boundingBox = face.getBoundingBox();
                        
                        // Notify listener
                        listener.onFaceDetected(face, boundingBox, confidence);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Face detection failed: " + e.getMessage());
                        listener.onFaceDetectionFailed(e);
                    }
                });
    }
    
    /**
     * Detect faces in the input image (with ImageProxy closing)
     */
    private void detectFaces(InputImage image, final ImageProxy imageProxy) {
        faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.isEmpty()) {
                            // No faces detected
                            listener.onNoFaceDetected();
                        } else {
                            // Get the first face (assuming one person)
                            Face face = faces.get(0);
                            
                            // Calculate confidence (using tracking confidence if available)
                            // Calculate confidence based on tracking ID existence
                            // ML Kit 16.1.5 doesn't have getTrackingConfidence(), so we use a fixed confidence
                            float confidence = face.getTrackingId() != null ? 0.9f : 0.8f;
                            
                            // Get bounding box
                            Rect boundingBox = face.getBoundingBox();
                            
                            // Notify listener
                            listener.onFaceDetected(face, boundingBox, confidence);
                        }
                        
                        // Close the imageProxy to release resources
                        imageProxy.close();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Face detection failed: " + e.getMessage());
                        listener.onFaceDetectionFailed(e);
                        
                        // Close the imageProxy to release resources
                        imageProxy.close();
                    }
                });
    }
    
    /**
     * Close and release resources
     */
    public void shutdown() {
        faceDetector.close();
    }
}