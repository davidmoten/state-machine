package model;


public final class BasketProduct {

    public final String basketId;
    public final String productId;
    public final int quantity;

    public BasketProduct(String basketId, String productId, int quantity) {
        this.basketId = basketId;
        this.productId = productId;
        this.quantity = quantity;
    }

}
