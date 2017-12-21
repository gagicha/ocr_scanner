package com.example.gagicha.camera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;



public class MainActivity extends AppCompatActivity {

    private Button camera;
    private Button crop;
    private ImageView imageview;
    private DragRectView view;
    private static final String IMAGE_DIRECTORY = "/demonuts";
    private int GALLERY = 1, CAMERA = 2, PIC_CROP=3;
    private static final int REQUEST_READ_PERMISSION = 1;
    private static final int REQUEST_WRITE_PERMISSION = 2;
    private Uri imageURI;

    private Uri scanURI;
    private TextView scanResults;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";
    private TextRecognizer detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = (Button) findViewById(R.id.camera);
        imageview = (ImageView) findViewById(R.id.iv);
        view = (DragRectView) findViewById(R.id.dragRect);

        scanResults = (TextView) findViewById(R.id.results);

        if (savedInstanceState != null) {
            scanURI = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }
        detector = new TextRecognizer.Builder(getApplicationContext()).build();

        if (null != view) {
            view.setOnUpCallback(new DragRectView.OnUpCallback() {
                @Override
                public void onRectFinished(final Rect rect) {
//                    Toast.makeText(getApplicationContext(), "Rect is (" + rect.left + ", " + rect.top + ",  "+ rect.height() +" , "+ rect.width() +")",
//                            Toast.LENGTH_LONG).show();
                crop_image(rect.left, rect.top, rect.width(), rect.height());
                }
            });

        }
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPictureDialog();
            }
        });
    }

    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select photo from gallery",
                "Capture photo from camera" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                ActivityCompat.requestPermissions(MainActivity.this, new
                                        String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_PERMISSION);
                                break;
                            case 1:
                                ActivityCompat.requestPermissions(MainActivity.this, new
                                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    choosePhotoFromGallery();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhotoFromCamera();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                imageURI = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageURI);
 //                   String path = saveImage(bitmap);
//                    Toast.makeText(MainActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
                    imageview.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        } else if (requestCode == CAMERA) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            imageview.setImageBitmap(thumbnail);
            saveImage(thumbnail);
            Toast.makeText(MainActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }
        else if(requestCode == PIC_CROP) {
            Bundle extras = data.getExtras();
//get the cropped bitmap
            Bitmap thePic = extras.getParcelable("data");
            //retrieve a reference to the ImageView

//display the returned cropped image
            imageview.setImageBitmap(thePic);
        }
        }


    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    private void crop_image(int left, int top, int width, int height) {
//        left <= right and top <= bottom
//load bitmap from imageview
        Bitmap bitmap = ((BitmapDrawable) imageview.getDrawable()).getBitmap();
// Resize the bitmap to imageview width and height
        Bitmap bMapScaled = Bitmap.createScaledBitmap(bitmap, imageview.getMeasuredWidth(), imageview.getMeasuredHeight(), true);
//crop the image using createbitmap method
        Bitmap bitmapcrop = Bitmap.createBitmap(bMapScaled, left, top, width, height);
//        imageview.setImageBitmap(bitmapcrop);

        try {
            if (detector.isOperational() && bitmapcrop != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmapcrop).build();
                SparseArray<TextBlock> textBlocks = detector.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
//                    for (Text line : tBlock.getComponents()) {
//                        //extract scanned text lines here
//                        lines = lines + line.getValue() + "\n";
//                        for (Text element : line.getComponents()) {
//                            //extract scanned text words here
//                            words = words + element.getValue() + ", ";
//                        }
//                    }
                }
                if (textBlocks.size() == 0) {
                    scanResults.setText("Scan Failed: Found nothing to scan");
                } else {
//                    scanResults.setText(scanResults.getText() + "Blocks: " + "\n");
                    scanResults.setText(scanResults.getText() + blocks + "\n");
//                    scanResults.setText(scanResults.getText() + "---------" + "\n");
//                    scanResults.setText(scanResults.getText() + "Lines: " + "\n");
//                    scanResults.setText(scanResults.getText() + lines + "\n");
//                    scanResults.setText(scanResults.getText() + "---------" + "\n");
//                    scanResults.setText(scanResults.getText() + "Words: " + "\n");
//                    scanResults.setText(scanResults.getText() + words + "\n");
//                    scanResults.setText(scanResults.getText() + "---------" + "\n");
                }
            } else {
                scanResults.setText("Could not set up the detector!");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                    .show();
//        Log.e(LOG_TAG, e.toString());
        }
    }
    }


