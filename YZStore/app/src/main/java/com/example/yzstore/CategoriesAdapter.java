package com.example.yzstore;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoriesViewHolder> {

    private Context context;
    private List<Categories> categoryList;

    public CategoriesAdapter(Context context, List<Categories> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public CategoriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_content, parent, false);
        return new CategoriesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriesViewHolder holder, int position) {
        Categories category = categoryList.get(position);
        holder.categoryName.setText(category.getName());
        holder.categoryStock.setText(category.getStockDescription());

        // Load image using Glide
        Glide.with(context)
                .load(category.getImageUrl())
                .placeholder(R.drawable.nopicture)
                .error(R.drawable.nopicture)
                .into(holder.categoryImage);

        // Set touch listener for opacity effect
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    holder.itemView.setAlpha(0.2f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holder.itemView.setAlpha(1f);
                    break;
            }
            return false;
        });

        // Set click listener for intent navigation with categoryId
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CatalogActivity.class);
            intent.putExtra("categoryId", category.getCategoryId()); // Pass categoryId
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class CategoriesViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;
        TextView categoryStock;
        ImageView categoryImage;

        public CategoriesViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.ProductName);
            categoryImage = itemView.findViewById(R.id.ProductImage);
            categoryStock = itemView.findViewById(R.id.ProductPrice);
        }
    }
}
