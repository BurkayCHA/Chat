package com.example.chat.Activities;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.example.chat.databinding.ActivitySettingsBinding;
import com.example.chat.utilities.Constants;
import com.example.chat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private String encodedImage;
    private static final String TAG = "TAG_SETTINGS";
    private FirebaseFirestore database;
    PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        preferenceManager=new PreferenceManager(getApplicationContext());
        Constants.sharedPreferences = getSharedPreferences(Constants.PREFERENCE_KEY, 0);
    }

    private void setListeners() {
        binding.settingsBack.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        binding.layoutSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        binding.changeSave.setOnClickListener(v -> change());
    }

    private void change() {
        database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.changeUserName.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        UpdateImageData(encodedImage);
        UpdateNameData(binding.changeUserName.getText().toString());
    }

    void UpdateImageData(String newData) {
        if (newData!=null){
            database = FirebaseFirestore.getInstance();
            Map<String, Object> profileImage = new HashMap<>();
            profileImage.put(Constants.KEY_IMAGE, newData);
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .whereEqualTo(Constants.KEY_EMAIL,"y@y.com")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            Log.i("TAG_MAIN", String.valueOf(documentSnapshot.getString(Constants.KEY_IMAGE)));

                            database.collection("users")
                                    .document(documentSnapshot.getId())
                                    .update(profileImage)
                                    .addOnSuccessListener(unused -> {
                                        Constants.editor.putString("image", newData);
                                        Constants.editor.apply();
                                        Toast.makeText(SettingsActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
        }
    }
    void UpdateNameData(String newData) {
        if (!newData.isEmpty()){
            database = FirebaseFirestore.getInstance();
            Map<String, Object> userName = new HashMap<>();
            userName.put(Constants.KEY_NAME, newData);
           // Log.i(TAG,);
            database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, "y@y.com")  //String.valueOf(database.collection(Constants.KEY_COLLECTION_USERS).document(Constants.KEY_USER_ID).collection(Constants.KEY_EMAIL)))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        Log.i("TAG_MAIN", String.valueOf(documentSnapshot.getString(Constants.KEY_NAME)));

                        database.collection("users")
                                .document(documentSnapshot.getId())
                                .update(userName)
                                .addOnSuccessListener(unused -> {
                                    Constants.editor.putString("name", newData);
                                    Constants.editor.apply();
                                    Toast.makeText(SettingsActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }
       // Toast.makeText(SettingsActivity.this, "Insert Name", Toast.LENGTH_SHORT).show();
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
                            binding.settingsProfileImage.setImageBitmap(bitmap);
                            encodedImage= encodeImage(bitmap);
                            binding.settingschangeProfileImage.setVisibility(View.GONE);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

}