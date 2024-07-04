package com.example.mobiletilangapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class MainMenuActivity extends AppCompatActivity {

    private TextView textViewHelloUser;
    private LinearLayout violationsLayout;
    private String username;
    private String violationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Initialize views
        textViewHelloUser = findViewById(R.id.textViewHelloUser);
        violationsLayout = findViewById(R.id.violationsLayout);

        Button logoutButton = findViewById(R.id.logoutButton);
        // Tombol Bayar

        // Set click listener for logout button
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        // Set click listener for bayar button


        // Get username from SharedPreferences
        username = getUsername();
        Log.d("MainMenuActivity", "Retrieved username: " + username); // Debug log to check retrieved username
        if (!username.isEmpty()) {
            // Set welcome message
            textViewHelloUser.setText("Hello, " + username);

            // Load violation info and violation proof
            fetchViolationCount(username);
        } else {
            // Handle case where username is not available
            textViewHelloUser.setText("Hello, User");
        }
    }

    // Method untuk logout, clear username, dan intent kembali ke MainActivity
    private void logoutUser() {
        clearUsername();

        Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close the MainMenuActivity
    }

    // Method untuk menghapus username dari SharedPreferences
    private void clearUsername() {
        SharedPreferences.Editor editor = getSharedPreferences("my_prefs", MODE_PRIVATE).edit();
        editor.remove("username");
        editor.apply();
    }

    // Method untuk mengambil username dari SharedPreferences
    private String getUsername() {
        return getSharedPreferences("my_prefs", MODE_PRIVATE)
                .getString("username", "");
    }

    // Method untuk mengambil jumlah pelanggaran dari server
    private void fetchViolationCount(String username) {
        new FetchViolationCountTask().execute(username);
    }

    // AsyncTask untuk mengambil jumlah pelanggaran
    private class FetchViolationCountTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            int count = 0;
            try {
                String username = params[0];
                URL url = new URL("http://172.22.107.82:8000/violationinfo/count/" + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    count = jsonResponse.getInt("count");
                }
                connection.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return count;
        }

        @Override
        protected void onPostExecute(Integer count) {
            // Fetch each violation based on the count
            for (int i = 0; i < count; i++) {
                fetchViolationInfo(username, i);
            }
        }
    }

    // Method untuk mengambil informasi pelanggaran dari server
    private void fetchViolationInfo(String username, int index) {
        new FetchViolationInfoTask().execute(username, String.valueOf(index));
    }

    // AsyncTask untuk mengambil informasi pelanggaran
    private class FetchViolationInfoTask extends AsyncTask<String, Void, String> {

        private int index;

        @Override
        protected String doInBackground(String... params) {
            StringBuilder result = new StringBuilder();
            try {
                String username = params[0];
                index = Integer.parseInt(params[1]);
                // URL untuk mengambil informasi pelanggaran berdasarkan index
                URL url = new URL("http://172.22.107.82:8000/violationinfo/" + username + "/" + index);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                    reader.close();
                } else {
                    result.append("Error: ").append(responseCode);
                }
                connection.disconnect();

            } catch (Exception e) {
                result.append("Error: ").append(e.getMessage());
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // Handle result untuk informasi pelanggaran
            Log.d("FetchViolationInfoTask", "Server response: " + result);
            parseViolationInfo(result, index);
        }
    }

    private void parseViolationInfo(String response, int index) {
        try {
            if (!response.contains("Violation information for user")) {
                throw new Exception("Response is not in the expected format");
            }

            String[] parts = response.split("\n");
            String violationCode = null;
            StringBuilder violationInfo = new StringBuilder();
            String imageUrl = null;

            for (String part : parts) {
                if (part.contains("Code:")) {
                    violationCode = part.split("Code:")[1].trim(); // Ambil bagian setelah "Code:"
                } else if (part.contains("Image URL:")) {
                    imageUrl = part.split("Image URL:")[1].trim();
                }
                violationInfo.append(part.trim()).append("\n");
            }

            if (violationCode != null) {
                // Save violation code and index to SharedPreferences
                Set<String> violationCodes = getViolationCodes();
                violationCodes.add(violationCode + ":" + index); // Store violation code and index together
                saveViolationCodes(violationCodes);
                Log.d("MainMenuActivity", "Violation code saved: " + violationCode + " at index: " + index); // Debug log with index
            } else {
                throw new Exception("Violation code not found in response");
            }

            // Create TextView and ImageView dynamically for each violation
            TextView textViewViolationInfo = new TextView(this);
            ImageView imageViewViolationProof = new ImageView(this);

            textViewViolationInfo.setText(violationInfo.toString());
            textViewViolationInfo.setClickable(true);
            textViewViolationInfo.setFocusable(true);

            String finalViolationCode2 = violationCode;
            textViewViolationInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startViolationProofActivity(username, index, finalViolationCode2);
                }
            });
            violationsLayout.addView(textViewViolationInfo);

            if (imageUrl != null) {
                // Fetch and display image using FetchViolationProofTask
                new FetchViolationProofTask(imageViewViolationProof).execute(imageUrl);
                imageViewViolationProof.setClickable(true);
                imageViewViolationProof.setFocusable(true);
                String finalViolationCode = violationCode;
                imageViewViolationProof.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startViolationProofActivity(username, index, finalViolationCode);
                    }
                });
                violationsLayout.addView(imageViewViolationProof);
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    // Method untuk menyimpan violation code ke SharedPreferences
    private void saveViolationCodes(Set<String> violationCodes) {
        SharedPreferences.Editor editor = getSharedPreferences("my_prefs", MODE_PRIVATE).edit();
        editor.putStringSet("violationCodes", violationCodes);
        editor.apply();
    }

    // Method untuk mengambil semua violation code dari SharedPreferences
    private Set<String> getViolationCodes() {
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        return prefs.getStringSet("violationCodes", new HashSet<String>());
    }

    // Method untuk mengambil violation code dari SharedPreferences


    // Method untuk memulai ViolationProofActivity dengan username dan index yang dipilih
    private void startViolationProofActivity(final String username, final int index, final String violationCode) {
        Intent intent = new Intent(MainMenuActivity.this, ViolationProofActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("index", index); // Mengirimkan indeks pelanggaran
        intent.putExtra("violationCode", violationCode); // Pass violation code
        startActivity(intent);
    }


    // Method untuk navigasi ke PaymentActivity
    private void navigateToPaymentActivity(String violationCode) {
        if (!violationCode.isEmpty()) {
            Intent intent = new Intent(MainMenuActivity.this, PaymentActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("violationCode", violationCode);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No violation selected", Toast.LENGTH_SHORT).show();
        }
    }

    // AsyncTask untuk mengambil bukti pelanggaran
    private class FetchViolationProofTask extends AsyncTask<String, Void, Bitmap> {

        private ImageView imageView;

        public FetchViolationProofTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                String imageUrl = params[0];
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // Display the fetched bitmap in imageViewViolationProof
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(MainMenuActivity.this, "Failed to load violation proof", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
