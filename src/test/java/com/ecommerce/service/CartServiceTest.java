package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)   // ✅ FIX ADDED
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private CartServiceImpl cartService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItemRequest cartItemRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .password("hashed")
                .roles(Set.of("USER"))
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .category("Electronics")
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .cartItems(new ArrayList<>())
                .build();

        cartItemRequest = new CartItemRequest();
        cartItemRequest.setProductId(1L);
        cartItemRequest.setQuantity(2);
    }

    @Test
    @DisplayName("Get Cart - Creates New Cart If Not Exists")
    void getCart_CreatesNewCartIfNotExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.getCartByUser("testuser");

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Get Cart - Returns Existing Cart")
    void getCart_ReturnsExistingCart() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCartByUser("testuser");

        assertThat(response.getId()).isEqualTo(1L);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Add Item To Cart - Success (new item)")
    void addItem_Success_NewItem() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addItemToCart("testuser", cartItemRequest);

        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Add Item To Cart - Insufficient Stock")
    void addItem_InsufficientStock() {

        product.setStockQuantity(1);
        cartItemRequest.setQuantity(5);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // ❌ REMOVED unnecessary stubbing
        // NO cartRepository / cartItemRepository mocking needed

        assertThatThrownBy(() ->
                cartService.addItemToCart("testuser", cartItemRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Add Item To Cart - Product Not Found")
    void addItem_ProductNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        cartItemRequest.setProductId(99L);

        assertThatThrownBy(() ->
                cartService.addItemToCart("testuser", cartItemRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Add Item - Increments Quantity for Existing Item")
    void addItem_IncrementsExistingItem() {
        CartItem existing = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(existing);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));

        cartService.addItemToCart("testuser", cartItemRequest);

        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(existing);
    }

    @Test
    @DisplayName("Remove Item - Unauthorized User")
    void removeItem_UnauthorizedUser() {
        User otherUser = User.builder().id(2L).username("other").roles(Set.of("USER")).build();
        Cart otherCart = Cart.builder().id(2L).user(otherUser).cartItems(new ArrayList<>()).build();
        CartItem item = CartItem.builder().id(5L).cart(otherCart).product(product).quantity(1).build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() ->
                cartService.removeItemFromCart("testuser", 5L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Clear Cart - Success")
    void clearCart_Success() {
        CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();
        cart.getCartItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart("testuser");

        assertThat(cart.getCartItems()).isEmpty();
        verify(cartRepository).save(cart);
    }
}