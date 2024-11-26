package com.example.yzstore;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class OrderActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String orderId; // Set this order ID dynamically (will be fetched from Firebase)
    private String currentUserId; // To hold the current user's ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Initialize Firebase Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference("Orders");

        // Get current user ID using Firebase Authentication
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // UI elements
        ImageButton call = findViewById(R.id.CallButton);
        ImageButton complete = findViewById(R.id.CompleteButton);
        ImageButton cancel = findViewById(R.id.CancelButton);

        ImageView productImage = findViewById(R.id.ProductImage);
        TextView productPriceQuantity = findViewById(R.id.ProductPrice);
        TextView productName = findViewById(R.id.ProductName);
        TextView confirmTitle = findViewById(R.id.confirmTitle);
        TextView manufactureTitle = findViewById(R.id.manufactureTitle);
        TextView deliveringTitle = findViewById(R.id.deliveringTitle);
        TextView deliveredTitle = findViewById(R.id.deliveredTitle);
        TextView completedTitle = findViewById(R.id.completedTitle);
        TextView recalledTitle = findViewById(R.id.recalledTitle);
        TextView cancelledTitle = findViewById(R.id.cancelledTitle);
        TextView dateStart = findViewById(R.id.dateStart);
        TextView dateEnd = findViewById(R.id.dateEnd);
        TextView mainTitle = findViewById(R.id.mainTitle);

        // Set OnClickListener for mainTitle to refresh activity
        mainTitle.setOnClickListener(v -> refreshActivity());

        // Fetch all orders and check the current user ID to load the correct order
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String userId = orderSnapshot.child("UserId").getValue(String.class);
                    if (userId != null && userId.equals(currentUserId)) {
                        // Order ID will be dynamic based on the matched UserId
                        orderId = orderSnapshot.getKey();

                        // Retrieve product data for the matching order
                        String productNameText = orderSnapshot.child("ProductName").getValue(String.class);
                        String productPictureLink = orderSnapshot.child("ProductPictureLink").getValue(String.class);
                        String productPriceText = orderSnapshot.child("ProductPrice").getValue(String.class);
                        int Quantity = orderSnapshot.child("Quantity").getValue(Integer.class);
                        String startDate = orderSnapshot.child("StartDate").getValue(String.class);
                        String endDate = orderSnapshot.child("EndDate").getValue(String.class);
                        int orderStatus = orderSnapshot.child("OrderStatus").getValue(Integer.class);

                        // Set product image and price
                        Picasso.get().load(productPictureLink).into(productImage);
                        productPriceQuantity.setText(productPriceText + " Quantity: " + Quantity);
                        productName.setText(productNameText);

                        // Update start and end dates
                        dateStart.setText(formatDate(startDate));
                        dateEnd.setText(formatDate(endDate));

                        // Display the OrderStatus

                        // Update order status
                        updateOrderStatus(orderStatus, confirmTitle, manufactureTitle, deliveringTitle,
                                deliveredTitle, completedTitle, recalledTitle, cancelledTitle,
                                complete, cancel);

                        // Handle complete button intent
                        complete.setOnClickListener(v -> {
                            if (orderStatus == 5 || orderStatus == 7) {
                                showConfirmationDialog("Are you sure you want to complete this order?", () -> {
                                    Intent intent = new Intent(OrderActivity.this, CompleteActivity.class);
                                    intent.putExtra("OrderId", orderId); // Pass the orderId
                                    intent.putExtra("OrderStatus", orderStatus); // Pass the OrderStatus
                                    startActivity(intent);
                                });
                            }
                        });

                        cancel.setOnClickListener(v -> {
                            showConfirmationDialog("Are you sure you want to cancel this order?", () -> {
                                Intent intent = new Intent(OrderActivity.this, CompleteActivity.class);
                                intent.putExtra("OrderId", orderId); // Pass the orderId
                                intent.putExtra("OrderStatus", 7); // Cancel status (7)
                                startActivity(intent);
                            });
                        });


                        // Handle call button to open WhatsApp
                        call.setOnClickListener(v -> {
                            String sellerPhone = orderSnapshot.child("SellerPhone").getValue(String.class);

                            // Remove non-numeric characters from the phone number (e.g., spaces, hyphens)
                            if (sellerPhone != null) {
                                sellerPhone = sellerPhone.replaceAll("[^\\d]", ""); // Remove all non-digit characters

                                // Now the phone number should be in the correct format for WhatsApp
                                if (sellerPhone.length() >= 10) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + sellerPhone));
                                    startActivity(intent);
                                } else {
                                    // Handle the case where the phone number is still invalid or too short
                                    Toast.makeText(OrderActivity.this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle database read failure
            }
        });

        // Opacity effect for buttons
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

        call.setOnTouchListener(touchListener);
        complete.setOnTouchListener(touchListener);
        cancel.setOnTouchListener(touchListener);
        mainTitle.setOnTouchListener(touchListener);
    }

    // Helper method to refresh the activity
    private void refreshActivity() {
        Intent intent = getIntent();  // Get the current Intent
        finish();  // Finish the current activity
        startActivity(intent);  // Start the activity again with the same Intent
    }

    // Helper method to format date from Firebase
    private String formatDate(String date) {
        if (date == null || date.isEmpty()) {
            return " ";  // Return empty string if EndDate is null
        }

        String[] dateParts = date.split("/"); // Assuming date format is DD/MM/YYYY
        return dateParts[0] + "-" + dateParts[1] + "-" + dateParts[2].substring(2); // Format as DD-MM-YY
    }

    // Helper method to get order status as a text string
    private String getOrderStatusText(int orderStatus) {
        switch (orderStatus) {
            case 1: return "Confirmed";
            case 2: return "Manufacturing";
            case 3: return "Delivering";
            case 4: return "Delivered";
            case 5: return "Completed";
            case 6: return "Recalled";
            case 7: return "Cancelled";
            default: return "Unknown";
        }
    }

    // Helper method to update order status and UI elements accordingly
    private void updateOrderStatus(int orderStatus, TextView confirmTitle, TextView manufactureTitle,
                                   TextView deliveringTitle, TextView deliveredTitle, TextView completedTitle,
                                   TextView recalledTitle, TextView cancelledTitle, ImageButton completeButton,
                                   ImageButton cancelButton) {
        resetTextViewsOpacity(confirmTitle, manufactureTitle, deliveringTitle, deliveredTitle, completedTitle,
                recalledTitle, cancelledTitle);

        switch (orderStatus) {
            case 1:
                setOpacity(confirmTitle, 1f, "#FFFFFF");
                break;
            case 2:
                setOpacity(manufactureTitle, 1f, "#FFFFFF");
                break;
            case 3:
                setOpacity(deliveringTitle, 1f, "#FFFFFF");
                break;
            case 4:
                setOpacity(deliveredTitle, 1f, "#FFFFFF");
                break;
            case 5:
                setOpacity(completedTitle, 1f, "#85E801");  // RGB: 133, 232, 1
                break;
            case 6:
                setOpacity(recalledTitle, 1f, "#FFDE00");  // RGB: 255, 222, 0
                break;
            case 7:
                setOpacity(cancelledTitle, 1f, "#FD3030");  // RGB: 253, 48, 48
                break;
        }

        // Set opacity and enable the "Complete" button if status is 5
        completeButton.setAlpha((orderStatus == 5 || orderStatus == 7 ) ? 1f : 0.2f);
        completeButton.setEnabled(orderStatus == 5 || orderStatus == 7);

        // Set opacity and enable the "Cancel" button if status is 7
        cancelButton.setAlpha(orderStatus == 1 ? 1f : 0.2f);
        cancelButton.setEnabled(orderStatus == 1);
    }

    // Helper method to reset the opacity of all TextViews
    private void resetTextViewsOpacity(TextView... textViews) {
        for (TextView textView : textViews) {
            textView.setAlpha(0.2f); // Reset opacity to 0.2 (inactive state)
            textView.setTextColor(0xFFFFFFFF); // Set text color to white (#FFFFFF)

        }
    }

    private void showConfirmationDialog(String message, Runnable onConfirm) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> onConfirm.run())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }


    // Helper method to set opacity of specific TextViews
    private void setOpacity(TextView textView, float opacity, String color) {
        textView.setAlpha(opacity); // Set opacity
        textView.setTextColor(Color.parseColor(color)); // Set color
    }
}

