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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CatalogActivity extends AppCompatActivity {

    private RecyclerView productRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList;

    private ImageView profilePic;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        productRecyclerView = findViewById(R.id.ProductRecyclerView);
        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        productAdapter = new ProductAdapter(this, productList);
        productRecyclerView.setAdapter(productAdapter);

        profilePic = findViewById(R.id.ProfilePic);
        setupProfilePicTouchEffect();
        setupProfilePicIntent();
        loadProfilePicture();
        loadProductsFromFirebase();
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
            Intent intent = new Intent(CatalogActivity.this, MenuActivity.class);
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
                        Glide.with(CatalogActivity.this)
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

    private void loadProductsFromFirebase() {
        String categoryId = getIntent().getStringExtra("categoryId"); // Retrieve the passed categoryId

        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("Products");

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot productsSnapshot) {
                for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                    String MaxQuantity = productSnapshot.child("MaxQuantity").getValue(String.class);
                    String productCategory = productSnapshot.child("ProductCategory").getValue(String.class);
                    String productName = productSnapshot.child("ProductName").getValue(String.class);
                    String productPictureLink = productSnapshot.child("ProductPictureLink").getValue(String.class);
                    String productPrice = productSnapshot.child("ProductPrice").getValue(String.class);
                    boolean isIDR = productSnapshot.child("isIDR").getValue(Boolean.class) != null && productSnapshot.child("isIDR").getValue(Boolean.class);
                    boolean isSoldOut = productSnapshot.child("isSoldOut").getValue(Boolean.class) != null && productSnapshot.child("isSoldOut").getValue(Boolean.class);

                    // Filter products by categoryId
                    if (productCategory != null && productCategory.equals(categoryId)) {
                        String formattedPrice = formatPrice(productPrice, isIDR);

                        // Add product to the list
                        if (productName != null && productPictureLink != null) {
                            productList.add(new Product(MaxQuantity, productCategory, productName, productPictureLink, formattedPrice, isSoldOut));
                        }
                    }
                }

                // Notify adapter of data changes
                productAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to load products: " + databaseError.getMessage());
            }
        });
    }



    // Helper method to format the price in CatalogActivity
    private String formatPrice(String price, boolean isIDR) {
        try {
            int priceValue = Integer.parseInt(price);
            String currencySymbol = isIDR ? "Rp " : "$ ";

            // Format the price with dots as thousand separators
            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            formatter.applyPattern("#,###");
            String formattedPrice = formatter.format(priceValue).replace(",", ".");
            return currencySymbol + formattedPrice;
        } catch (NumberFormatException e) {
            return price; // Return the original price if parsing fails
        }
    }
}