package com.ecommerce.service;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks private ProductServiceImpl productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L).name("Laptop").description("A great laptop")
                .price(new BigDecimal("999.99")).stockQuantity(10)
                .category("Electronics").imageUrl("http://img.test/laptop.jpg")
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("Laptop");
        productRequest.setDescription("A great laptop");
        productRequest.setPrice(new BigDecimal("999.99"));
        productRequest.setStockQuantity(10);
        productRequest.setCategory("Electronics");
        productRequest.setImageUrl("http://img.test/laptop.jpg");
    }

    @Test
    @DisplayName("Create Product - Success")
    void createProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getPrice()).isEqualByComparingTo("999.99");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Get Product By Id - Found")
    void getProductById_Found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Get Product By Id - Not Found")
    void getProductById_NotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get All Products")
    void getAllProducts_ReturnsList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Update Product - Success")
    void updateProduct_Success() {
        productRequest.setName("Updated Laptop");
        productRequest.setPrice(new BigDecimal("899.99"));

        Product updated = Product.builder()
                .id(1L).name("Updated Laptop").price(new BigDecimal("899.99"))
                .stockQuantity(10).category("Electronics").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        ProductResponse response = productService.updateProduct(1L, productRequest);

        assertThat(response.getName()).isEqualTo("Updated Laptop");
        assertThat(response.getPrice()).isEqualByComparingTo("899.99");
    }

    @Test
    @DisplayName("Delete Product - Success")
    void deleteProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        assertThatCode(() -> productService.deleteProduct(1L)).doesNotThrowAnyException();
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete Product - Not Found")
    void deleteProduct_NotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Search Products By Name")
    void searchProducts_ReturnsResults() {
        when(productRepository.findByNameContainingIgnoreCase("laptop"))
                .thenReturn(List.of(product));

        List<ProductResponse> results = productService.searchProducts("laptop");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).containsIgnoringCase("Laptop");
    }
}
