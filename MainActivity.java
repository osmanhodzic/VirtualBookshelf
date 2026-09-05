package com.example.virtualbookshelf;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {


    private ImageView bookshelfBackground;

    private FloatingActionButton btnAdd;

    private Uri cameraImageUri;


    // Lista knjiga

    private final ArrayList<BookInfo> books =
            new ArrayList<>();


    // SharedPreferences

    private SharedPreferences preferences;


    private static final String PREFS_NAME =
            "VirtualBookshelfPrefs";


    private static final String BOOKS_KEY =
            "books";


    // KLasa koja predstavlja jednu knjigu

    private static class BookInfo {

        String imagePath;

        float x;

        float y;


        BookInfo(
                String imagePath,
                float x,
                float y
        ) {

            this.imagePath = imagePath;

            this.x = x;

            this.y = y;
        }
    }


    // =========================================================
    // GALERIJA
    // =========================================================

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null) {

                            openCropActivity(uri);
                        }
                    }
            );


    // =========================================================
    // KAMERA
    // =========================================================

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode()
                                == RESULT_OK
                                && cameraImageUri != null) {

                            openCropActivity(
                                    cameraImageUri
                            );
                        }
                    }
            );


    // =========================================================
    // DOZVOLA ZA KAMERU
    // =========================================================

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {

                        if (granted) {

                            openCamera();

                        } else {

                            Toast.makeText(
                                    this,
                                    "Potrebna je dozvola za kameru.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );


    // =========================================================
    // CROP RESULT
    // =========================================================

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode()
                                == RESULT_OK
                                && result.getData() != null) {


                            String imagePath =
                                    result.getData()
                                            .getStringExtra(
                                                    "croppedImagePath"
                                            );


                            if (imagePath != null) {

                                addBookToShelf(
                                        imagePath
                                );
                            }
                        }
                    }
            );


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        setContentView(
                R.layout.activity_main
        );


        bookshelfBackground =
                findViewById(
                        R.id.bookshelfBackground
                );


        btnAdd =
                findViewById(
                        R.id.btnAdd
                );


        // SharedPreferences

        preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );


        btnAdd.setOnClickListener(
                v -> showAddMenu()
        );


        // UČITAJ STARE KNJIGE

        loadBooks();
    }


    // =========================================================
    // MENI ZA DODAVANJE
    // =========================================================

    private void showAddMenu() {

        PopupMenu popupMenu =
                new PopupMenu(
                        this,
                        btnAdd
                );


        popupMenu.getMenu().add(
                "📁 Galerija"
        );


        popupMenu.getMenu().add(
                "📷 Kamera"
        );


        popupMenu.getMenu().add(
                "🪵 Drvena polica"
        );


        popupMenu.getMenu().add(
                "⬜ Bijela polica"
        );


        popupMenu.getMenu().add(
                "⬛ Crna polica"
        );


        popupMenu.setOnMenuItemClickListener(
                item -> {

                    String selected =
                            item.getTitle()
                                    .toString();


                    switch (selected) {


                        case "📁 Galerija":

                            openGallery();

                            break;


                        case "📷 Kamera":

                            checkCameraPermission();

                            break;


                        case "🪵 Drvena polica":

                            bookshelfBackground.setImageResource(
                                    R.drawable.shelf_wood
                            );

                            break;


                        case "⬜ Bijela polica":

                            bookshelfBackground.setImageResource(
                                    R.drawable.shelf_white
                            );

                            break;


                        case "⬛ Crna polica":

                            bookshelfBackground.setImageResource(
                                    R.drawable.shelf_black
                            );

                            break;
                    }


                    return true;
                }
        );


        popupMenu.show();
    }


    // =========================================================
    // GALERIJA
    // =========================================================

    private void openGallery() {

        galleryLauncher.launch(
                "image/*"
        );
    }


    // =========================================================
    // PROVJERA DOZVOLE
    // =========================================================

    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {


            openCamera();


        } else {

            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }


    // =========================================================
    // OTVARANJE KAMERE
    // =========================================================

    private void openCamera() {

        try {


            File imageFile =
                    File.createTempFile(
                            "book_",
                            ".jpg",
                            getCacheDir()
                    );


            cameraImageUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            imageFile
                    );


            Intent cameraIntent =
                    new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE
                    );


            cameraIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    cameraImageUri
            );


            cameraIntent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION
            );


            cameraLauncher.launch(
                    cameraIntent
            );


        } catch (IOException e) {

            e.printStackTrace();


            Toast.makeText(
                    this,
                    "Kamera se ne može otvoriti.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // OTVORI CROP
    // =========================================================

    private void openCropActivity(
            Uri uri
    ) {

        Intent intent =
                new Intent(
                        MainActivity.this,
                        CropBookActivity.class
                );


        intent.putExtra(
                "imageUri",
                uri
        );


        cropLauncher.launch(
                intent
        );
    }


    // =========================================================
    // DODAVANJE KNJIGE
    // =========================================================

    private void addBookToShelf(
            String imagePath
    ) {


        File imageFile =
                new File(
                        imagePath
                );


        if (!imageFile.exists()) {

            Toast.makeText(
                    this,
                    "Slika knjige ne postoji.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        ImageView book =
                new ImageView(this);


        Uri imageUri =
                Uri.fromFile(
                        imageFile
                );


        book.setImageURI(
                imageUri
        );


        book.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );


        book.setBackgroundColor(
                Color.TRANSPARENT
        );


        // VELIČINA KNJIGE

        int width = 80;

        int height = 240;


        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        width,
                        height
                );


        params.gravity =
                Gravity.BOTTOM
                        | Gravity.START;


        // Nova knjiga ide desno od prethodne

        params.leftMargin =
                30
                        + (books.size() * 90);


        params.bottomMargin =
                100;


        book.setLayoutParams(
                params
        );


        book.setElevation(
                10
        );


        FrameLayout container =
                findViewById(
                        R.id.bookshelfContainer
                );


        container.addView(
                book
        );


        // Podatak o novoj knjizi

        BookInfo bookInfo =
                new BookInfo(
                        imagePath,
                        book.getX(),
                        book.getY()
                );


        books.add(
                bookInfo
        );


        // Omogućava pomjeranje knjige

        setupBookTouch(
                book,
                bookInfo
        );


        // Nakon što se knjiga prikaže,
        // zapamti njenu stvarnu poziciju

        book.post(
                () -> {

                    bookInfo.x =
                            book.getX();

                    bookInfo.y =
                            book.getY();

                    saveBooks();
                }
        );
    }


    // =========================================================
    // POSTAVLJANJE TOUCH-A NA KNJIGU
    // =========================================================

    private void setupBookTouch(
            ImageView book,
            BookInfo bookInfo
    ) {


        book.setOnTouchListener(
                new View.OnTouchListener() {


                    float dX;

                    float dY;

                    long downTime;


                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {


                        switch (
                                event.getActionMasked()
                        ) {


                            case MotionEvent.ACTION_DOWN:


                                downTime =
                                        System.currentTimeMillis();


                                dX =
                                        view.getX()
                                                - event.getRawX();


                                dY =
                                        view.getY()
                                                - event.getRawY();


                                return true;


                            case MotionEvent.ACTION_MOVE:


                                view.setX(
                                        event.getRawX()
                                                + dX
                                );


                                view.setY(
                                        event.getRawY()
                                                + dY
                                );


                                return true;


                            case MotionEvent.ACTION_UP:


                                long duration =
                                        System.currentTimeMillis()
                                                - downTime;


                                // Dug pritisak
                                // = brisanje

                                if (duration >= 600) {


                                    showDeleteMenu(
                                            view,
                                            bookInfo
                                    );


                                } else {


                                    // Kratki pritisak
                                    // završava pomjeranje

                                    bookInfo.x =
                                            view.getX();


                                    bookInfo.y =
                                            view.getY();


                                    saveBooks();
                                }


                                return true;
                        }


                        return true;
                    }
                }
        );
    }


    // =========================================================
    // BRISANJE KNJIGE - MENI
    // =========================================================

    private void showDeleteMenu(
            View book,
            BookInfo bookInfo
    ) {


        PopupMenu popupMenu =
                new PopupMenu(
                        this,
                        book
                );


        popupMenu.getMenu().add(
                "🗑 Obriši knjigu"
        );


        popupMenu.setOnMenuItemClickListener(
                item -> {


                    if (item.getTitle()
                            .toString()
                            .equals(
                                    "🗑 Obriši knjigu"
                            )) {


                        deleteBook(
                                book,
                                bookInfo
                        );
                    }


                    return true;
                }
        );


        popupMenu.show();
    }


    // =========================================================
    // BRISANJE KNJIGE
    // =========================================================

    private void deleteBook(
            View book,
            BookInfo bookInfo
    ) {


        FrameLayout container =
                findViewById(
                        R.id.bookshelfContainer
                );


        // Ukloni sa ekrana

        container.removeView(
                book
        );


        // Ukloni iz liste

        books.remove(
                bookInfo
        );


        // Obriši fizičku sliku

        try {

            File imageFile =
                    new File(
                            bookInfo.imagePath
                    );


            if (imageFile.exists()) {

                imageFile.delete();
            }

        } catch (Exception e) {

            e.printStackTrace();
        }


        // Sačuvaj novu listu

        saveBooks();


        Toast.makeText(
                this,
                "Knjiga je obrisana.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // ČUVANJE KNJIGA
    // =========================================================

    private void saveBooks() {

        try {


            JSONArray jsonArray =
                    new JSONArray();


            for (
                    BookInfo book :
                    books
            ) {


                JSONObject object =
                        new JSONObject();


                object.put(
                        "imagePath",
                        book.imagePath
                );


                object.put(
                        "x",
                        book.x
                );


                object.put(
                        "y",
                        book.y
                );


                jsonArray.put(
                        object
                );
            }


            preferences
                    .edit()
                    .putString(
                            BOOKS_KEY,
                            jsonArray.toString()
                    )
                    .apply();


        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // UČITAVANJE KNJIGA
    // =========================================================

    private void loadBooks() {

        String savedBooks =
                preferences.getString(
                        BOOKS_KEY,
                        null
                );


        if (savedBooks == null
                || savedBooks.isEmpty()) {

            return;
        }


        try {


            JSONArray jsonArray =
                    new JSONArray(
                            savedBooks
                    );


            for (
                    int i = 0;
                    i < jsonArray.length();
                    i++
            ) {


                JSONObject object =
                        jsonArray.getJSONObject(
                                i
                        );


                String imagePath =
                        object.getString(
                                "imagePath"
                        );


                float x =
                        (float) object.getDouble(
                                "x"
                        );


                float y =
                        (float) object.getDouble(
                                "y"
                        );


                File imageFile =
                        new File(
                                imagePath
                        );


                // Ako slika još postoji,
                // učitaj knjigu

                if (imageFile.exists()) {


                    BookInfo bookInfo =
                            new BookInfo(
                                    imagePath,
                                    x,
                                    y
                            );


                    books.add(
                            bookInfo
                    );


                    createBookView(
                            bookInfo
                    );
                }
            }


        } catch (Exception e) {

            e.printStackTrace();


            Toast.makeText(
                    this,
                    "Greška pri učitavanju knjiga.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // KREIRANJE KNJIGE PRILIKOM UČITAVANJA
    // =========================================================

    private void createBookView(
            BookInfo bookInfo
    ) {


        ImageView book =
                new ImageView(this);


        File imageFile =
                new File(
                        bookInfo.imagePath
                );


        Uri imageUri =
                Uri.fromFile(
                        imageFile
                );


        book.setImageURI(
                imageUri
        );


        book.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );


        book.setBackgroundColor(
                Color.TRANSPARENT
        );


        int width = 80;

        int height = 240;


        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        width,
                        height
                );


        params.gravity =
                Gravity.TOP
                        | Gravity.START;


        book.setLayoutParams(
                params
        );


        book.setElevation(
                10
        );


        FrameLayout container =
                findViewById(
                        R.id.bookshelfContainer
                );


        container.addView(
                book
        );


        // Postavi spremljenu poziciju

        book.post(
                () -> {

                    book.setX(
                            bookInfo.x
                    );


                    book.setY(
                            bookInfo.y
                    );
                }
        );


        setupBookTouch(
                book,
                bookInfo
        );
    }
}