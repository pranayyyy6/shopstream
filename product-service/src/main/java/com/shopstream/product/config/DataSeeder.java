package com.shopstream.product.config;

import com.shopstream.product.model.Product;
import com.shopstream.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    // CommandLineRunner runs once after Spring Boot starts
    // Perfect for seeding test data
    @Bean
    public CommandLineRunner seedData(ProductRepository productRepository) {
        return args -> {
            // Only seed if DB is empty — don't duplicate on restart
            if (productRepository.count() > 0) {
                log.info("Data already seeded — skipping");
                return;
            }

            log.info("Seeding 1000 products...");
            Random random = new Random();

            String[][] products = {
                    // {name, category, basePrice}
                    {"iPhone 15 Pro", "Electronics", "134900"},
                    {"Samsung Galaxy S24", "Electronics", "79999"},
                    {"MacBook Pro M3", "Electronics", "199900"},
                    {"Dell XPS 15", "Electronics", "159900"},
                    {"Sony WH-1000XM5", "Electronics", "29990"},
                    {"iPad Pro 12.9", "Electronics", "109900"},
                    {"Apple Watch Ultra", "Electronics", "89900"},
                    {"OnePlus 12", "Electronics", "64999"},
                    {"Bose QuietComfort 45", "Electronics", "24990"},
                    {"LG OLED TV 55", "Electronics", "139990"},
                    {"Nike Air Max 270", "Footwear", "12995"},
                    {"Adidas Ultraboost 22", "Footwear", "14999"},
                    {"Puma RS-X", "Footwear", "8999"},
                    {"Reebok Classic", "Footwear", "6999"},
                    {"New Balance 574", "Footwear", "9999"},
                    {"Levi's 511 Slim Fit", "Clothing", "3999"},
                    {"Allen Solly Formal Shirt", "Clothing", "1999"},
                    {"H&M Cotton T-Shirt", "Clothing", "799"},
                    {"Zara Blazer", "Clothing", "5999"},
                    {"Van Heusen Trousers", "Clothing", "2999"},
                    {"Instant Pot Duo", "Kitchen", "8999"},
                    {"Philips Air Fryer", "Kitchen", "6999"},
                    {"Prestige Pressure Cooker", "Kitchen", "2499"},
                    {"Bosch Mixer Grinder", "Kitchen", "4999"},
                    {"Morphy Richards Toaster", "Kitchen", "3499"},
                    {"Atomic Habits", "Books", "399"},
                    {"The Lean Startup", "Books", "499"},
                    {"Clean Code", "Books", "2999"},
                    {"System Design Interview", "Books", "3499"},
                    {"Deep Work", "Books", "449"},
                    {"Yoga Mat Premium", "Sports", "1999"},
                    {"Protein Whey Isolate 2kg", "Sports", "3999"},
                    {"Resistance Bands Set", "Sports", "999"},
                    {"Adjustable Dumbbell Set", "Sports", "8999"},
                    {"Running Shoes Pro", "Sports", "7999"},
                    {"L'Oreal Serum", "Beauty", "899"},
                    {"Nivea Body Lotion", "Beauty", "349"},
                    {"Mamaearth Face Wash", "Beauty", "299"},
                    {"The Ordinary Niacinamide", "Beauty", "599"},
                    {"Biotique Sunscreen", "Beauty", "249"},
            };

            List<Product> toSave = new ArrayList<>();

            // Generate 1000 products from the base list with variations
            for (int i = 0; i < 1000; i++) {
                String[] template = products[i % products.length];
                String name = template[0];
                String category = template[1];
                double basePrice = Double.parseDouble(template[2]);

                // Add variation so products aren't identical
                if (i >= products.length) {
                    name = name + " - Variant " + (i / products.length);
                    basePrice = basePrice * (0.8 + random.nextDouble() * 0.4);
                }

                Product product = Product.builder()
                        .name(name)
                        .description("Premium quality " + name +
                                " — best in class performance and value.")
                        .price(BigDecimal.valueOf(Math.round(basePrice)))
                        .stockQuantity(random.nextInt(200) + 10)
                        .category(category)
                        .deleted(false)
                        .build();

                toSave.add(product);
            }

            // saveAll in one batch — much faster than 1000 individual saves
            // One DB round trip vs 1000
            productRepository.saveAll(toSave);
            log.info("Successfully seeded 1000 products into PostgreSQL");
        };
    }
}