package com.example.chat.Activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import com.example.chat.R;
import com.example.chat.adapters.UsersAdapter;
import com.example.chat.databinding.ActivityUsersBinding;
import com.example.chat.listeners.UserListener;
import com.example.chat.models.User;
import com.example.chat.utilities.Constants;
import com.example.chat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    Editable phoneFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityUsersBinding.inflate(getLayoutInflater());
        database=FirebaseFirestore.getInstance();
        setContentView(binding.getRoot());
        preferenceManager=new PreferenceManager(getApplicationContext());
        setListeners();
        // getUsers(); for all users
        getFriends();
    }
    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.fabNewChat2.setOnClickListener(v -> {
            Dialog dialog=new Dialog(UsersActivity.this);
            dialog.setTitle("ENTER USER PHONE");
            dialog.setContentView(R.layout.add_friend);
            EditText textPhone = dialog.findViewById(R.id.editPhone);
            dialog.show();
            phoneFriend = textPhone.getText();
            // final EditText editPhone=dialog.findViewById(R.id.editPhone);
            Button buttonAdd=dialog.findViewById(R.id.buttonAdd);

            buttonAdd.setOnClickListener(v1 -> {

                String idPhone=textPhone.getText().toString();
                if (TextUtils.isEmpty(idPhone)){
                    textPhone.setError("required");
                }else{
                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .whereEqualTo(Constants.KEY_PHONE,idPhone)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (queryDocumentSnapshots.isEmpty()){
                                    textPhone.setError("Phone not found");
                                }else{
                                    for (DocumentSnapshot documentSnapshot:queryDocumentSnapshots.getDocuments()){
                                        if (idPhone.equals(preferenceManager.getString("phone"))) {
                                            textPhone.setError("That is Your Phone");
                                        }else{
                                            checkFriendExists(String.valueOf(phoneFriend));
                                            dialog.cancel();
                                        }
                                    }
                                }
                            });
                }
            });
        });
    }




    private void checkFriendExists(String phoneFriend){
        database.collection("users").document("phone").collection("friend").document().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot documentSnapshot=task.getResult();
                if (documentSnapshot.exists()){
                    String idChatRoom=documentSnapshot.get("idChatRoom",String.class);
                    goChatRoom(idChatRoom, String.valueOf(phoneFriend));
                }else{
                    createNewChatRoom(String.valueOf(phoneFriend));
                }
            }
        });
    }

    private void createNewChatRoom(final String phoneFriend){
        HashMap<String,Object> dataChatRoom=new HashMap<>();
        dataChatRoom.put("date Added", FieldValue.serverTimestamp());
        dataChatRoom.put("ReceiverPhone",phoneFriend);
        dataChatRoom.put("SenderPhone",preferenceManager.getString("phone"));
        database.collection("ChatRoom").document(preferenceManager.getString("phone") + phoneFriend).set(dataChatRoom).addOnSuccessListener(unused -> {
            HashMap<String, Object> dataUserFriend = new HashMap<>();
            dataUserFriend.put("idChatRoom", preferenceManager.getString("phone") + phoneFriend);


            database.collection("users")
                    .document(preferenceManager.getString("userId"))
                    .collection("friend")
                    .document(phoneFriend)
                    .set(dataUserFriend)
             .addOnSuccessListener(unused1 -> goChatRoom(preferenceManager.getString("phone") + phoneFriend, phoneFriend));
        });
    }
    private void goChatRoom(String idChatRoom,String phoneFriend){
      /*  Intent i=new Intent(UsersActivity.this,ChatActivity.class);
        i.putExtra("idChatRoom",idChatRoom);
        i.putExtra("phoneFriend",phoneFriend);
        startActivity(i);*/
    }

    private void getFriends(){
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        final int[] i = {0};
        database.collection("users").document(preferenceManager.getString(Constants.KEY_USER_ID)).collection(Constants.KEY_FRIEND)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()&&task.getResult()!=null){
                        List<User> users=new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot:task.getResult()){
                            database.collection(Constants.KEY_COLLECTION_USERS)
                                    .whereEqualTo(Constants.KEY_PHONE, queryDocumentSnapshot.getId())
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()
                                                && task1.getResult() != null
                                                && task1.getResult().getDocuments().size() > 0) {
                                            DocumentSnapshot documentSnapshot = task1.getResult().getDocuments().get(0);
                                            Log.i("TAG_CREATE", String.valueOf(documentSnapshot.getString(Constants.KEY_NAME)));
                                            User user=new User();
                                            user.name = String.valueOf(documentSnapshot.getString(Constants.KEY_NAME));
                                            user.image=String.valueOf(documentSnapshot.getString(Constants.KEY_IMAGE));
                                            user.phone=String.valueOf(documentSnapshot.getString(Constants.KEY_PHONE));
                                            user.email=String.valueOf(documentSnapshot.getString(Constants.KEY_EMAIL));
                                            user.token=String.valueOf(documentSnapshot.getString(Constants.KEY_FCM_TOKEN));
                                            user.id=documentSnapshot.getId();
                                            Log.i("TAG_USER", user.id);
                                            users.add(user);
                                            i[0]++;

                                            if (users.size()>0 && i[0] == task.getResult().size()){
                                                UsersAdapter usersAdapter=new UsersAdapter(users,this);
                                                binding.usersRecyclerView.setAdapter(usersAdapter);
                                                binding.usersRecyclerView.setVisibility(View.VISIBLE);
                                            }else if (i[0] == task.getResult().size()){
                                                showErrorMessage();
                                            }
                                        }
                                    });
                        }

                    }
                });
    }

    //for all users
    private void getUsers(){
        //    loading(true);
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    //         loading(false);
                    String currentUserId=preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful()&&task.getResult()!=null) {
                        List<User> users = new ArrayList<>();
                        // List<Friend>friends=new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user=new User();
                            user.name=queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email=queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.phone=queryDocumentSnapshot.getString(Constants.KEY_PHONE);
                            user.image=queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token=queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id=queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if (users.size()>0){
                            UsersAdapter usersAdapter=new UsersAdapter(users,this);
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        }else{
                            showErrorMessage();
                        }
                    }else{
                        showErrorMessage();
                    }
                });
    }


    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s","No friends available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }
    // private void loading(Boolean isLoading){
    //     if (isLoading){
    //         binding.progressBar.setVisibility(View.VISIBLE);
    //     }else{
    //         binding.progressBar.setVisibility(View.INVISIBLE);
    //     }
    // }

    @Override
    public void onUserClicked(User user) {
        Intent intent= new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }
}