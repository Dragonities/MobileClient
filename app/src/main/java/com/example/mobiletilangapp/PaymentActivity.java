package com.example.mobiletilangapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1;

    private ImageView imageViewPreview;
    private Bitmap selectedImageBitmap;
    private String username;
    private String violationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        imageViewPreview = findViewById(R.id.imageViewPreview);
        Button buttonUpload = findViewById(R.id.buttonUpload);

        // Get username and index from intent
        Intent intent = getIntent();
        if (intent != null) {
            username = intent.getStringExtra("username");
            int index = intent.getIntExtra("index", -1); // Default value -1 if index is not found
            violationCode = getViolationCode(index);

            // Check if violationCode is null
            if (!violationCode.isEmpty()) {
                // Set text to TextView displaying violation code
                TextView textViewViolationCode = findViewById(R.id.textViewViolationCode);
                textViewViolationCode.setText("Violation Code: " + violationCode);
            } else {
                // Handle case where violation code is empty
                Toast.makeText(this, "Violation code is empty", Toast.LENGTH_SHORT).show();
                // Optionally, you can finish() this activity or take appropriate action
            }
        } else {
            // Handle case where intent is null
            Toast.makeText(this, "Intent is null", Toast.LENGTH_SHORT).show();
            // Optionally, you can finish() this activity or take appropriate action
        }


        // Set click listener for upload button
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });
    }

    // Method to open image picker (gallery)
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // Handle result from image picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            try {
                // Convert URI to Bitmap
                Uri selectedImageUri = data.getData();
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                // Display selected image in ImageView
                imageViewPreview.setImageBitmap(selectedImageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to submit payment with selected image
    // Method untuk submit payment dengan gambar yang dipilih
    public void submitPayment(View view) {
        if (selectedImageBitmap != null) {
            // Convert Bitmap to byte array
            String filename = generateUniqueFilename();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Ambil violation code dari SharedPreferences
            // Execute AsyncTask untuk upload gambar
            String endpoint = "http://172.22.107.82:8000/paymentstatus/" + username;
            new UploadFileTask().execute(endpoint, imageBytes, violationCode, filename);
        } else {
            Toast.makeText(this, "Please select an image to upload", Toast.LENGTH_SHORT).show();
        }
    }
    private String generateUniqueFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        return "IMG_" + timestamp + ".jpg";
    }

    // Method untuk mengambil violation code dari SharedPreferences
    private String getViolationCode(int index) {
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        return prefs.getString("violationCode_" + index, "");
    }


    // AsyncTask to upload file (image) to server
    private class UploadFileTask extends AsyncTask<Object, Void, String> {
        private boolean isSuccess;

        @Override
        protected String doInBackground(Object... params) {
            String endpoint = (String) params[0];
            byte[] imageBytes = (byte[]) params[1];
            String violationCode = (String) params[2];

            HttpURLConnection connection = null;

            try {
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                connection.setRequestProperty("Accept", "application/json");

                // Prepare multipart form data
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                StringBuilder requestBody = new StringBuilder();

                // Add image data to request body
                requestBody.append(twoHyphens).append(boundary).append(lineEnd);
                requestBody.append("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"").append(lineEnd);
                requestBody.append("Content-Type: image/jpeg").append(lineEnd);
                requestBody.append(lineEnd);
                connection.getOutputStream().write(requestBody.toString().getBytes());
                connection.getOutputStream().write(imageBytes);
                connection.getOutputStream().write(lineEnd.getBytes());

                // Add violation code to request body
                requestBody.setLength(0); // clear the buffer
                requestBody.append(twoHyphens).append(boundary).append(lineEnd);
                requestBody.append("Content-Disposition: form-data; name=\"violationcode\"").append(lineEnd);
                requestBody.append(lineEnd);
                requestBody.append(violationCode).append(lineEnd);

                // End the multipart form data
                requestBody.append(twoHyphens).append(boundary).append(twoHyphens).append(lineEnd);
                connection.getOutputStream().write(requestBody.toString().getBytes());

                // Get response from server
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    isSuccess = true;
                    return response.toString();
                } else {
                    InputStream errorStream = connection.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    reader.close();
                    isSuccess = false;
                    return "Error uploading image: " + responseCode + " - " + errorResponse.toString();
                }
            } catch (IOException e) {
                e.printStackTrace();
                isSuccess = false;
                return "Error: " + e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(PaymentActivity.this, result, Toast.LENGTH_SHORT).show();
            if (isSuccess) {
                // Navigate back to MainMenuActivity
                Intent intent = new Intent(PaymentActivity.this, MainMenuActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }
    }

    // Method to retrieve violation code from SharedPreferences

}
