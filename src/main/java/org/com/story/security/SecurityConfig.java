package org.com.story.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.com.story.security.oauth2.CustomOAuth2UserService;
import org.com.story.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ✅ CORS - ENABLE for ngrok
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ❌ CSRF
                .csrf(csrf -> csrf.disable())

                // Session: STATELESS cho API, nhưng OAuth2 cần session tạm để xử lý authorization code flow
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // ❌ Unauthorized handler (KHÔNG redirect)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized"
                                )
                        )
                )

                // ✅ AUTH RULES
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow root path and error page
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()

                        // Allow public access to uploaded images (avatars, etc.)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        .requestMatchers(
                                "/api/auth/**",
                                "/api/health",
                                "/oauth2/**",
                                "/test/**"
                        ).permitAll()

                        // Payment: danh sách gói (public) + IPN webhook (PayOS server gọi)
                        .requestMatchers(HttpMethod.GET,  "/api/payment/packages").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payment/ipn").permitAll()

                        // Public read access to stories, chapters, categories, comments
                        .requestMatchers(HttpMethod.GET, "/api/stories", "/api/stories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chapters/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/chapter/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/missions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ratings/story/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/gifts/story/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/follows/story/*/count").permitAll()

                        // Admin endpoints - ADMIN only
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Reviewer endpoints - REVIEWER and ADMIN
                        .requestMatchers("/api/reviewer/**")
                        .hasAnyRole("REVIEWER", "ADMIN")

                        // Editor endpoints - EDITOR and ADMIN
                        .requestMatchers("/api/editor/**")
                        .hasAnyRole("EDITOR", "ADMIN")

                        // Edit Request - Editor side (nhận việc, nộp bản, xem open)
                        .requestMatchers(HttpMethod.GET, "/api/edit-requests/open").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/edit-requests/assigned").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/edit-requests/*/assign").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/edit-requests/*/submit").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/edit-requests/*/withdraw").hasAnyRole("EDITOR", "ADMIN")
                        // Edit Request - Author side & general (authenticated)
                        .requestMatchers("/api/edit-requests/**").authenticated()

                        // Category management (POST/PUT/DELETE) - ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        // Role change requests - user tự gửi & xem của mình
                        .requestMatchers(HttpMethod.POST, "/api/role-change-requests").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/role-change-requests/my").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // ✅ OAuth2 (KHÔNG DÙNG /login)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                // ✅ JWT FILTER
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // ===============================
    // CORS CONFIG – FIX PREFLIGHT & NGROK
    // ===============================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow all origins including ngrok
        config.setAllowedOriginPatterns(List.of("*"));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        config.setAllowCredentials(false);

        config.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
