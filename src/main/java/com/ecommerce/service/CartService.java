package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;

public interface CartService {
    CartResponse getCartByUser(String username);
    CartResponse addItemToCart(String username, CartItemRequest request);
    CartResponse updateCartItem(String username, Long itemId, CartItemRequest request);
    CartResponse removeItemFromCart(String username, Long itemId);
    void clearCart(String username);
}
