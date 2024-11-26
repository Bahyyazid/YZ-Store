package com.example.yzstore;

public class Product {

    private String MaxQuantity;
    private String category;
    private String name;
    private String imageUrl;
    private String formattedPrice;
    private boolean isSoldOut;

    public Product(String MaxQuantity, String category, String name, String imageUrl, String formattedPrice, boolean isSoldOut) {

        this.MaxQuantity = MaxQuantity;
        this.category = category;
        this.name = name;
        this.imageUrl = imageUrl;
        this.formattedPrice = formattedPrice;
        this.isSoldOut = isSoldOut;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public boolean isSoldOut() {
        return isSoldOut;
    }

    public String getMaxQuantity(){ return MaxQuantity;}
}
