package restaurant.site.config.Security;


import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import restaurant.site.services.security.UserDetailsServiceImpl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity // --> for using @PreAuthorize("hasRole('ADMIN')")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final SecurityErrorHandler securityErrorHandler;

    // Browser origins (frontends) allowed to call this API, comma separated.
    // Empty = no browser origin is allowed. Set per profile / with CORS-ALLOWED-ORIGINS.
    @Value("${application.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
                 {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // uses the corsConfigurationSource bean below
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)  // 401 as JSON
                        .accessDeniedHandler(securityErrorHandler))      // 403 as JSON
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**"
                        ).permitAll()
                        // the menu is public: anyone can read items and categories (GET only);
                        // creating / changing / deleting still needs ADMIN via @PreAuthorize
                        .requestMatchers(HttpMethod.GET, "/items/**", "/categories/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    // Browsers block a frontend on another origin (e.g. localhost:3000) from calling this API
    // unless the API says that origin is allowed. Mobile apps and Postman do not need this.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
        config.setMaxAge(3600L); // the browser may cache the preflight answer for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new DefaultMethodSecurityExpressionHandler();
    }



}