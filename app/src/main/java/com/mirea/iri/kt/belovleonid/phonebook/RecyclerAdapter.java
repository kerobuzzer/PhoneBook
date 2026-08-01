package com.mirea.iri.kt.belovleonid.phonebook;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<String> contact_id, contact_name, contact_number, contact_avatar;
    private RecyclerAdapter.OnContactClickListener onContactClickListener;
    private static final String TAG = "RecyclerAdapter";
    public interface OnContactClickListener{
        void onContactClick(String id,
                            String name,
                            String number,
                            String avatar,
                            View view,
                            boolean isLongClick);
    }

    public RecyclerAdapter(Context context,
                           ArrayList<String> contact_id,
                           ArrayList<String> contact_name,
                           ArrayList<String> contact_number,
                           ArrayList<String> contact_avatar,
                           OnContactClickListener onContactClickListener) {
        this.context = context;
        this.contact_id = contact_id;
        this.contact_name = contact_name;
        this.contact_number = contact_number;
        this.contact_avatar = contact_avatar;
        this.onContactClickListener = onContactClickListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.contact_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        if (position < 0 || position >= contact_name.size()) {
            return;
        }

        String id = contact_id.get(position);
        String name = contact_name.get(position);
        String number = contact_number.get(position);
        String avatar = contact_avatar.get(position);

        holder.contact_name.setText(name);
        holder.contact_number.setText(number);

        holder.contact_avatar.setImageResource(R.drawable.avatar_sync);
        if (!avatar.isEmpty()) {
            Runnable avatarRunnable = () -> {
                File imgFile = new File(avatar);
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                try {
                    Activity activity = (Activity) context;
                    activity.runOnUiThread(() -> holder.contact_avatar.setImageBitmap(bitmap));
                } catch (ClassCastException e) {
                    Log.e(TAG, "well .. that was not activity");
                }
            };
            Thread avatarThread = new Thread(avatarRunnable);
            avatarThread.start();
        } else {
            holder.contact_avatar.setImageResource(R.drawable.avatar_placeholder);
        }
        holder.contact_card.setOnClickListener(view -> {
            if (onContactClickListener != null) onContactClickListener.onContactClick(
                    id,
                    name,
                    number,
                    avatar,
                    view,
                    false
            );
        });

        holder.contact_card.setOnLongClickListener(view -> {
            if (onContactClickListener != null) onContactClickListener.onContactClick(
                    id,
                    name,
                    number,
                    avatar,
                    view,
                    true
            );
            return true;
        });


    }

    public void updateData(
            ArrayList<String> newId,
            ArrayList<String> newFullName,
            ArrayList<String> newNumber,
            ArrayList<String> newAvatar
    ) {
        this.contact_id = newId;
        this.contact_name = newFullName;
        this.contact_number = newNumber;
        this.contact_avatar = newAvatar;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(contact_name != null) return contact_name.size();
        else return 0;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView contact_name, contact_number;
        ImageView contact_avatar;
        CardView contact_card;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            contact_name = itemView.findViewById(R.id.contact_fullname);
            contact_number = itemView.findViewById(R.id.contact_phone_number);
            contact_avatar = itemView.findViewById(R.id.contact_avatar);
            contact_card = itemView.findViewById(R.id.contact_card);
        }
    }
}
