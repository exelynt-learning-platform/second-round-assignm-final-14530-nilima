package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    // Stripe payment method token from frontend
    private String paymentMethodId;

    // Currency, default USD
    private String currency = "usd";
}
