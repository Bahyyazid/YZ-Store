package com.example.yzstore;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_content, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        // Set product name
        holder.productName.setText(product.getName());

        // Handle sold-out state and price formatting
        if (product.isSoldOut()) {
            holder.productPrice.setText("Out of Stock");
            holder.productPrice.setTextColor(Color.rgb(253, 48, 48)); // Red font for sold out
        } else {
            holder.productPrice.setText(product.getFormattedPrice());
            holder.productPrice.setTextColor(Color.rgb(0, 0, 0)); // Default black font
        }

        // Load product image using Glide
        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.nopicture)
                .error(R.drawable.nopicture)
                .into(holder.productImage);

        // Set touch listener for opacity effect
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    holder.itemView.setAlpha(0.2f); // Set opacity to 20% on press
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holder.itemView.setAlpha(1f); // Reset to 100% opacity on release
                    break;
            }
            return false; // Allow click events to propagate
        });

        // Set click listener for intent navigation
        holder.itemView.setOnClickListener(v -> {
            if (!product.isSoldOut()) {
                Intent intent = new Intent(context, CartActivity.class);
                intent.putExtra("productName", product.getName());
                intent.putExtra("productPictureLink", product.getImageUrl());
                intent.putExtra("productPrice", product.getFormattedPrice());
                intent.putExtra("MaxQuantity", product.getMaxQuantity());
                context.startActivity(intent);
            } else {
                // Optionally show a message that the product is sold out
                Toast.makeText(context, "This product is out of stock.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView productPrice;
        ImageView productImage;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.ProductName);
            productImage = itemView.findViewById(R.id.ProductImage);
            productPrice = itemView.findViewById(R.id.ProductPrice);
        }
    }
}