package com.ecommerce.service;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(String username, OrderRequest request);
    OrderResponse getOrderById(String username, Long orderId);
    List<OrderResponse> getUserOrders(String username);
    List<OrderResponse> getAllOrders();
    OrderResponse updateOrderStatus(Long orderId, String status);
}
