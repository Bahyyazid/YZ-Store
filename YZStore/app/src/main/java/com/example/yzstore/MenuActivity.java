package com.example.yzstore;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class MenuActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient; // Declare GoogleSignInClient

    private ImageView profilePic;
    private TextView profileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // Find views
        profilePic = findViewById(R.id.ProfilePic);
        profileName = findViewById(R.id.ProfileName);
        TextView myOrder = findViewById(R.id.MyOrderButton);
        TextView editProfile = findViewById(R.id.EditProfileButton);
        TextView about = findViewById(R.id.AboutButton);
        TextView logout = findViewById(R.id.LogoutButton);

        FirebaseUser userOrder = mAuth.getCurrentUser(); // Use mAuth to get the current user

        // Set touch listener for opacity effect
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setAlpha(0.2f);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.setAlpha(1f);
                        break;
                }
                return false; // Continue with default click behavior
            }
        };

        // Apply the touch listener to each button
        myOrder.setOnTouchListener(touchListener);
        editProfile.setOnTouchListener(touchListener);
        about.setOnTouchListener(touchListener);
        logout.setOnTouchListener(touchListener);

        // Set click listener to go to EditProfileActivity
        editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        myOrder.setOnClickListener(v -> {
            checkCurrentUser(userOrder);
        });

        // Set click listener to log out the user
        logout.setOnClickListener(v -> logoutUser());

        // Fetch the current user from FirebaseAuth
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Get the UID of the currently authenticated user
            String userId = user.getUid();

            // Load profile picture and name from Firebase
            loadProfilePic(userId);
            loadProfileName(userId);
        } else {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCurrentUser(FirebaseUser userOrder) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        ordersRef.get().addOnSuccessListener(snapshot -> {
            boolean userHasOrder = false;

            if (snapshot.exists()) {
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String existingUserId = orderSnapshot.child("UserId").getValue(String.class);
                    if (existingUserId != null && existingUserId.equals(userOrder.getUid())) {
                        userHasOrder = true;
                        break;
                    }
                }
            }

            if (userHasOrder) {
                Intent intent = new Intent(MenuActivity.this, OrderActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MenuActivity.this, "You don't have an order yet.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(MenuActivity.this, "Error checking orders: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    // Method to log out the user
    private void logoutUser() {
        // Create an AlertDialog for logout confirmation
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // Sign out from Google
                    googleSignInClient.signOut()
                            .addOnCompleteListener(this, task -> {
                                // Show a toast message for successful logout
                                Toast.makeText(MenuActivity.this, "Logged out", Toast.LENGTH_SHORT).show();

                                // Redirect to the login screen or splash screen
                                Intent intent = new Intent(MenuActivity.this, WelcomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Dismiss the dialog if user clicks No
                    dialog.dismiss();
                })
                .setCancelable(false) // Optional: Prevent dialog from being dismissed by tapping outside
                .show();
    }

    // Load the profile picture URL from Firebase Realtime Database
    private void loadProfilePic(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String profilePicUrl = dataSnapshot.child("profilePic").getValue(String.class);
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    // Use Glide to load the profile picture
                    Glide.with(MenuActivity.this)
                            .load(profilePicUrl)
                            .placeholder(R.drawable.nopicture) // Optional placeholder image
                            .error(R.drawable.nopicture)       // Optional error image
                            .into(profilePic);
                } else {
                    Toast.makeText(MenuActivity.this, "Profile picture not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MenuActivity.this, "Failed to load profile picture", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Load the profile name from Firebase Realtime Database
    private void loadProfileName(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue(String.class);
                if (name != null) {
                    profileName.setText(name);
                } else {
                    Toast.makeText(MenuActivity.this, "Profile name not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MenuActivity.this, "Failed to load profile name", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
