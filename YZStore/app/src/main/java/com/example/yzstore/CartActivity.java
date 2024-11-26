package com.example.yzstore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CartActivity extends AppCompatActivity {

    private ShapeableImageView profilePic;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Retrieve passed product details from Intent
        String productName = getIntent().getStringExtra("productName");
        String productPictureLink = getIntent().getStringExtra("productPictureLink");
        String productPrice = getIntent().getStringExtra("productPrice");
        String maxQuantityStr = getIntent().getStringExtra("MaxQuantity");

        int maxQuantity = maxQuantityStr != null ? Integer.parseInt(maxQuantityStr) : 0;

        // Find views in the layout
        ImageButton placeOrder = findViewById(R.id.OrderButton);
        profilePic = findViewById(R.id.ProfilePic);
        ImageView productImage = findViewById(R.id.ProductImage);
        TextView quantityView = findViewById(R.id.Quantity);
        TextView productNameView = findViewById(R.id.ProductName);
        TextView productPriceView = findViewById(R.id.ProductPrice);
        TextView recipientNameView = findViewById(R.id.RecipientName);
        TextView recipientPhoneView = findViewById(R.id.RecipientPhone);
        TextView recipientAddressView = findViewById(R.id.RecipientAddress);

        // Set product details dynamically
        Glide.with(this)
                .load(productPictureLink)
                .placeholder(R.drawable.nopicture)
                .error(R.drawable.nopicture)
                .into(productImage);
        productNameView.setText(productName);
        productPriceView.setText(productPrice);
        quantityView.setHint("Max. " + maxQuantityStr);

        // Load user details and profile picture from Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String profilePicUrl = snapshot.child("profilePic").getValue(String.class);

                    recipientNameView.setText(name != null ? name : "N/A");
                    recipientPhoneView.setText(phone != null ? phone : "N/A");
                    recipientAddressView.setText(address != null ? address : "N/A");

                    // Load profile picture
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(CartActivity.this)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.nopicture)
                                .error(R.drawable.nopicture)
                                .into(profilePic);
                    }
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(CartActivity.this, "Failed to load user details: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }

        // Touch listener for button opacity effects
        View.OnTouchListener touchListener = (view, motionEvent) -> {
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
        };

        placeOrder.setOnTouchListener(touchListener);
        profilePic.setOnTouchListener(touchListener);

        // Set click listener for profile icon to start new activity
        profilePic.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        // Set click listener for place order button
        placeOrder.setOnClickListener(v -> {
            // Validate all fields
            String quantityStr = quantityView.getText().toString().trim();
            String recipientName = recipientNameView.getText().toString().trim();
            String recipientPhone = recipientPhoneView.getText().toString().trim();
            String recipientAddress = recipientAddressView.getText().toString().trim();

            if (quantityStr.isEmpty() || recipientName.isEmpty() || recipientPhone.isEmpty() || recipientAddress.isEmpty()) {
                Toast.makeText(CartActivity.this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            if (quantity > maxQuantity) {
                Toast.makeText(CartActivity.this, "Quantity exceeds maximum allowed.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Orders in the database
            checkAndPlaceOrder(productPictureLink, productName, productPrice, recipientName, recipientPhone, recipientAddress, quantity, currentUser);
        });
    }

    private void checkAndPlaceOrder(String productPictureLink, String productName, String productPrice, String recipientName,
                                    String recipientPhone, String recipientAddress, int quantity, FirebaseUser currentUser) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        ordersRef.get().addOnSuccessListener(snapshot -> {
            boolean userHasOrder = false;

            if (snapshot.exists()) {
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String existingUserId = orderSnapshot.child("UserId").getValue(String.class);
                    if (existingUserId != null && existingUserId.equals(currentUser.getUid())) {
                        userHasOrder = true;
                        break;
                    }
                }
            }

            if (userHasOrder) {
                Toast.makeText(CartActivity.this, "You already have an active order. Complete or cancel it before placing another.", Toast.LENGTH_SHORT).show();
            } else {
                createOrder(ordersRef, productPictureLink, currentUser.getUid(), productName, productPrice, recipientName, recipientPhone, recipientAddress, quantity);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(CartActivity.this, "Error checking orders: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void createOrder(DatabaseReference ordersRef, String productPictureLink, String userId, String productName,
                             String productPrice, String recipientName, String recipientPhone, String recipientAddress, int quantity) {
        String orderId = UUID.randomUUID().toString();
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        DatabaseReference newOrderRef = ordersRef.child(orderId);
        newOrderRef.child("ProductPictureLink").setValue(productPictureLink);
        newOrderRef.child("UserId").setValue(userId);
        newOrderRef.child("ProductName").setValue(productName);
        newOrderRef.child("ProductPrice").setValue(productPrice);
        newOrderRef.child("RecipientName").setValue(recipientName);
        newOrderRef.child("RecipientPhone").setValue(recipientPhone);
        newOrderRef.child("RecipientAddress").setValue(recipientAddress);
        newOrderRef.child("Quantity").setValue(quantity);
        newOrderRef.child("OrderStatus").setValue(1); // Default order status
        newOrderRef.child("StartDate").setValue(currentDate);

        Toast.makeText(CartActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();

        // Navigate to OrderActivity
        Intent intent = new Intent(CartActivity.this, OrderActivity.class);
        startActivity(intent);
    }
}
