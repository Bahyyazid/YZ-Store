package com.example.yzstore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView categoryRecyclerView;
    private CategoriesAdapter categoriesAdapter;
    private List<Categories> categoryList;

    private String item;
    private ImageView profilePic;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firebaseAuth = FirebaseAuth.getInstance();

        categoryRecyclerView = findViewById(R.id.CategoryRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryList = new ArrayList<>();
        categoriesAdapter = new CategoriesAdapter(this, categoryList);
        categoryRecyclerView.setAdapter(categoriesAdapter);

        profilePic = findViewById(R.id.ProfilePic);
        setupProfilePicTouchEffect();
        setupProfilePicIntent();
        loadProfilePicture();
        loadCategoriesFromFirebase();
    }

    private void setupProfilePicTouchEffect() {
        profilePic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    profilePic.setAlpha(0.2f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    profilePic.setAlpha(1f);
                    break;
            }
            return false;
        });
    }

    private void setupProfilePicIntent() {
        profilePic.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MenuActivity.class);
            startActivity(intent);
        });
    }

    private void loadProfilePicture() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String profilePicUrl = snapshot.child("profilePic").getValue(String.class);
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(HomeActivity.this)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.nopicture)
                                .error(R.drawable.nopicture)
                                .into(profilePic);
                    } else {
                        Log.e("Firebase", "Profile picture URL not found for user: " + userId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Failed to load profile picture: " + error.getMessage());
                }
            });
        } else {
            Log.e("FirebaseAuth", "No authenticated user found.");
        }
    }

    private void loadCategoriesFromFirebase() {
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("Categories");
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("Products");

        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot categoriesSnapshot) {
                HashMap<String, Integer> productCounts = new HashMap<>();

                productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot productsSnapshot) {
                        for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                            String productCategory = productSnapshot.child("ProductCategory").getValue(String.class);
                            if (productCategory != null) {
                                productCounts.put(productCategory, productCounts.getOrDefault(productCategory, 0) + 1);
                            }
                        }

                        for (DataSnapshot categorySnapshot : categoriesSnapshot.getChildren()) {
                            String categoryId = categorySnapshot.getKey();
                            String categoryName = categorySnapshot.child("CategoryName").getValue(String.class);
                            String categoryPictureLink = categorySnapshot.child("CategoryPictureLink").getValue(String.class);

                            int stockRemaining = productCounts.getOrDefault(categoryId, 0);

                            if (categoryName != null && categoryPictureLink != null) {
                                if (stockRemaining > 1) {
                                    item = " Products";
                                } else if (stockRemaining <= 1){
                                    item = " Product";
                                }
                                categoryList.add(new Categories(categoryName, categoryPictureLink, stockRemaining + item, categoryId));
                            }
                        }

                        categoriesAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("Firebase", "Failed to load products: " + databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to load categories: " + databaseError.getMessage());
            }
        });
    }
}

