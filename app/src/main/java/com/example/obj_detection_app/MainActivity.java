package com.example.obj_detection_app;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

//import com.example.obj_detection_app.ml.AndroidFigurine;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button captureBtn;
    private ImageView inputImgView;
    private ImageView imgSample1;
    private ImageView imgSample2;
    private ImageView imgSample3;
    private TextView tvPlaceHolder;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initID();
        initAction();


    }
    private void initID() {
        captureBtn = findViewById(R.id.capture_btn);
        inputImgView = findViewById(R.id.imageViewID);
        imgSample1 = findViewById(R.id.sampleImg1);
        imgSample2 = findViewById(R.id.sampleImg2);
        imgSample3 = findViewById(R.id.sampleImg3);
        tvPlaceHolder = findViewById(R.id.textViewPlaceHolder);
    }
    private void initAction() {
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dispatchTakePicIntent(); }
        });
        imgSample1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    setViewAndDetect(getSampleImage(R.drawable.android_bigbox_case));
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        imgSample2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    setViewAndDetect(getSampleImage(R.drawable.kite));
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        imgSample3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    setViewAndDetect(getSampleImage(R.drawable.bugdroid_android_figurine));
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
    }

    ActivityResultLauncher<Intent> startForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result != null && result.getResultCode() == RESULT_OK) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        try {
                            runObjectDetection(imageBitmap);
                            // Display captured image
//                            inputImgView.setImageBitmap(imageBitmap);
                            tvPlaceHolder.setVisibility(View.VISIBLE);
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void dispatchTakePicIntent() {
        Intent takePicIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startForResult.launch(takePicIntent);
    }

    private Bitmap getSampleImage(int resId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        return  BitmapFactory.decodeResource(getResources(), resId, options);
    }

    private void setViewAndDetect(Bitmap bitmap) throws IOException {
        // Display captured image
        inputImgView.setImageBitmap(bitmap);
        tvPlaceHolder.setVisibility(View.VISIBLE);

        // Run Obj-Detection and Display result
        // Note: run this in background thread to avoid blocking the app's UI because TFLite obj-det is a synchronized process
        runObjectDetection(bitmap);
    }

    private void runObjectDetection(Bitmap bitmap) throws IOException {
        TensorImage image = TensorImage.fromBitmap(bitmap); // Follow this https://youtu.be/yV9nrRIC_R0?t=127 to add model into system

        // Initialize Detector obj
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(10)
                .setScoreThreshold(0.2f)    // show if result > Confident score
                .build();
        ObjectDetector detector = ObjectDetector.createFromFileAndOptions(
                this,   // application context
                "android-figurine.tflite",  // must be same as filename in assets folder
                options
        );

        List<Detection> results = detector.detect(image);

        // result
        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Use to draw
        Canvas canvas = new Canvas(outputBitmap);
        Paint pen = new Paint();
        pen.setTextAlign(Paint.Align.LEFT);

        for (Detection result : results) {
            pen.setColor(Color.RED);
            pen.setStrokeWidth(8F);
            pen.setStyle(Paint.Style.STROKE);
            RectF box = result.getBoundingBox();
            canvas.drawRect(box, pen);

            Rect tagSize = new Rect(0, 0, 0, 0);

            // Calculate the right font size
            pen.setStyle(Paint.Style.FILL_AND_STROKE);
            pen.setColor(Color.RED);
            pen.setStrokeWidth(2F);

            // Which category? java? tf? I chose tensorflow since result.getCategories which result is Detection obj from tf
            Category category = result.getCategories().get(0);
            String text = category.getLabel() + " " + Math.round(category.getScore()*100) + "%";

            pen.setTextSize(96F);
            pen.getTextBounds(text, 0, text.length(), tagSize);

            canvas.drawText(text, box.left, box.top, pen);
        }

        // Display result
        inputImgView.setImageBitmap(outputBitmap);
    }

}