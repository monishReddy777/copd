package com.simats.cdss;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.simats.cdss.views.CircularCropOverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageCropActivity extends AppCompatActivity {

    private ImageView ivCropImage;
    private CircularCropOverlay cropOverlay;
    private Bitmap originalBitmap;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private float startX, startY;
    private ScaleGestureDetector scaleDetector;
    private float currentScale = 1f;
    private float minScale = 0.5f;
    private float maxScale = 5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        ivCropImage = findViewById(R.id.iv_crop_image);
        cropOverlay = findViewById(R.id.crop_overlay);

        findViewById(R.id.iv_back).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Rotation
        findViewById(R.id.iv_rotate).setOnClickListener(v -> rotateImage());

        // Load image from intent URI
        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString != null) {
            try {
                Uri imageUri = Uri.parse(uriString);
                InputStream in = getContentResolver().openInputStream(imageUri);
                originalBitmap = BitmapFactory.decodeStream(in);
                if (in != null) in.close();

                if (originalBitmap != null) {
                    ivCropImage.setImageBitmap(originalBitmap);
                    ivCropImage.post(() -> centerAndFillImage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        // Touch events go through the overlay to the image
        cropOverlay.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    startX = event.getX();
                    startY = event.getY();
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ZOOM;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        float dx = event.getX() - startX;
                        float dy = event.getY() - startY;
                        matrix.postTranslate(dx, dy);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }

            ivCropImage.setImageMatrix(matrix);
            return true;
        });

        // Save cropped/adjusted image
        findViewById(R.id.btn_save_crop).setOnClickListener(v -> saveCroppedImage());
    }

    /**
     * Centers and scales the image to fill the crop circle area,
     * so the circular crop area is fully covered by default.
     */
    private void centerAndFillImage() {
        if (originalBitmap == null) return;
        float viewW = ivCropImage.getWidth();
        float viewH = ivCropImage.getHeight();
        float bmpW = originalBitmap.getWidth();
        float bmpH = originalBitmap.getHeight();

        // Scale to fill the view (use max so image covers the circle)
        float scale = Math.max(viewW / bmpW, viewH / bmpH);
        currentScale = scale;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate((viewW - bmpW * scale) / 2f, (viewH - bmpH * scale) / 2f);
        ivCropImage.setImageMatrix(matrix);
    }

    /**
     * Rotates the image visually by 90 degrees clockwise around the center
     */
    private void rotateImage() {
        if (originalBitmap == null) return;
        
        float cx = ivCropImage.getWidth() / 2f;
        float cy = ivCropImage.getHeight() / 2f;
        
        // Rotate the matrix 90 degrees clockwise
        matrix.postRotate(90f, cx, cy);
        ivCropImage.setImageMatrix(matrix);
    }

    /**
     * Saves the visible portion within the circular crop area as a round profile image.
     */
    private void saveCroppedImage() {
        try {
            // Capture what's visible in the ImageView
            int w = ivCropImage.getWidth();
            int h = ivCropImage.getHeight();

            Bitmap viewBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(viewBitmap);
            ivCropImage.draw(canvas);

            // Get the circle dimensions from the overlay
            float cx = w / 2f;
            float cy = h / 2f;
            int radius = cropOverlay.getCircleRadius();
            if (radius <= 0) {
                radius = (int) (Math.min(w, h) * 0.42f);
            }

            // Crop to the square bounding the circle
            int cropX = (int) Math.max(0, cx - radius);
            int cropY = (int) Math.max(0, cy - radius);
            int cropSize = Math.min(radius * 2, Math.min(w - cropX, h - cropY));

            Bitmap squareBitmap = Bitmap.createBitmap(viewBitmap, cropX, cropY, cropSize, cropSize);

            // Scale to 256x256 for storage efficiency
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 256, 256, true);

            // Create circular bitmap
            Bitmap circularBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
            Canvas circleCanvas = new Canvas(circularBitmap);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Path circlePath = new Path();
            circlePath.addCircle(128f, 128f, 128f, Path.Direction.CW);
            circleCanvas.clipPath(circlePath);
            circleCanvas.drawBitmap(scaledBitmap, 0, 0, paint);

            // Save to internal storage as PNG to preserve transparency
            File file = new File(getFilesDir(), "profile_image.jpg");
            FileOutputStream out = new FileOutputStream(file);
            circularBitmap.compress(Bitmap.CompressFormat.PNG, 95, out);
            out.flush();
            out.close();

            // Clean up
            if (squareBitmap != viewBitmap) squareBitmap.recycle();
            viewBitmap.recycle();
            scaledBitmap.recycle();

            Uri savedUri = Uri.fromFile(file);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("cropped_uri", savedUri.toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;

            if (newScale >= minScale && newScale <= maxScale) {
                currentScale = newScale;
                matrix.postScale(scaleFactor, scaleFactor,
                        detector.getFocusX(), detector.getFocusY());
                ivCropImage.setImageMatrix(matrix);
            }
            return true;
        }
    }
}
