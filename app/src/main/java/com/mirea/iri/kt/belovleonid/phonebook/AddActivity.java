package com.mirea.iri.kt.belovleonid.phonebook;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mirea.iri.kt.belovleonid.phonebook.databinding.ActivityAddBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class AddActivity extends AppCompatActivity implements View.OnClickListener, ActivityResultCallback<ActivityResult> {

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private static final String TAG = "AddActivity";

    private ActivityAddBinding binding;
    private String avatarPath = "";
    private DataBaseHelper dataBaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "AddActivity created");
        super.onCreate(savedInstanceState);

        binding = ActivityAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataBaseHelper = new DataBaseHelper(this);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),this);

        binding.editImageButton.setOnClickListener(this);
        binding.createContactButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == binding.createContactButton){
            Log.i(TAG, "Create button was clicked");

            String name = binding.nameEdit.getText().toString().trim();
            String phone = binding.numberEdit.getText().toString().trim();
            Log.i(TAG, name + phone);

            if(!name.isEmpty() && !phone.isEmpty()) {
                createContacts(stringToJsonArray(name, phone, avatarPath));
            } else {
                Toast.makeText(this , R.string.fill_in_fields, Toast.LENGTH_SHORT).show();
            }
        } else if (v == binding.editImageButton) {
            Log.i(TAG, "IMG button was clicked");

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            try {
                imagePickerLauncher.launch(intent);
            } catch (Exception e){
                Log.e(TAG, "error on image picker " + e.getMessage());
                Toast.makeText(this, R.string.unable_to_pick_image_check_permissions,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(ActivityResult activityResult) {
        if (activityResult.getResultCode() == AddActivity.RESULT_OK){
            Intent data = activityResult.getData();
            if (data != null) {
                Uri imageUri = data.getData();
                try {
                    File avatarsDir = new File(getFilesDir(), "avatars");
                    if (!avatarsDir.exists()) avatarsDir.mkdirs();


                    String lastId;
                    try{
                        lastId = Integer.toString(dataBaseHelper.getLastId() + 1);
                    } catch (RuntimeException e) {
                        Log.e(TAG, "failed to check last ID");
                        lastId = String.valueOf(System.currentTimeMillis());
                    }

                    String fileName = "avatar_" + lastId + ".png";
                    File destFile = new File(avatarsDir, fileName);

                    if (destFile.exists()) {
                        destFile.delete();
                        Log.i(TAG, "old avatar deleted: " + destFile.getAbsolutePath());
                    }

                    Log.i(TAG, "new avatar picture with name " + fileName + ".png");

                    try (InputStream in = getContentResolver().openInputStream(Objects.requireNonNull(imageUri));
                         OutputStream out = new FileOutputStream(destFile)) {
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                    avatarPath = destFile.getAbsolutePath();
                    if (!avatarPath.isEmpty()) {
                        File imgFile = new File(avatarPath);
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        binding.avatarEdit.setImageBitmap(bitmap);
                    } else {
                        binding.avatarEdit.setImageResource(R.drawable.avatar_placeholder);
                    }

                    Log.i(TAG, "avatar saved: " + avatarPath);

                } catch (IOException e) {
                    Log.e(TAG, "error on avatar choosing " + e.getMessage());
                }
            }
        }
    }

    private JsonArray stringToJsonArray(String name, String phone, String avatar){
        String jsonString = "[{" +
                "\"name\": \"" + name + "\"," +
                "\"phone\": \"" + phone + "\"," +
                "\"avatar\": \"" + avatar + "\"" +
                "}]";
        return JsonParser.parseString(jsonString).getAsJsonArray();
    }
    private void createContacts(JsonArray jsonArray){
        Runnable addContactRunnable = () -> {
            Log.i(TAG, jsonArray.toString());
            try {
                dataBaseHelper.addContacts(jsonArray);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.succsesful_write), Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (RuntimeException ex) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_on_write), Toast.LENGTH_SHORT).show());
            }
        };
        Thread addContactThread = new Thread(addContactRunnable);
        addContactThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "AddActivity started");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "AddActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "AddActivity paused");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "AddActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AddActivity destroyed");
    }


}