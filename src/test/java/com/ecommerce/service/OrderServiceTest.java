package com.ecommerce.service;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartService cartService;

    @InjectMocks private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).username("testuser").email("test@test.com")
                .password("hashed").roles(Set.of("USER")).build();

        product = Product.builder()
                .id(1L).name("Laptop").price(new BigDecimal("999.99"))
                .stockQuantity(10).category("Electronics").build();

        cartItem = CartItem.builder()
                .id(1L).product(product).quantity(2).build();

        cart = Cart.builder()
                .id(1L).user(user).cartItems(new ArrayList<>(List.of(cartItem))).build();
        cartItem.setCart(cart);

        orderRequest = new OrderRequest();
        orderRequest.setShippingAddress("123 Main St");
        orderRequest.setShippingCity("Mumbai");
        orderRequest.setShippingState("MH");
        orderRequest.setShippingZipCode("400001");
        orderRequest.setShippingCountry("India");
        orderRequest.setPaymentMethod("STRIPE");
    }

    @Test
    @DisplayName("Create Order - Success")
    void createOrder_Success() {
        Order savedOrder = Order.builder()
                .id(1L).user(user).totalPrice(new BigDecimal("1999.98"))
                .orderItems(new ArrayList<>())
                .shippingAddress("123 Main St").shippingCity("Mumbai")
                .shippingState("MH").shippingZipCode("400001").shippingCountry("India")
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartService).clearCart("testuser");

        OrderResponse response = orderService.createOrder("testuser", orderRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        verify(cartService).clearCart("testuser");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Create Order - Empty Cart")
    void createOrder_EmptyCart() {
        cart.getCartItems().clear();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder("testuser", orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("Create Order - No Cart Found")
    void createOrder_NoCart() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder("testuser", orderRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Create Order - Insufficient Stock")
    void createOrder_InsufficientStock() {
        product.setStockQuantity(1);
        cartItem.setQuantity(5);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrder("testuser", orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Get Order By Id - Success")
    void getOrderById_Success() {
        Order order = Order.builder()
                .id(1L).user(user).totalPrice(new BigDecimal("999.99"))
                .orderItems(new ArrayList<>())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("testuser", 1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get Order By Id - Not Found")
    void getOrderById_NotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("testuser", 99L))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Get User Orders - Returns List")
    void getUserOrders_ReturnsList() {
        Order order = Order.builder()
                .id(1L).user(user).totalPrice(new BigDecimal("999.99"))
                .orderItems(new ArrayList<>())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getUserOrders("testuser");

        assertThat(orders).hasSize(1);
    }

    @Test
    @DisplayName("Update Order Status - Success")
    void updateOrderStatus_Success() {
        Order order = Order.builder()
                .id(1L).user(user).totalPrice(new BigDecimal("999.99"))
                .orderItems(new ArrayList<>())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(1L, "CONFIRMED");

        assertThat(response.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Update Order Status - Invalid Status")
    void updateOrderStatus_InvalidStatus() {
        Order order = Order.builder()
                .id(1L).user(user).totalPrice(new BigDecimal("999.99"))
                .orderItems(new ArrayList<>())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "INVALID_STATUS"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid order status");
    }

    @Test
    @DisplayName("Get All Orders - Admin")
    void getAllOrders_ReturnsAll() {
        Order order = Order.builder()
                .id(1L).user(user).totalPrice(new BigDecimal("999.99"))
                .orderItems(new ArrayList<>())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getAllOrders();

        assertThat(orders).hasSize(1);
        verify(orderRepository).findAll();
    }
}
