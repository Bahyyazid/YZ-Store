package com.example.yzstore;

public class Categories {
    private String name;
    private String imageUrl;
    private String stockDescription;
    private String categoryId; // Add this field

    public Categories(String name, String imageUrl, String stockDescription, String categoryId) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.stockDescription = stockDescription;
        this.categoryId = categoryId; // Initialize it
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStockDescription() {
        return stockDescription;
    }

    public String getCategoryId() {
        return categoryId; // Getter for categoryId
    }
}
