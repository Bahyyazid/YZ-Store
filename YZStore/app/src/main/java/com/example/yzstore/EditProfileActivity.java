package com.example.yzstore;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private StorageReference storageRef;

    private ImageView profilePic;
    private EditText userName;
    private EditText phoneNumber;
    private EditText email;
    private EditText address;

    private Uri selectedImageUri;
    private boolean imageChanged = false;

    private String currentProfilePicUrl = null; // To store the current profile picture URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase services
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_pics");

        FirebaseUser user = mAuth.getCurrentUser();

        // UI Elements
        ImageButton saveButton = findViewById(R.id.SaveButton);
        userName = findViewById(R.id.UserName);
        phoneNumber = findViewById(R.id.RecipientPhone);
        email = findViewById(R.id.Email);
        address = findViewById(R.id.RecipientAddress);
        profilePic = findViewById(R.id.ProfilePic);

        // Check if user is authenticated and autofill email field if authenticated
        if (user != null) {
            if (user.getEmail() != null) {
                email.setText(user.getEmail());
            }
            loadProfileData(user.getUid());
        } else {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
        }

        // Click listener for uploading profile image
        profilePic.setOnClickListener(v -> openImagePicker());

        // Save data on SaveButton click
        saveButton.setOnClickListener(v -> {
            String name = userName.getText().toString().trim();
            String phone = phoneNumber.getText().toString().trim();
            String addr = address.getText().toString().trim();
            String userEmail = email.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || addr.isEmpty() || userEmail.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (user != null && user.getUid() != null) {
                if (imageChanged) {
                    uploadImageToFirebase(user.getUid(), name, phone, addr, userEmail);
                } else {
                    saveProfileData(user.getUid(), name, phone, addr, userEmail, currentProfilePicUrl); // Use the current image URL if no new image
                }
            } else {
                Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*"); // Accept all image formats
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            profilePic.setImageURI(selectedImageUri); // Set selected image in ImageView
            imageChanged = true;
        }
    }

    private void uploadImageToFirebase(String userId, String name, String phone, String addr, String userEmail) {
        if (selectedImageUri == null) {
            // No image selected, just save data without updating profile picture
            saveProfileData(userId, name, phone, addr, userEmail, currentProfilePicUrl);
            return;
        }

        ContentResolver contentResolver = getContentResolver();
        String mimeType = contentResolver.getType(selectedImageUri);
        String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        if (fileExtension == null || fileExtension.isEmpty()) {
            Toast.makeText(this, "Invalid file type!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = userId + "." + fileExtension;
        StorageReference userProfilePicRef = storageRef.child(fileName);

        userProfilePicRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> userProfilePicRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Log.d("Upload", "Image uploaded successfully: " + uri.toString());
                            saveProfileData(userId, name, phone, addr, userEmail, uri.toString());
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Upload", "Failed to get download URL: " + e.getMessage());
                            Toast.makeText(this, "Failed to get image URL.", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    Log.e("Upload", "Image upload failed: " + e.getMessage());
                    Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfileData(String userId, String name, String phone, String addr, String email, String profilePicUrl) {
        try {
            DatabaseReference userRef = mDatabase.getReference("users").child(userId);

            // If no image is selected, use the existing profile picture URL
            if (profilePicUrl == null) {
                profilePicUrl = currentProfilePicUrl; // Use the current profile picture URL if no new image is selected
            }

            UserProfile userProfile = new UserProfile(name, phone, addr, email, profilePicUrl);

            userRef.setValue(userProfile)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileData(String userId) {
        DatabaseReference userRef = mDatabase.getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);

                if (userProfile != null) {
                    userName.setText(userProfile.getName());
                    phoneNumber.setText(userProfile.getPhone());
                    address.setText(userProfile.getAddress());
                    email.setText(userProfile.getEmail());

                    // Store the current profile picture URL
                    currentProfilePicUrl = userProfile.getProfilePic();

                    if (currentProfilePicUrl != null && !currentProfilePicUrl.isEmpty()) {
                        Glide.with(EditProfileActivity.this)
                                .load(currentProfilePicUrl)
                                .placeholder(R.drawable.nopicture) // Optional placeholder image
                                .error(R.drawable.nopicture)       // Optional error image
                                .into(profilePic);
                    }
                } else {
                    Toast.makeText(EditProfileActivity.this, "Profile data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                Log.e("EditProfile", "Error: " + databaseError.getMessage());
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public static class UserProfile {
        private String name;
        private String phone;
        private String address;
        private String email;
        private String profilePic;

        public UserProfile() {
            // Default constructor required for Firebase
        }

        public UserProfile(String name, String phone, String address, String email, String profilePic) {
            this.name = name;
            this.phone = phone;
            this.address = address;
            this.email = email;
            this.profilePic = profilePic;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        public String getAddress() {
            return address;
        }

        public String getEmail() {
            return email;
        }

        public String getProfilePic() {
            return profilePic;
        }
    }
}
