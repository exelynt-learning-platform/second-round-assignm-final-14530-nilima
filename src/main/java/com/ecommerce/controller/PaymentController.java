package com.ecommerce.controller;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

  
    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPaymentIntent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPaymentIntent(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment intent created"));
    }

    @PostMapping("/confirm/{paymentIntentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String paymentIntentId) {
        PaymentResponse response = paymentService.confirmPayment(
                userDetails.getUsername(), paymentIntentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment confirmed"));
    }

    
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        PaymentResponse response = paymentService.refundPayment(
                userDetails.getUsername(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Refund processed successfully"));
    }
}
