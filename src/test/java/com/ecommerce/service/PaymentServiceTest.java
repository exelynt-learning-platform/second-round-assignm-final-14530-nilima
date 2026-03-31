package com.ecommerce.service;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private PaymentServiceImpl paymentService;

    private User user;
    private Order order;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "sk_test_dummy");

        user = User.builder()
                .id(1L).username("testuser").email("test@test.com")
                .password("hashed").roles(Set.of("USER")).build();

        order = Order.builder()
                .id(1L).user(user)
                .totalPrice(new BigDecimal("999.99"))
                .orderItems(new ArrayList<>())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setCurrency("usd");
    }

    @Test
    @DisplayName("Create Payment Intent - Success (mock Stripe fallback)")
    void createPaymentIntent_Success_MockFallback() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse response = paymentService.createPaymentIntent("testuser", paymentRequest);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getPaymentIntentId()).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Create Payment Intent - Order Already Paid")
    void createPaymentIntent_AlreadyPaid() {
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPaymentIntent("testuser", paymentRequest))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("already been paid");
    }

    @Test
    @DisplayName("Create Payment Intent - Order Not Found")
    void createPaymentIntent_OrderNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        paymentRequest.setOrderId(99L);

        assertThatThrownBy(() -> paymentService.createPaymentIntent("testuser", paymentRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Confirm Payment - Success (mock intent)")
    void confirmPayment_Success_MockIntent() {
        String mockIntentId = "pi_mock_123456";
        order.setPaymentIntentId(mockIntentId);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByPaymentIntentId(mockIntentId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse response = paymentService.confirmPayment("testuser", mockIntentId);

        assertThat(response.getStatus()).isEqualTo("succeeded");
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Confirm Payment - Intent Not Found")
    void confirmPayment_IntentNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByPaymentIntentId("nonexistent_id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment("testuser", "nonexistent_id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Refund Payment - Success (mock intent)")
    void refundPayment_Success_MockIntent() {
        String mockIntentId = "pi_mock_99999";
        order.setPaymentIntentId(mockIntentId);
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse response = paymentService.refundPayment("testuser", 1L);

        assertThat(response.getStatus()).isEqualTo("refunded");
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.REFUNDED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Refund Payment - Payment Not Completed")
    void refundPayment_NotCompleted() {
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.refundPayment("testuser", 1L))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("payment not completed");
    }
    
    @Test
    @DisplayName("Confirm Payment - Unauthorized User")
    void confirmPayment_UnauthorizedUser() {
        String mockIntentId = "pi_mock_123456";

        User anotherUser = User.builder()
                .id(2L).username("otheruser").build();

        order.setPaymentIntentId(mockIntentId);
        order.setUser(anotherUser); 
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByPaymentIntentId(mockIntentId)).thenReturn(Optional.of(order));

        assertThrows(Exception.class, () -> {
            paymentService.confirmPayment("testuser", mockIntentId);
        });
    }
    
    

    @Test
    @DisplayName("Refund Payment - Unauthorized User")
    void refundPayment_UnauthorizedUser() {

        User anotherUser = User.builder()
                .id(2L).username("otheruser").build();

        order.setUser(anotherUser);
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(Exception.class, () -> {
            paymentService.refundPayment("testuser", 1L);
        });
    }
}
