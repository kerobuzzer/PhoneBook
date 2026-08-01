package com.mirea.iri.kt.belovleonid.phonebook;


import android.app.Application;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public class RetrofitClient extends Application {

    private static Api api;

    public interface Api {
        @POST("login.php")
        @FormUrlEncoded
        Call<ServerResponse> getAllData(@Field("lgn")String login,
                                        @Field("pwd")String password,
                                        @Field("g")String group);

    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("RetrofitClient", "Client created");
        Retrofit rt = new Retrofit.Builder()
                .baseUrl("https://android-for-students.ru/coursework/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = rt.create(Api.class);
    }

    public static Api getServerApi() {
        return api;
    }

}
