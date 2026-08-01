package com.mirea.iri.kt.belovleonid.phonebook;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class JsonAvatarPathLocalizer {
    private Context context;
    private static final String TAG = "JsonAvatarPathLocalizer";
    private final OkHttpClient client = new OkHttpClient();
    public JsonAvatarPathLocalizer(Context context) {
        this.context = context;
    }

    public JsonArray localize(JsonArray initialArray) {
        JsonArray finalArray = new JsonArray();
        File avatarsDir = new File(context.getFilesDir(), "avatars");
        boolean dirCreated = avatarsDir.mkdirs();
        Log.i(TAG, "Avatars dir created: " + dirCreated);

        int id = 1;
        for (JsonElement element : initialArray) {
            try {
                JsonObject elementObject = element.getAsJsonObject();

                String name = elementObject.has("name") ? elementObject.get("name").getAsString() : "";
                String number = elementObject.has("phone") ? elementObject.get("phone").getAsString() : "";
                String avatar = elementObject.has("avatar") ? elementObject.get("avatar").getAsString() : "";

                Log.i(TAG, "Name: " + name + ", Phone: " + number + ", Avatar: " + avatar);

                int patienceCounter = 3;
                String localAvatarPath = avatar;

                while (patienceCounter > 0) {
                    try {
                        if (!avatar.isEmpty()) {
                            String avatarFilename = "avatar_" + id + ".png";
                            File avatarFile = new File(avatarsDir, avatarFilename);
                            localAvatarPath = avatarFile.getAbsolutePath();

                            downloadImgTo(avatar, localAvatarPath, client);

                            Log.i(TAG, "Avatar download succeeded: " + avatar);
                            avatar = localAvatarPath;
                            break;
                        } else {
                            Log.i(TAG, "URL is empty, skipping download");
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception download: " + e.getMessage(), e);
                        patienceCounter--;
                    }
                }

                if (patienceCounter <= 0) {
                    Log.e(TAG, "Setting avatar to empty :(");
                    avatar = "";
                }

                String jsonString = "{" +
                        "\"name\": \"" + name + "\"," +
                        "\"phone\": \"" + number + "\"," +
                        "\"avatar\": \"" + avatar + "\"" +
                        "}";

                finalArray.add(JsonParser.parseString(jsonString));

            } catch (Exception e) {
                Log.e(TAG, "Exception with " + id + " element " + e.getMessage());
            }
            id++;
        }

        return finalArray;
    }

    private void downloadImgTo(String pictureURL,
                               String absolutePath,
                               OkHttpClient client) throws IOException {
        Request request = new Request.Builder()
                .url(pictureURL)
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            File file = new File(absolutePath);

            FileOutputStream fos = new FileOutputStream(file);

            InputStream is = response.body().byteStream();

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.flush();
            fos.close();
            is.close();

            response.close();
        } else {
            Log.e("Download", "Failed to download file: " + response.message());
        }
    }
}

