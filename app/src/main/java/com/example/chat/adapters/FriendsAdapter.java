package com.example.chat.adapters;

import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.Activities.UsersActivity;
import com.example.chat.databinding.ItemContainerUserBinding;
import com.example.chat.listeners.FriendListener;
import com.example.chat.models.Friend;
import com.example.chat.models.User;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private final List<Friend>friends;
    private final FriendListener friendListener;

    public FriendsAdapter(List<Friend> friends, UsersActivity friendListener) {
        this.friends = friends;
        this.friendListener = (FriendListener) friendListener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding=ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new FriendViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        holder.setFriendData(friends.get(position));
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder{
        ItemContainerUserBinding binding;

       FriendViewHolder(ItemContainerUserBinding itemContainerUserBinding){
           super(itemContainerUserBinding.getRoot());
           binding=itemContainerUserBinding;
       }
       void setFriendData(Friend friend){
           binding.textPhone.setText(friend.phonee);
           binding.textName.setText(friend.namee);
           binding.imageProfile.setImageBitmap(getFriendImage(friend.imagee));
           binding.getRoot().setOnClickListener(v -> friendListener.onFriendClicked(friend));
       }
    }

    private Bitmap getFriendImage(String encodedImage){
        byte[]bytes= Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
    }

}
