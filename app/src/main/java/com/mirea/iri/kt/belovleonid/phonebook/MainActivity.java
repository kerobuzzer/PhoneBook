package com.mirea.iri.kt.belovleonid.phonebook;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.mirea.iri.kt.belovleonid.phonebook.databinding.ActivityMainBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Callback<ServerResponse> {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor shaPreEdit;
    private ActivityMainBinding binding;
    private DataBaseHelper dataBaseHelper;
    private static final String TAG = "MainActivity";

    public interface OnContactsCreatedListener {
        void onSuccessContactsCreated();
        void onErrorContactsCreated();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity created");
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.progressBar.setVisibility(View.GONE);
        binding.incorrectPassword.setVisibility(View.GONE);

        dataBaseHelper = new DataBaseHelper(this);
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        shaPreEdit = sharedPreferences.edit();

        if(sharedPreferences.getBoolean("isDataSaved", false)){
            Log.d(TAG, "Skipped login screen");
            Intent intent = new Intent(MainActivity.this, StoreActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        binding.next.setOnClickListener(this);
        binding.skip.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if(v == binding.next){
            Log.d(TAG, "Login button clicked");

            binding.progressBar.setVisibility(View.VISIBLE);
            binding.incorrectPassword.setVisibility(View.GONE);

            String login = binding.login.getText().toString();
            String password = binding.password.getText().toString();

            if(!login.isEmpty() && !password.isEmpty()){
                Call<ServerResponse> call = RetrofitClient.getServerApi()
                        .getAllData(login, password, "RIBO-02-23");
                try {
                    call.enqueue(this);
                } catch (Exception e){
                    Log.e(TAG, "Exception on request" + e.getMessage());
                    binding.incorrectPassword.setText(R.string.try_again);
                    binding.incorrectPassword.setVisibility(View.VISIBLE);
                }
            }else{
                Log.d(TAG, "Empty fields haha");
                binding.incorrectPassword.setText(R.string.fill_in_fields);
                binding.progressBar.setVisibility(View.GONE);
                binding.incorrectPassword.setVisibility(View.VISIBLE);
            }
        } else if (v == binding.skip) {
            Toast.makeText(this, R.string.to_download_data_use_login, Toast.LENGTH_SHORT).show();
            shaPreEdit.putBoolean("isDataSaved", true);
            shaPreEdit.apply();
            Log.d(TAG, "Started StoreActivity");
            Intent intent = new Intent(MainActivity.this, StoreActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onResponse(@NonNull Call<ServerResponse> call, Response<ServerResponse> response) {
        binding.incorrectPassword.setVisibility(View.GONE);
        Log.d(TAG, "Successful response from server");

        if (response.isSuccessful() && response.body() != null && response.body().getResultCode() == 1){
            Log.d(TAG, "login is successful");
            Log.i(TAG, response.body().getTask());

            shaPreEdit.putBoolean("isDataSaved", true);
            shaPreEdit.apply();
            try {
                createContacts(response.body().getData(), new OnContactsCreatedListener() {
                    @Override
                    public void onSuccessContactsCreated() {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, R.string.succsesful_write, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Started StoreActivity");
                        Intent intent = new Intent(MainActivity.this, StoreActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    @Override
                    public void onErrorContactsCreated() {
                        Toast.makeText(MainActivity.this, R.string.error_on_contact_creating, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this, R.string.error_on_write, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "wrong login/password");
            binding.incorrectPassword.setText(R.string.invalid_login_or_password);
            binding.incorrectPassword.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFailure(@NonNull Call<ServerResponse> call, Throwable t) {
        binding.progressBar.setVisibility(View.GONE);
        binding.incorrectPassword.setText(R.string.check_intenet_connection);
        binding.incorrectPassword.setVisibility(View.VISIBLE);
        Log.d(TAG, "Server is unreachable :( " + t.getMessage());
    }

    private void createContacts(JsonArray jsonArray, OnContactsCreatedListener listener){

        Runnable addContactRunnable = () -> {

            Log.i(TAG, jsonArray.toString());
            try {
                JsonAvatarPathLocalizer localizer = new JsonAvatarPathLocalizer(this);
                JsonArray localArray = localizer.localize(jsonArray);
                Log.i(TAG, localArray.toString());
                dataBaseHelper.addContacts(localArray);

                runOnUiThread( () -> {
                    binding.progressBar.setVisibility(View.GONE);
                    listener.onSuccessContactsCreated();
                });
            } catch (Exception ex) {
                Log.e(TAG, "Write contacts error, null array", ex);
                runOnUiThread(() -> listener.onErrorContactsCreated());
            }
        };

        Thread addContactThread = new Thread(addContactRunnable);
        addContactThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity started");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity paused");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity stopped");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity destroyed");
        super.onDestroy();
    }



}

