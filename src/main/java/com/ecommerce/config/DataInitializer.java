package com.ecommerce.config;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@ecommerce.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .firstName("Admin")
                    .lastName("User")
                    .roles(Set.of("ADMIN", "USER"))
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("testuser")) {
            User user = User.builder()
                    .username("testuser")
                    .email("testuser@ecommerce.com")
                    .password(passwordEncoder.encode("Test@123"))
                    .firstName("Test")
                    .lastName("User")
                    .roles(Set.of("USER"))
                    .build();
            userRepository.save(user);
        }

        if (productRepository.count() == 0) {
            productRepository.save(Product.builder()
                    .name("Laptop Pro 15")
                    .description("High-performance laptop with 16GB RAM and 512GB SSD")
                    .price(new BigDecimal("1299.99"))
                    .stockQuantity(50)
                    .category("Electronics")
                    .imageUrl("https://via.placeholder.com/400x300?text=Laptop")
                    .build());

            productRepository.save(Product.builder()
                    .name("Wireless Headphones")
                    .description("Noise-cancelling Bluetooth headphones")
                    .price(new BigDecimal("199.99"))
                    .stockQuantity(100)
                    .category("Electronics")
                    .imageUrl("https://via.placeholder.com/400x300?text=Headphones")
                    .build());

            productRepository.save(Product.builder()
                    .name("Running Shoes")
                    .description("Lightweight and durable running shoes")
                    .price(new BigDecimal("89.99"))
                    .stockQuantity(200)
                    .category("Footwear")
                    .imageUrl("https://via.placeholder.com/400x300?text=Shoes")
                    .build());

            productRepository.save(Product.builder()
                    .name("Smart Watch")
                    .description("Fitness tracking smartwatch with heart rate monitor")
                    .price(new BigDecimal("249.99"))
                    .stockQuantity(75)
                    .category("Electronics")
                    .imageUrl("https://via.placeholder.com/400x300?text=SmartWatch")
                    .build());

            productRepository.save(Product.builder()
                    .name("Backpack 30L")
                    .description("Water-resistant hiking backpack")
                    .price(new BigDecimal("59.99"))
                    .stockQuantity(150)
                    .category("Accessories")
                    .imageUrl("https://via.placeholder.com/400x300?text=Backpack")
                    .build());
        }
    }
}
