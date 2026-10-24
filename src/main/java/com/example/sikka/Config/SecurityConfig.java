package com.example.sikka.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/account/register").permitAll()
                        .requestMatchers("/api/v1/account/get", "/api/v1/account/add",
                                "/api/v1/account/update/**", "/api/v1/account/delete/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/visitor-profile/get", "/api/v1/visitor-profile/add",
                                "/api/v1/visitor-profile/update/**", "/api/v1/visitor-profile/delete/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/visit/get", "/api/v1/visit/add",
                                "/api/v1/visit/update/**", "/api/v1/visit/delete/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/visit-group/get", "/api/v1/visit-group/add",
                                "/api/v1/visit-group/update/**", "/api/v1/visit-group/delete/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/visit-member/get", "/api/v1/visit-member/add",
                                "/api/v1/visit-member/delete/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .httpBasic(basic -> {})
                .build();
    }
}