package com.ecommerce.service;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPaymentIntent(String username, PaymentRequest request);
    PaymentResponse confirmPayment(String username, String paymentIntentId);
    PaymentResponse refundPayment(String username, Long orderId);
}
