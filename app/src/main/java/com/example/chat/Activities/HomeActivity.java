package com.example.chat.Activities;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.chat.adapters.RecentConversationsAdapter;
import com.example.chat.databinding.ActivityMainBinding;
import com.example.chat.listeners.ConversionListener;
import com.example.chat.listeners.UserListener;
import com.example.chat.models.ChatMessage;
import com.example.chat.models.User;
import com.example.chat.utilities.Constants;
import com.example.chat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class HomeActivity extends BaseActivity implements ConversionListener,UserListener {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    public static String currentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        database=FirebaseFirestore.getInstance();

        setContentView(binding.getRoot());
        preferenceManager=new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        setListeners();
        listenConversations();
        Constants.sharedPreferences = getSharedPreferences(Constants.PREFERENCE_KEY, 0);
        Constants.editor = Constants.sharedPreferences.edit();
        String ID=preferenceManager.getString(Constants.KEY_USER_ID);

        DocumentReference docRef = database.collection(Constants.KEY_COLLECTION_USERS).document(ID);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    currentEmail= String.valueOf(document.get(Constants.KEY_EMAIL));
                }
            }
        });
    }

    private void init(){
        conversations=new ArrayList<>();
        conversationsAdapter=new RecentConversationsAdapter(conversations,this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        database=FirebaseFirestore.getInstance();
    }
    //clicked
    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.imageProfile.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
            finish();
        });
        binding.fabNewChat.setOnClickListener(v ->
            startActivity(new Intent(getApplicationContext(),UsersActivity.class)));

    }
    //myprofile
    private void loadUserDetails(){
        binding.textName.setText(Constants.sharedPreferences.getString(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME)));//(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes= Base64.decode(Constants.sharedPreferences.getString(Constants.KEY_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE)),Base64.DEFAULT);
        Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
    //error message
    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener=((value, error) -> {
        if (error!=null){
            return;
        }
        if (value!=null){
            for (DocumentChange documentChange:value.getDocumentChanges()){
                if (documentChange.getType()== DocumentChange.Type.ADDED){
                    String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverID=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage=new ChatMessage();
                    chatMessage.senderId=senderId;
                    chatMessage.receiverId=receiverID;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    }else{
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                }else if (documentChange.getType()==DocumentChange.Type.MODIFIED){
                    for (int i =0;i<conversations.size();i++){
                        String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if (conversations.get(i).senderId.equals(senderId)&&conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations,(obj1,obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    });

    private void signOut(){
        showToast("Signing out..");
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String,Object>updates =new HashMap<>();
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent= new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }
}