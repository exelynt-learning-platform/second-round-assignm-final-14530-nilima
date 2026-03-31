package com.ecommerce.controller;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(com.ecommerce.config.SecurityConfig.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProductService productService;
    @MockBean private com.ecommerce.security.UserDetailsServiceImpl userDetailsService;
    @MockBean private com.ecommerce.security.JwtUtils jwtUtils;

    private ProductResponse productResponse;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id(1L).name("Laptop").description("Great laptop")
                .price(new BigDecimal("999.99")).stockQuantity(10)
                .category("Electronics").build();

        productRequest = new ProductRequest();
        productRequest.setName("Laptop");
        productRequest.setDescription("Great laptop");
        productRequest.setPrice(new BigDecimal("999.99"));
        productRequest.setStockQuantity(10);
        productRequest.setCategory("Electronics");
    }

    @Test
    @DisplayName("GET /api/products - Returns product list (public)")
    void getAllProducts_Public() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Laptop"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Returns product (public)")
    void getProductById_Public() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productResponse);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.price").value(999.99));
    }

    @Test
    @DisplayName("POST /api/products - Admin can create product")
    @WithMockUser(roles = "ADMIN")
    void createProduct_AdminSuccess() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Laptop"));
    }

    @Test
    @DisplayName("POST /api/products - User (non-admin) gets 403")
    @WithMockUser(roles = "USER")
    void createProduct_UserForbidden() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - Admin can update")
    @WithMockUser(roles = "ADMIN")
    void updateProduct_AdminSuccess() throws Exception {
        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(productResponse);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop"));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - Admin can delete")
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_AdminSuccess() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/products/search?name=laptop")
    void searchProducts() throws Exception {
        when(productService.searchProducts("laptop")).thenReturn(List.of(productResponse));

        mockMvc.perform(get("/api/products/search").param("name", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Laptop"));
    }
}
