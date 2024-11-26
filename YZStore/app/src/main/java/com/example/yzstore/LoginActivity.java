package com.example.yzstore;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private boolean isPasswordVisible = false;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        TextView emailTextView = findViewById(R.id.Email);
        TextView passwordTextView = findViewById(R.id.Password);
        ImageButton loginButton = findViewById(R.id.LoginButton);
        ImageButton seeButton = findViewById(R.id.SeeButton);

        addTouchEffect(loginButton);
        addTouchEffect(seeButton);



        seeButton.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide the password
                passwordTextView.setTransformationMethod(PasswordTransformationMethod.getInstance());
                seeButton.setImageResource(R.drawable.invisible); // Change to invisible icon
            } else {
                // Show the password
                passwordTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                seeButton.setImageResource(R.drawable.visible); // Change to visible icon
            }
            isPasswordVisible = !isPasswordVisible; // Toggle the visibility state
        });

        loginButton.setOnClickListener(v -> {
            String email = emailTextView.getText().toString().trim();
            String password = passwordTextView.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Authenticate the user with email and password
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check if user exists in the database
                                databaseReference.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            // Retrieve user data (phone number) from the database
                                            String phoneNumber = snapshot.child("phone").getValue(String.class);

                                            // Proceed with the login process, now using the data from Firebase
                                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                            intent.putExtra("phoneNumber", phoneNumber);  // Pass the phone number to the HomeActivity
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            // Show a toast if email or password is incorrect
                            Toast.makeText(LoginActivity.this, "Email or password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
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
}
