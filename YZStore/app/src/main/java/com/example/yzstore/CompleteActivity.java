package com.example.yzstore;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CompleteActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete);

        // Initialize Firebase reference
        mDatabase = FirebaseDatabase.getInstance().getReference("Orders");

        // Retrieve the intent
        Intent intent = getIntent();

        // Get the OrderId and OrderStatus from the intent
        String orderId = intent.getStringExtra("OrderId"); // Retrieve OrderId
        int orderStatus = intent.getIntExtra("OrderStatus", 1); // Default value is 1 if not found

        // Use the data as needed, for example, updating the UI with the retrieved values
        ImageButton backButton = findViewById(R.id.BackButton);
        ImageView mainIcon = findViewById(R.id.mainicon);
        TextView maintitle = findViewById(R.id.mainTitle);
        TextView maintitle2 = findViewById(R.id.mainTitle2);

        if(orderStatus != 7){
            mainIcon.setImageResource(R.drawable.completeicon);
            maintitle.setText("Order Completed");
            maintitle2.setText("Thanks for using our Service!");
        } else {
            mainIcon.setImageResource(R.drawable.canceled);
            maintitle.setText("Order Canceled");
            maintitle2.setText("Sorry for our Inconvenience!");
        }

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
        backButton.setOnTouchListener(touchListener);

        // Set click listeners to start new activities and delete the order
        backButton.setOnClickListener(v -> {
            // Delete the order from Firebase based on the OrderId
            if (orderId != null) {
                deleteOrder(orderId);
            }

            // Navigate to HomeActivity after deletion
            Intent homeIntent = new Intent(CompleteActivity.this, HomeActivity.class);
            startActivity(homeIntent);
        });
    }

    private void deleteOrder(String orderId) {
        // Find the order in the Firebase database using the OrderId
        mDatabase.child(orderId).removeValue().addOnCompleteListener(task -> {
        });
    }
}
