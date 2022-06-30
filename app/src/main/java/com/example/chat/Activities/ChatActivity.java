package com.example.chat.Activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chat.R;
import com.example.chat.adapters.ChatAdapter;
import com.example.chat.databinding.ActivityChatBinding;
import com.example.chat.models.ChatMessage;
import com.example.chat.models.User;
import com.example.chat.network.ApiClient;
import com.example.chat.network.ApiService;
import com.example.chat.utilities.Constants;
import com.example.chat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId=null;
    private Boolean isReceiverAvailable=false;
    private String encodedImage;

    /*
        mesaj tıpını kontrol et (image or text)
        eger ımage ıse set et imageview visibility vısıbıle
        text ıse farklı degıstırme.
         */
    Uri uri;
    private static final int PICK_IMAGE=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }

   @SuppressLint("IntentReset")
   private void setListeners(){
     binding.imageBack.setOnClickListener(v -> onBackPressed());
     binding.layoutSend.setOnClickListener(v -> sendMessage());
     binding.layoutSendPic.setOnClickListener(v -> {
         Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
       //  intent.setType("image/*");
         intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
         pickImage.launch(intent);
     });
    }

   private void init(){
        preferenceManager=new PreferenceManager(getApplicationContext());
        chatMessages=new ArrayList<>();
        chatAdapter=new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database=FirebaseFirestore.getInstance();
   }
    private String encodeImage(Bitmap bitmap){
        int previewWidth=150;
        int previewHeight=bitmap.getHeight()*previewWidth/ bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        byte[] bytes=byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }
    private final ActivityResultLauncher<Intent> pickImage=registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode()==RESULT_OK){
                    if (result.getData()!=null){
                        Uri imageuri=result.getData().getData();
                        try{
                            InputStream inputStream= getContentResolver().openInputStream(imageuri);
                            Bitmap bitmap= BitmapFactory.decodeStream(inputStream);
                            //binding.sendPic.setImageBitmap(bitmap);
                            encodedImage=encodeImage(bitmap);
                            sendPic();
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

   private void sendPic(){
       //ImageView img= findViewById(R.id.sendPicture);
       HashMap<String,Object>message=new HashMap<>();
       message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
       message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
       message.put(Constants.KEY_TIMESTAMP,new Date());
       message.put(Constants.KEY_MESSAGE,encodedImage);
       database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
       if (conversionId!=null){
           updateConversion("photo");
       }else{
           HashMap<String,Object>conversion =new HashMap<>();
           conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
           conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
           conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
           conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
           conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
          // conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image);
           conversion.put(Constants.KEY_LAST_MESSAGE,"photo");
          // conversion.put(Constants.KEY_LAST_MESSAGE,"encodedImage");
           conversion.put(Constants.KEY_TIMESTAMP,new Date());
           addConversion(conversion);
       }
       if (!isReceiverAvailable){
           try {
                JSONArray tokens=new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data=new JSONObject();
                data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN,preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
                JSONObject body=new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA,data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

                sendNotification(body.toString());
           }catch (Exception exception){
               showToast(exception.getMessage());
           }
       }
       binding.inputMessage.setText(null);
   }
   private void sendMessage(){
        HashMap<String, Object> message=new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
        message.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP,new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if (conversionId!=null){
            updateConversion(binding.inputMessage.getText().toString());
        }else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
            if (!isReceiverAvailable){
                try {
                    JSONArray tokens=new JSONArray();
                    tokens.put(receiverUser.token);

                    JSONObject data=new JSONObject();
                    data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                    data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
                    data.put(Constants.KEY_FCM_TOKEN,preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                    data.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
                    JSONObject body=new JSONObject();
                    body.put(Constants.REMOTE_MSG_DATA,data);
                    body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);
                    sendNotification(body.toString());
                }catch (Exception exception){
                    showToast(exception.getMessage());
                }
            }
            binding.inputMessage.setText(null);
        }


         Bitmap StringToBitMap(String encodedString){
        try{
            byte [] encodeByte = Base64.decode(encodedString,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        }
        catch(Exception e){
            e.getMessage();
            return null;
        }
    }

   private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
   }
   private void sendNotification(String messageBody){
       ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
       ).enqueue(new Callback<String>() {
            @Override
           public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if (response.isSuccessful()){
                    try {
                        if (response.body()!=null){
                            JSONObject responseJson=new JSONObject(response.body());
                            JSONArray results=responseJson.getJSONArray("results");
                            Log.i("results", String.valueOf(results));
                            if (responseJson.getInt("failure")==1){
                                JSONObject error=(JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }catch (JSONException e){
                        Log.e("TAG_NOTIF", e.getMessage());
                        e.printStackTrace();
                    }
                    showToast("Notification sent succesfully");
                    Log.e("TAG_NOTIF", "not sent");
                }else{
                    showToast("Error"+response.code());
                    Log.e("TAG_NOTIF", "Error: " + response.code() + "-" + response + "-" + response.errorBody());
                }
            }

           @Override
           public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
           }
       });
   }
   private void listenAvailabilityReceiver(){
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this,(value, error) -> {
            if (error!=null){
                return;
            }
            if (value!=null){
                if (value.getLong(Constants.KEY_AVAILABILITY)!=null){
                    int availability= Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable=availability==1;
                }
                receiverUser.token=value.getString(Constants.KEY_FCM_TOKEN);
            }
            if (isReceiverAvailable){
                binding.textAvailability.setVisibility(View.VISIBLE);
            }else{
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
   }

   private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

   @SuppressLint({"NotifyDataSetChanged", "NewApi"})
   private final EventListener<QuerySnapshot> eventListener=(value, error)->{
        if (error!=null){
            return;
        }
        if (value!=null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    //////////////////////////////////////////dene burayı
                  //  chatMessage.sendImage= String.valueOf(documentChange.getDocument().get(Constants.KEY_SENDER_IMAGE));
                  //  chatMessage.receivImage= String.valueOf(documentChange.getDocument().get(Constants.KEY_RECEIVER_IMAGE));
                  //////////////////////////////////////////////
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject =documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, Comparator.comparing(o -> o.dateObject));

            if (count==0){
                chatAdapter.notifyDataSetChanged();
            }else{
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId==null){
            checkForConversion();
        }
   };

   private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes= Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
   }
   private void loadReceiverDetails(){
        receiverUser=(User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
   }


    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd,yyyy- hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String,Object>conversion){
       database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
               .add(conversion)
               .addOnSuccessListener(documentReference -> conversionId=documentReference.getId());
    }

   private void updateConversion(String message){
        DocumentReference   documentReference=
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP,new Date()
        );
   }

   private void checkForConversion(){
       if (chatMessages.size()!=0){
           checkForConversionRemotely(
                   preferenceManager.getString(Constants.KEY_USER_ID),
                   receiverUser.id
           );
           checkForConversionRemotely(
                   receiverUser.id,
                   preferenceManager.getString(Constants.KEY_USER_ID)
          );
      }
   }

   private void checkForConversionRemotely(String senderId,String receiverId){
       database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
               .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
               .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
               .get()
               .addOnCompleteListener(conversionOnCompleteListener);
    }


   private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener=task ->{
       if (task.isSuccessful()&&task.getResult()!=null&&task.getResult().getDocuments().size()>0){
           DocumentSnapshot documentSnapshot=task.getResult().getDocuments().get(0);
           conversionId=documentSnapshot.getId();
       }
    };

    @Override
   protected void onResume() {
        super.onResume();
        listenAvailabilityReceiver();
    }
}