package com.example.chat.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chat.databinding.ActivitySignUpBinding;
import com.example.chat.utilities.Constants;
import com.example.chat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "TAG_SIGNUP";
    public PreferenceManager preferenceManager;
    private ActivitySignUpBinding binding;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);
        setListeners();
    }
    private void setListeners(){
        binding.buttonSign.setOnClickListener(v ->onBackPressed());
        binding.buttonCreate.setOnClickListener(v -> {
            if (isValidSignUpDetails()){
                signUp();
            }
        });
        binding.layoutProfile.setOnClickListener(v->{
           Intent intent =new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
           intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
           pickImage.launch(intent);
           binding.profileText.setVisibility(View.INVISIBLE);
        });

    }

    public void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_PHONE, binding.signupPhone.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.signupEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.Password.getText().toString());
        user.put(Constants.KEY_NAME, binding.signupName.getText().toString());
        user.put(Constants.KEY_IMAGE,encodedImage);

        //numara kayıtlı mı kontrol
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE, binding.signupPhone.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()
                            && task.getResult() != null
                            && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                         Log.i("TAG_MAIN", String.valueOf(documentSnapshot.getString(Constants.KEY_NAME)));
                        Toast.makeText(getApplicationContext(), "Phone Using.", Toast.LENGTH_SHORT).show();
                    }else{
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .add(user)
                                .addOnSuccessListener(documentReference -> {
                                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                                    preferenceManager.putString(Constants.KEY_NAME, binding.signupName.getText().toString());
                                    preferenceManager.putString(Constants.KEY_PHONE, binding.signupPhone.getText().toString());
                                    preferenceManager.putString(Constants.KEY_IMAGE,encodedImage);
                                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(exception -> {

                                    showToast(exception.getMessage());
                                });
                        Log.i("TAG_MAIN", "NOT SUCCESS");
                    }
                });
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
                            binding.imageProfile.setImageBitmap(bitmap);
                            encodedImage= encodeImage(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

//signup control
    private Boolean isValidSignUpDetails() {
        if (binding.signupName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (encodedImage==null){showToast("Select profile image");
            return false;
        } else if (binding.signupEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.signupEmail.getText().toString()).matches()) {
            showToast("Enter Valid email");
            return false;
        } else if (binding.signupPhone.getText().toString().trim().isEmpty()){
            showToast("Enter phone");
            return false;
        } else if (!Patterns.PHONE.matcher(binding.signupPhone.getText().toString()).matches()) {
            showToast("Enter Valid phone");
            return false;
        } else if (binding.Password.getText().toString().trim().isEmpty()) {
            showToast("Enter Password");
            return false;
        } else if (binding.confirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter Confirm Password");
            return false;
        } else if (!binding.Password.getText().toString().equals(binding.confirmPassword.getText().toString())) {
            showToast("password not matched");
            return false;
        } else {
            return true;
        }
    }
   /* private void loading(Boolean isLoading){
        if (isLoading){
            binding.buttonCreate.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{

            binding.buttonCreate.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }*/
}



