package com.nhom13.ecommerce.config;

import com.nhom13.ecommerce.security.JwtAuthenticationEntryPoint;
import com.nhom13.ecommerce.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                            "/api/auth/**",
                            "/api/products/**",
                            "/api/categories/**",
                            "/api/promotions/active", // Đảm bảo public
                            "/api/reviews/product/**", // Đảm bảo public
                            "/api/search/**", // Đảm bảo public
                            "/api/payments/vnpay_return", // GET, Public (cho trình duyệt)
                            "/api/payments/vnpay_ipn",    // GET, Public (cho VNPAY server)


                            "/swagger-ui.html", // Chỉ định file html
                            "/swagger-ui/**",   // Các tài nguyên tĩnh của swagger
                            "/v3/api-docs/**"   // File JSON định nghĩa API
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/refunds").hasRole("CUSTOMER")
                   .requestMatchers(HttpMethod.GET, "/api/refunds/me").hasRole("CUSTOMER")

                    // ADMIN: Có thể cập nhật status, xem danh sách
                   .requestMatchers(HttpMethod.PUT, "/api/refunds/{id}/status").hasRole("ADMIN")
                   .requestMatchers(HttpMethod.GET, "/api/refunds").hasRole("ADMIN")

                    // CHUNG: Cả hai đều có thể xem chi tiết (logic sở hữu được xử lý ở Controller/Service)
                   .requestMatchers(HttpMethod.GET, "/api/refunds/{id}").hasAnyRole("CUSTOMER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // SỬ DỤNG PATTERNS
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}