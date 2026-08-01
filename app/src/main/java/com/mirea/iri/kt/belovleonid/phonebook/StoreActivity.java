package com.mirea.iri.kt.belovleonid.phonebook;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mirea.iri.kt.belovleonid.phonebook.databinding.ActivityStoreBinding;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;

public class StoreActivity extends AppCompatActivity implements RecyclerAdapter.OnContactClickListener, View.OnClickListener {

    private static final String TAG = "StoreActivity";

    private ActivityStoreBinding binding;
    private DataBaseHelper dataBaseHelper;

    private RecyclerAdapter recyclerAdapter;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor shaPreEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "StoreActivity created");
        super.onCreate(savedInstanceState);

        binding = ActivityStoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataBaseHelper = new DataBaseHelper(this);
        ArrayList<String> contactId = new ArrayList<>();
        ArrayList<String> contactFullName = new ArrayList<>();
        ArrayList<String> contactNumber = new ArrayList<>();
        ArrayList<String> contactAvatar = new ArrayList<>();

        recyclerAdapter = new RecyclerAdapter(
                this,
                contactId,
                contactFullName,
                contactNumber,
                contactAvatar,
                this
        );

        binding.contactsRecyclerView.setAdapter(recyclerAdapter);
        binding.contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contactsLoading.setVisibility(View.GONE);

        binding.addButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        if (v == binding.addButton) {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.floating_button_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.log_out) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.attention)
                            .setMessage(R.string.are_you_sure_all_data_will_be_lost)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                                shaPreEdit = sharedPreferences.edit();
                                shaPreEdit.putBoolean("isDataSaved", false);
                                shaPreEdit.apply();

                                dataBaseHelper.removeContact(true);

                                Intent intent = new Intent(StoreActivity.this, MainActivity.class);
                                startActivity(intent);

                                dialog.dismiss();
                                finish();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> {
                                dialog.dismiss();
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return true;
                } else if (id == R.id.create_contact) {
                    Log.i(TAG, "Add button was clicked");
                    Intent intent = new Intent(StoreActivity.this, AddActivity.class);
                    startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            });
            popup.show();
        }
    }


    @Override
    public void onContactClick(String id, String name,
                               String number, String avatar,
                               View view, boolean isLongClick) {
        if (isLongClick) {
            call(number);
        } else {
            PopupMenu popup = new PopupMenu(this, view);
            popup.getMenuInflater().inflate(R.menu.contact_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int click_id = item.getItemId();
                if (click_id == R.id.call_contact){
                    call(number);
                    return true;
                } else if (click_id == R.id.share_contact) {
                    share(name, number);
                    return true;
                } else if (click_id == R.id.delete_contact) {
                    deleteContactUI(id, name, avatar, dataBaseHelper);
                    return true;
                } else {
                    return false;
                }
            });
            popup.show();
        }
    }
    private void deleteContactUI(String id, String name, String avatar, DataBaseHelper dataBaseHelper){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.attention)
                .setMessage(this.getString(R.string.are_you_sure_you_want_to_delete) + name + "?")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    int int_id = Integer.parseInt(id);
                    dataBaseHelper.removeContact(int_id, avatar);
                    refreshList();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void call(String number){
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + number));
        try {
            this.startActivity(dialIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_suitable_application_found, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.unexpected_error_occured_check_permissions, Toast.LENGTH_SHORT).show();
        }
    }
    private void share(String name, String number){
        String shareText = getString(R.string.fullname) + name + getString(R.string.number) + number;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, shareText);

        try {
            this.startActivity(Intent.createChooser(shareIntent, getString(R.string.share_contact)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_suitable_application_found, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.unexpected_error_occured_check_permissions, Toast.LENGTH_SHORT).show();
        }
    }
    private void refreshList(){
        binding.contactsLoading.setVisibility(View.VISIBLE);
        Runnable refreshRunnable = () -> {
            ArrayList<String> tempIdList = new ArrayList<>();
            ArrayList<String> tempFullNameList = new ArrayList<>();
            ArrayList<String> tempNumberList = new ArrayList<>();
            ArrayList<String> tempAvatarList = new ArrayList<>();

            fillAndSortArrays(tempIdList, tempFullNameList,
                    tempNumberList, tempAvatarList);

            runOnUiThread(() -> {
                recyclerAdapter.updateData(
                        tempIdList,
                        tempFullNameList,
                        tempNumberList,
                        tempAvatarList
                );
                binding.contactsLoading.setVisibility(View.GONE);
            });
        };

        Thread refreshThread = new Thread(refreshRunnable);
        refreshThread.start();

    }

    private void fillAndSortArrays(
            ArrayList<String> idList,
            ArrayList<String> fullNameList,
            ArrayList<String> numberList,
            ArrayList<String> avatarList
    ) {
        TreeMap<String, String[]> sortedMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Cursor cursor = dataBaseHelper.readAllData()) {
            if (cursor.getCount() == 0) {
                Log.e(TAG, "No data to be shown");
                runOnUiThread(() -> Toast.makeText(this, R.string.no_data_to_be_shown,
                        Toast.LENGTH_SHORT).show());
            } else {
                final int ID_INDEX = 0;
                final int NAME_INDEX = 1;
                final int NUMBER_INDEX = 2;
                final int AVATAR_INDEX = 3;
                while (cursor.moveToNext()) {
                    sortedMap.put(cursor.getString(NAME_INDEX), new String[]{
                            cursor.getString(ID_INDEX),
                            cursor.getString(NUMBER_INDEX),
                            cursor.getString(AVATAR_INDEX)
                    });
                }
                for (String keyName : sortedMap.keySet()) {
                    String id = Objects.requireNonNull(sortedMap.get(keyName))[0];
                    String name = keyName;
                    String number = Objects.requireNonNull(sortedMap.get(keyName))[1];
                    String avatar = Objects.requireNonNull(sortedMap.get(keyName))[2];
                    idList.add(id);
                    fullNameList.add(name);
                    numberList.add(number);
                    avatarList.add(avatar);
                }
                Log.d(TAG, "fill and sort arrays successful");
            }
        } catch (Exception e) {
            Log.e(TAG, "shit happened: " + e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "StoreActivity started");
        super.onStart();
        refreshList();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "StoreActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "StoreActivity paused");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "StoreActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "StoreActivity destroyed");
    }
}