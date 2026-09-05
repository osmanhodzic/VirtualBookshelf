package com.example.virtualbookshelf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CropBookActivity extends AppCompatActivity {

    private ImageView imageToCrop;

    private Uri imageUri;

    private Bitmap originalBitmap;

    private Matrix imageMatrix;

    private ScaleGestureDetector scaleDetector;

    private float lastX;
    private float lastY;

    private float scaleFactor = 1.0f;

    private boolean moving = false;

    private final float MIN_SCALE = 0.5f;
    private final float MAX_SCALE = 5.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_crop_book);


        imageToCrop =
                findViewById(R.id.imageToCrop);

        Button btnCrop =
                findViewById(R.id.btnCrop);

        Button btnRotate =
                findViewById(R.id.btnRotate);


        // DOBIJANJE URI-JA

        if (Build.VERSION.SDK_INT >= 33) {

            imageUri =
                    getIntent().getParcelableExtra(
                            "imageUri",
                            Uri.class
                    );

        } else {

            imageUri =
                    getIntent().getParcelableExtra(
                            "imageUri"
                    );
        }


        if (imageUri == null) {

            Toast.makeText(
                    this,
                    "Greška pri učitavanju slike.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }


        // UČITAVANJE SLIKE

        loadImage();


        // ZOOM

        scaleDetector =
                new ScaleGestureDetector(
                        this,
                        new ScaleListener()
                );


        imageToCrop.setOnTouchListener(
                (view, event) -> {

                    scaleDetector.onTouchEvent(event);


                    switch (event.getActionMasked()) {

                        case MotionEvent.ACTION_DOWN:

                            lastX =
                                    event.getX();

                            lastY =
                                    event.getY();

                            moving = true;

                            return true;


                        case MotionEvent.ACTION_MOVE:

                            // Pomjeranje slike
                            // ako nije pinch zoom

                            if (!scaleDetector.isInProgress()
                                    && moving) {

                                float dx =
                                        event.getX()
                                                - lastX;

                                float dy =
                                        event.getY()
                                                - lastY;


                                imageMatrix.postTranslate(
                                        dx,
                                        dy
                                );


                                imageToCrop.setImageMatrix(
                                        imageMatrix
                                );


                                lastX =
                                        event.getX();

                                lastY =
                                        event.getY();
                            }

                            return true;


                        case MotionEvent.ACTION_UP:

                        case MotionEvent.ACTION_CANCEL:

                            moving = false;

                            return true;
                    }


                    return true;
                }
        );


        // ROTIRAJ

        btnRotate.setOnClickListener(
                v -> rotateImage()
        );


        // IZREŽI

        btnCrop.setOnClickListener(
                v -> cropImage()
        );
    }


    // UČITAVANJE SLIKE

    private void loadImage() {

        try {

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(imageUri);


            originalBitmap =
                    BitmapFactory.decodeStream(
                            inputStream
                    );


            if (inputStream != null) {

                inputStream.close();
            }


            if (originalBitmap == null) {

                Toast.makeText(
                        this,
                        "Slika se ne može učitati.",
                        Toast.LENGTH_SHORT
                ).show();

                finish();

                return;
            }


            imageMatrix =
                    new Matrix();


            imageToCrop.post(
                    () -> setupImage()
            );


        } catch (Exception e) {

            e.printStackTrace();


            Toast.makeText(
                    this,
                    "Greška pri učitavanju slike.",
                    Toast.LENGTH_SHORT
            ).show();


            finish();
        }
    }


    // POČETNO POSTAVLJANJE SLIKE

    private void setupImage() {

        float viewWidth =
                imageToCrop.getWidth();

        float viewHeight =
                imageToCrop.getHeight();


        float bitmapWidth =
                originalBitmap.getWidth();

        float bitmapHeight =
                originalBitmap.getHeight();


        float scaleX =
                viewWidth / bitmapWidth;

        float scaleY =
                viewHeight / bitmapHeight;


        float initialScale =
                Math.min(
                        scaleX,
                        scaleY
                );


        scaleFactor =
                initialScale;


        float scaledWidth =
                bitmapWidth * initialScale;

        float scaledHeight =
                bitmapHeight * initialScale;


        float dx =
                (viewWidth - scaledWidth) / 2;

        float dy =
                (viewHeight - scaledHeight) / 2;


        imageMatrix.reset();


        imageMatrix.postScale(
                initialScale,
                initialScale
        );


        imageMatrix.postTranslate(
                dx,
                dy
        );


        imageToCrop.setImageBitmap(
                originalBitmap
        );


        imageToCrop.setImageMatrix(
                imageMatrix
        );
    }


    // ROTIRANJE SLIKE ZA 90 STEPENI

    private void rotateImage() {

        if (originalBitmap == null) {

            return;
        }


        try {

            Matrix rotationMatrix =
                    new Matrix();


            rotationMatrix.postRotate(
                    90
            );


            Bitmap rotatedBitmap =
                    Bitmap.createBitmap(
                            originalBitmap,
                            0,
                            0,
                            originalBitmap.getWidth(),
                            originalBitmap.getHeight(),
                            rotationMatrix,
                            true
                    );


            // Stara slika više nije potrebna

            if (originalBitmap != rotatedBitmap) {

                originalBitmap.recycle();
            }


            originalBitmap =
                    rotatedBitmap;


            imageMatrix =
                    new Matrix();


            imageToCrop.setImageBitmap(
                    originalBitmap
            );


            // Ponovo centriraj sliku

            imageToCrop.post(
                    () -> setupImage()
            );


        } catch (Exception e) {

            e.printStackTrace();


            Toast.makeText(
                    this,
                    "Greška pri rotiranju slike.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // PINCH ZOOM

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {


        @Override
        public boolean onScale(
                ScaleGestureDetector detector
        ) {

            float factor =
                    detector.getScaleFactor();


            float newScale =
                    scaleFactor * factor;


            if (newScale < MIN_SCALE) {

                factor =
                        MIN_SCALE / scaleFactor;

                newScale =
                        MIN_SCALE;
            }


            if (newScale > MAX_SCALE) {

                factor =
                        MAX_SCALE / scaleFactor;

                newScale =
                        MAX_SCALE;
            }


            imageMatrix.postScale(
                    factor,
                    factor,
                    detector.getFocusX(),
                    detector.getFocusY()
            );


            scaleFactor =
                    newScale;


            imageToCrop.setImageMatrix(
                    imageMatrix
            );


            return true;
        }
    }


    // CROP

    private void cropImage() {

        try {

            float density =
                    getResources()
                            .getDisplayMetrics()
                            .density;


            float cropWidth =
                    100 * density;

            float cropHeight =
                    300 * density;


            float viewWidth =
                    imageToCrop.getWidth();

            float viewHeight =
                    imageToCrop.getHeight();


            float cropLeft =
                    (viewWidth - cropWidth) / 2;

            float cropTop =
                    (viewHeight - cropHeight) / 2;


            float cropRight =
                    cropLeft + cropWidth;

            float cropBottom =
                    cropTop + cropHeight;


            RectF cropRect =
                    new RectF(
                            cropLeft,
                            cropTop,
                            cropRight,
                            cropBottom
                    );


            Matrix inverseMatrix =
                    new Matrix();


            imageMatrix.invert(
                    inverseMatrix
            );


            RectF bitmapRect =
                    new RectF(cropRect);


            inverseMatrix.mapRect(
                    bitmapRect
            );


            int left =
                    Math.max(
                            0,
                            Math.round(bitmapRect.left)
                    );


            int top =
                    Math.max(
                            0,
                            Math.round(bitmapRect.top)
                    );


            int right =
                    Math.min(
                            originalBitmap.getWidth(),
                            Math.round(bitmapRect.right)
                    );


            int bottom =
                    Math.min(
                            originalBitmap.getHeight(),
                            Math.round(bitmapRect.bottom)
                    );


            int width =
                    right - left;

            int height =
                    bottom - top;


            if (width <= 0 || height <= 0) {

                Toast.makeText(
                        this,
                        "Povećaj ili pomjeri sliku.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            Bitmap croppedBitmap =
                    Bitmap.createBitmap(
                            originalBitmap,
                            left,
                            top,
                            width,
                            height
                    );


            String path =
                    saveBitmap(
                            croppedBitmap
                    );


            if (path != null) {

                Intent resultIntent =
                        new Intent();


                resultIntent.putExtra(
                        "croppedImagePath",
                        path
                );


                setResult(
                        RESULT_OK,
                        resultIntent
                );


                finish();
            }


        } catch (Exception e) {

            e.printStackTrace();


            Toast.makeText(
                    this,
                    "Greška pri izrezivanju.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // TRAJNO ČUVANJE SLIKE

    private String saveBitmap(
            Bitmap bitmap
    ) {

        try {

            // Napravi folder books
            // unutar interne memorije aplikacije

            File booksDirectory =
                    new File(
                            getFilesDir(),
                            "books"
                    );


            if (!booksDirectory.exists()) {

                boolean created =
                        booksDirectory.mkdirs();


                if (!created) {

                    Toast.makeText(
                            this,
                            "Folder za knjige se ne može napraviti.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return null;
                }
            }


            File file =
                    new File(
                            booksDirectory,
                            "book_" +
                                    System.currentTimeMillis() +
                                    ".jpg"
                    );


            FileOutputStream outputStream =
                    new FileOutputStream(file);


            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    outputStream
            );


            outputStream.flush();

            outputStream.close();


            return file.getAbsolutePath();


        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }
}