package com.ecommerce.service.impl;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Override
    @Transactional
    public PaymentResponse createPaymentIntent(String username, PaymentRequest request) {
        User user = getUser(username);
        Order order = getOrderForUser(request.getOrderId(), user);

        if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED) {
            throw new PaymentException("Order has already been paid");
        }

        try {
            // Convert to cents for Stripe
            long amountInCents = order.getTotalPrice()
                    .multiply(new java.math.BigDecimal("100"))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.getCurrency() != null ? request.getCurrency() : "usd")
                    .setDescription("Order #" + order.getId() + " for user: " + username)
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .putMetadata("userId", String.valueOf(user.getId()))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            order.setPaymentIntentId(intent.getId());
            orderRepository.save(order);

            return PaymentResponse.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .status(intent.getStatus())
                    .message("Payment intent created successfully")
                    .orderId(order.getId())
                    .build();

        } catch (StripeException e) {
            // For demo/testing without real Stripe key, simulate a payment intent
            String mockIntentId = "pi_mock_" + System.currentTimeMillis();
            order.setPaymentIntentId(mockIntentId);
            orderRepository.save(order);

            return PaymentResponse.builder()
                    .paymentIntentId(mockIntentId)
                    .clientSecret(mockIntentId + "_secret_mock")
                    .status("requires_payment_method")
                    .message("Mock payment intent created (Stripe not configured). IntentId: " + mockIntentId)
                    .orderId(order.getId())
                    .build();
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String username, String paymentIntentId) {
        User user = getUser(username);
        Order order = orderRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for payment intent: " + paymentIntentId));

        if (!order.getUser().getId().equals(user.getId()) && !user.getRoles().contains("ADMIN")) {
            throw new UnauthorizedException("Access denied to confirm this payment");
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            if (!"succeeded".equals(intent.getStatus())) {
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
                orderRepository.save(order);

                throw new PaymentException("Payment failed with status: " + intent.getStatus());
            }

            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);

            return PaymentResponse.builder()
                    .paymentIntentId(intent.getId())
                    .status(intent.getStatus())
                    .message("Payment confirmed successfully")
                    .orderId(order.getId())
                    .build();

        } catch (StripeException e) {
            // For demo: simulate confirm for mock intent IDs
            if (paymentIntentId.startsWith("pi_mock_")) {
                order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
                order.setStatus(Order.OrderStatus.CONFIRMED);
                orderRepository.save(order);

                return PaymentResponse.builder()
                        .paymentIntentId(paymentIntentId)
                        .status("succeeded")
                        .message("Mock payment confirmed successfully")
                        .orderId(order.getId())
                        .build();
            }
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new PaymentException("Payment confirmation failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String username, Long orderId) {
        User user = getUser(username);
        Order order = getOrderForUser(orderId, user);

        if (order.getPaymentStatus() != Order.PaymentStatus.COMPLETED) {
            throw new PaymentException("Cannot refund: payment not completed for this order");
        }

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(order.getPaymentIntentId())
                    .build();

            Refund refund = Refund.create(params);

            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);

            return PaymentResponse.builder()
                    .paymentIntentId(order.getPaymentIntentId())
                    .status("refunded")
                    .message("Refund processed successfully. Refund ID: " + refund.getId())
                    .orderId(order.getId())
                    .build();

        } catch (StripeException e) {
            // Demo mode: simulate refund for mock intents
            if (order.getPaymentIntentId() != null && order.getPaymentIntentId().startsWith("pi_mock_")) {
                order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);

                return PaymentResponse.builder()
                        .paymentIntentId(order.getPaymentIntentId())
                        .status("refunded")
                        .message("Mock refund processed successfully")
                        .orderId(order.getId())
                        .build();
            }
            throw new PaymentException("Refund failed: " + e.getMessage());
        }
    }

    // ---- Helpers ----

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private Order getOrderForUser(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getUser().getId().equals(user.getId()) && !user.getRoles().contains("ADMIN")) {
            throw new UnauthorizedException("You are not authorized to access this order");
        }
        return order;
    }
}
