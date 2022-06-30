package com.example.chat.Activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;
import com.example.chat.databinding.ActivitySignInBinding;
import com.example.chat.utilities.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.chat.utilities.PreferenceManager;

public class SignInActivity extends AppCompatActivity {
private ActivitySignInBinding binding;
private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        //acıksa tekrar sıgnın ekranına gırmıyor
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();

        Constants.sharedPreferences = getSharedPreferences(Constants.PREFERENCE_KEY, 0);
        Constants.editor = Constants.sharedPreferences.edit();
    }
    private void setListeners(){
        binding.btnCreate.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(),SignUpActivity.class)));
    /*    binding.textViewForget.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(),ForgetActivity.class)));*/
         binding.btnSign.setOnClickListener(v -> {

             if (isValidSignInDetails()){
                 signIn();
         }
    });
}


    private void signIn(){
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL,binding.signinEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PHONE,binding.signinPhone.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString())
                .get()
                        .addOnCompleteListener(task -> {

                            if (      task.isSuccessful()
                                    &&task.getResult() !=null
                                    &&task.getResult().getDocuments().size()>0){
                                DocumentSnapshot documentSnapshot=task.getResult().getDocuments().get(0);
                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                preferenceManager.putString(Constants.KEY_USER_ID,documentSnapshot.getId());
                                preferenceManager.putString(Constants.KEY_PHONE,documentSnapshot.getString(Constants.KEY_PHONE));
                                preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                                preferenceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE));
                                Intent intent=new Intent(getApplicationContext(), HomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }else{
                                showToast("Unable to sign in");
                            }
                });

    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    private boolean isValidSignInDetails() {

        if (binding.signinEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.signinEmail.getText().toString()).matches()) {
            showToast("Enter Valid email");
            return false;
        } else if (binding.signinPhone.getText().toString().trim().isEmpty()){
            showToast("Enter Phone");
            return false;
       } else if (!Patterns.PHONE.matcher(binding.signinPhone.getText().toString()).matches()) {
            showToast("Enter Valid phone");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter Password");
            return false;
        } else {
            return true;
        }
    }
}



