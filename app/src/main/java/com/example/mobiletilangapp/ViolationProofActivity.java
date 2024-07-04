package com.example.mobiletilangapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ViolationProofActivity extends AppCompatActivity {

    private ImageView imageViewViolationProof;
    private Button buttonBack;
    private Button buttonToPayment; // New button to navigate to PaymentActivity
    private String username;
    private int index;
    private String violationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_violation_proof);

        imageViewViolationProof = findViewById(R.id.imageViewViolationProof);
        buttonBack = findViewById(R.id.buttonBack);
        buttonToPayment = findViewById(R.id.buttonToPayment); // Initialize button

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to the previous activity (MainMenuActivity)
            }
        });

        buttonToPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPaymentActivity(index); // Call navigateToPaymentActivity method when button is clicked
            }
        });

        username = getIntent().getStringExtra("username");
        index = getIntent().getIntExtra("index", 0); // Terima indeks pelanggaran
        violationCode = getIntent().getStringExtra("violationCode"); // Retrieve violation code from intent

        if (username != null) {
            fetchViolationProof(username, index);
        } else {
            Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
        }
    }


    private void fetchViolationProof(String username, int index) {
        new FetchViolationProofTask().execute(username, String.valueOf(index));
    }

    private class FetchViolationProofTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                String username = params[0];
                int index = Integer.parseInt(params[1]);
                URL url = new URL("http://172.22.107.82:8000/seeviolationproof/" + username + "/" + index);
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
            if (bitmap != null) {
                imageViewViolationProof.setImageBitmap(bitmap);

                // Save violation code to SharedPreferences
                saveViolationCode(violationCode, index); // Assuming you have a method for saving violation code
            } else {
                Toast.makeText(ViolationProofActivity.this, "Failed to load violation proof", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to save violation code to SharedPreferences
    private void saveViolationCode(String violationCode, int index) {
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("violationCode_" + index, violationCode);
        editor.apply();
    }
    private String getViolationCode(int index) {
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        // Mendapatkan violationCode dengan kunci yang sesuai dengan indeks
        return prefs.getString("violationCode_" + index, "");
    }

    // Method to navigate to PaymentActivity
    public void navigateToPaymentActivity(int index) {
        Intent intent = new Intent(ViolationProofActivity.this, PaymentActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("index", index); // Mengirim indeks yang dipilih
        intent.putExtra("violationCode", getViolationCode(index)); // Mengambil violation code berdasarkan indeks
        startActivity(intent);
    }
}
