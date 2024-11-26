package com.example.yzstore;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignupActivity extends AppCompatActivity {

    private static final int GOOGLE_SIGN_IN = 100;
    private FirebaseAuth firebaseAuth;
    private ImageButton signupButton;
    private ImageButton signupGoogleButton;

    private boolean isPasswordVisible = false;
    private boolean isConPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        ImageButton seeButton = findViewById(R.id.SeeButton);
        ImageButton conSeeButton = findViewById(R.id.ConSeeButton);
        EditText passwordText = findViewById(R.id.Password);
        EditText conPasswordText = findViewById(R.id.ConfirmPassword);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Google Sign-In button setup
        signupGoogleButton = findViewById(R.id.SignupGoogle);
        signupGoogleButton.setOnClickListener(v -> signInWithGoogle());

        // Signup with email and password
        signupButton = findViewById(R.id.SignupButton);
        signupButton.setOnClickListener(v -> {
            String email = ((TextView) findViewById(R.id.Email)).getText().toString().trim();
            String password = ((TextView) findViewById(R.id.Password)).getText().toString().trim();
            String userName = ((TextView) findViewById(R.id.UserName)).getText().toString().trim();
            String phoneNumber = ((TextView) findViewById(R.id.RecipientPhone)).getText().toString().trim();

            // Check if all fields are filled
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(userName) || TextUtils.isEmpty(phoneNumber)) {
                Toast.makeText(SignupActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if email exists in Firebase
            checkEmailExists(email, password, userName, phoneNumber);
        });

        seeButton.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                seeButton.setImageResource(R.drawable.invisible);
                conPasswordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                conSeeButton.setImageResource(R.drawable.invisible);
            } else {
                passwordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                seeButton.setImageResource(R.drawable.visible);
                conPasswordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                conSeeButton.setImageResource(R.drawable.visible);
            }
            isPasswordVisible = !isPasswordVisible;
        });

        conSeeButton.setOnClickListener(v -> {
            if (isConPasswordVisible) {
                passwordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                seeButton.setImageResource(R.drawable.invisible);
                conPasswordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                conSeeButton.setImageResource(R.drawable.invisible);
            } else {
                passwordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                seeButton.setImageResource(R.drawable.visible);
                conPasswordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                conSeeButton.setImageResource(R.drawable.visible);
            }
            isConPasswordVisible = !isConPasswordVisible;
        });

        // Adding touch effects to buttons
        addTouchEffect(signupGoogleButton);
        addTouchEffect(seeButton);
        addTouchEffect(conSeeButton);
        addTouchEffect(signupButton);
    }

    private void addTouchEffect(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.2f); // Set 20% opacity on press
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1f); // Set 100% opacity on release
                        break;
                }
                return false;
            }
        });
    }

    private void signInWithGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("464523214829-0cr4g6fq3g7tal1uagal37rosp31at35.apps.googleusercontent.com")
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    private void checkEmailExists(String email, String password, String userName, String phoneNumber) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(SignupActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                } else {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(SignupActivity.this, task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                    if (firebaseUser != null) {
                                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid());
                                        userRef.child("email").setValue(email);
                                        userRef.child("password").setValue(password);
                                        userRef.child("name").setValue(userName);
                                        userRef.child("phone").setValue(phoneNumber);

                                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                } else {
                                    Exception exception = task.getException();
                                    if (exception != null) {
                                        Toast.makeText(SignupActivity.this, "Signup failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SignupActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN) {
            if (data != null) {
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
                    if (account != null) {
                        firebaseAuthWithGoogle(account);
                    } else {
                        Toast.makeText(this, "Google Sign-In failed!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Google Sign-In failed. No data received.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        if (account == null) {
            Toast.makeText(SignupActivity.this, "Google Sign-In failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid());
                    userRef.child("email").setValue(firebaseUser.getEmail());
                    userRef.child("name").setValue(firebaseUser.getDisplayName());
                    userRef.child("phone").setValue(firebaseUser.getPhoneNumber());
                }
                startActivity(new Intent(SignupActivity.this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(SignupActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
