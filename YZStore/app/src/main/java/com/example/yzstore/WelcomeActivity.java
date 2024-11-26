package com.example.yzstore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WelcomeActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 101; // Request code for Google Sign-In
    private static final String TAG = "WelcomeActivity";

    private ImageButton loginButton, signupButton, signupGoogleButton;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set up Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("464523214829-0cr4g6fq3g7tal1uagal37rosp31at35.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check if the user is logged in and handle accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserInDatabase(currentUser.getUid(), currentUser.getEmail());
        } else {
            setContentView(R.layout.activity_welcome); // Your layout with buttons
            // Set up UI components and listeners for login/signup buttons
            loginButton = findViewById(R.id.LoginButton);
            signupButton = findViewById(R.id.SignupButton);
            signupGoogleButton = findViewById(R.id.SignupGoogle);

            loginButton.setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, LoginActivity.class)));
            signupButton.setOnClickListener(v -> startActivity(new Intent(WelcomeActivity.this, SignupActivity.class)));
            signupGoogleButton.setOnClickListener(v -> signInWithGoogle());

            // Add touch effect for buttons
            addTouchEffect(loginButton);
            addTouchEffect(signupButton);
            addTouchEffect(signupGoogleButton);
        }
    }

    private void addTouchEffect(View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.2f); // Reduce opacity on press
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1f); // Reset opacity
                    break;
            }
            return false;
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(Exception.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (Exception e) {
                Log.e(TAG, "Google Sign-In failed: " + e.getMessage());
                Toast.makeText(this, "Google Sign-In failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if the email exists in Firebase database
                            checkUserInDatabase(user.getUid(), account.getEmail());
                        }
                    } else {
                        Log.e(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(WelcomeActivity.this, "Google Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInDatabase(String userId, String googleEmail) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        if (googleEmail != null && !googleEmail.isEmpty()) {
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean emailExists = false;

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);

                        if (googleEmail.equals(email)) {
                            emailExists = true;
                            break;
                        }
                    }

                    if (emailExists) {
                        // Email exists in the database, navigate to HomeActivity
                        Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear previous activities
                        startActivity(intent);
                    } else {
                        // Email does not exist in the database, navigate to EditProfileActivity
                        Intent intent = new Intent(WelcomeActivity.this, EditProfileActivity.class);
                        startActivity(intent);
                    }
                    finish();  // Close the current activity to prevent back navigation
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(WelcomeActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // No valid Google email, navigate to EditProfileActivity
            Intent intent = new Intent(WelcomeActivity.this, EditProfileActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // Method to log out from Firebase and Google
    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Google sign out successful.");
                Toast.makeText(WelcomeActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Google sign out failed: " + task.getException());
            }
        });

        // Redirect to the Login screen or show Google Sign-In button
        Intent intent = new Intent(WelcomeActivity.this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }
}
