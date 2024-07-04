package com.example.mobiletilangapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private EditText editTextEmail; // Add EditText for email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize EditText fields
        editTextUsername = findViewById(R.id.usernameEditText);
        editTextPassword = findViewById(R.id.passwordEditText);
        editTextEmail = findViewById(R.id.emailEditText); // Find email EditText

        Button registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve input values
                String username = editTextUsername.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String email = editTextEmail.getText().toString().trim(); // Get email input

                // Perform registration action by sending a POST request to the server
                performRegister(username, password, email);
            }
        });
    }

    private void performRegister(String username, String password, String email) {
        String url = "http://172.22.107.82:8000/register";

        try {
            // Create JSON object with registration data
            JSONObject requestData = new JSONObject();
            requestData.put("username", username);
            requestData.put("password", password);
            requestData.put("email", email); // Add email field to JSON
            requestData.put("isadmin", false);

            // Execute AsyncTask to send registration request
            RegisterTask registerTask = new RegisterTask();
            registerTask.execute(url, requestData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class RegisterTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            String requestData = params[1];
            String result = "";

            try {
                // Create URL connection
                URL apiUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Write JSON data to output stream
                OutputStream outputStream = conn.getOutputStream();
                outputStream.write(requestData.getBytes());
                outputStream.flush();
                outputStream.close();

                // Check response code
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response from input stream
                    InputStream inputStream = conn.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    StringBuilder response = new StringBuilder();

                    while ((line = bufferedReader.readLine()) != null) {
                        response.append(line);
                    }

                    // Close streams and set result
                    bufferedReader.close();
                    result = response.toString();
                } else {
                    result = "Error: " + responseCode;
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = "Error: " + e.getMessage();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("Response", "Response from server: " + result);
            try {
                JSONObject responseJson = new JSONObject(result);
                String message = responseJson.optString("message");

                if (message.equals("Register successful")) {
                    // Registration successful
                    Toast.makeText(RegisterActivity.this, "Pengguna berhasil didaftarkan", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // Registration failed (unexpected response)
                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(RegisterActivity.this, "Error: Invalid response from the server", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }
}
