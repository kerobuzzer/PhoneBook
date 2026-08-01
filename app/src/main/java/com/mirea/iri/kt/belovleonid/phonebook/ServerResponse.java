package com.mirea.iri.kt.belovleonid.phonebook;


import com.google.gson.JsonArray;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class ServerResponse {
    @SerializedName("result_code")
    @Expose
    private Integer resultCode;
    @SerializedName("data")
    @Expose
    private JsonArray data;
    @SerializedName("task")
    @Expose
    private String task;

    public Integer getResultCode() {
        return resultCode;
    }

    public JsonArray getData() {
        return data;
    }

    public String getTask() {
        return task;
    }

}
