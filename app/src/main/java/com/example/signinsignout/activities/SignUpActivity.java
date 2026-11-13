package com.example.signinsignout.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.signinsignout.R;
import com.example.signinsignout.databinding.ActivitySignInBinding;
import com.example.signinsignout.databinding.ActivitySignUpBinding;
import com.example.signinsignout.utilities.Constants;
import com.example.signinsignout.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;

    private PreferenceManager preferenceManager;

    private String encodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up binding
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setListeners();
    }

    /**
     * add function to sign in, sign up and image
     * sign in - sends user back to sign in page
     * sign up - sends user account details to firebase if valid and display toast message
     * image - allows user to choose image from photo downloads for account
     */
    private void setListeners() {
        binding.textSignIn.setOnClickListener(
                v -> onBackPressed());

        binding.buttonSignUp.setOnClickListener(v -> {
            if(isValidateSignUpDetails()){
                SignUp();
                showToast("Sign Up Successful");
            }
        });

        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    /**
     * set up toast to display message
     * @param message message to be displayed
     */
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * send account details to firebase as a user hashmap and display message if it fails
     */
    private void SignUp(){
        //check loading
        loading(true);
        
        //post to firebase
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String,String> user = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME,binding.inputFirstName.getText().toString());
        user.put(Constants.KEY_LAST_NAME,binding.inputLastName.getText().toString());
        user.put(Constants.KEY_EMAIL,binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString());

        user.put(Constants.KEY_IMAGE,encodeImage);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);

                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                    preferenceManager.putString(Constants.KEY_FIRST_NAME,binding.inputFirstName.getText().toString());
                    preferenceManager.putString(Constants.KEY_LAST_NAME,binding.inputLastName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE,encodeImage);

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);


                }).addOnFailureListener(exception ->{
                    loading(false);
                    showToast(exception.getMessage());
            });

    }

    /**
     * encode image into a string to be stored in firebase
     * @param bitmap image
     * @return image as string
     */
    private String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight()*previewWidth / bitmap.getWidth();

        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);

        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }

    /**
     * take image string as stored in firebase and view
     */
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->{
                if(result.getResultCode() == RESULT_OK){
                    Uri imageUrl = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUrl);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        binding.imageProfile.setImageBitmap(bitmap);
                        binding.textAddImage.setVisibility(View.GONE);
                        encodeImage = encodeImage(bitmap);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            }
    );

    /**
     * make sure each field is filled and correct, send error message if not
     * @return boolean
     */
    private Boolean isValidateSignUpDetails(){
        if (encodeImage == null){
            showToast("Please Select An Image");
            return false;
        }
        if (binding.inputFirstName.getText().toString().trim().isEmpty()){
            showToast("Please Enter Your First Name");
            return false;
        } else if (binding.inputLastName.getText().toString().trim().isEmpty()) {
            showToast("Please Enter Your Last Name");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Please Enter Your Email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Please Enter Valid Email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Please Enter Your Password");
            return false;
        } else if (binding.inputconfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Please Confirm Your Password");
            return false;
        } else if (!binding.inputPassword.getText().toString().equals((binding.inputconfirmPassword.getText().toString()))) {
            showToast("Passwords Do Not Match");
            return false;
        } else {
            return true;
        }

    }

    /**
     * set isLoading to true or false and adjust button and progress bar
     * @param isLoading
     */
    private void loading (Boolean isLoading) {
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

}
