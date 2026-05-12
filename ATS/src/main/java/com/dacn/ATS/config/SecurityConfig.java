package com.dacn.ATS.config;

import com.dacn.ATS.module.auth.service.impl.CustomUserDetailsService;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
        @Autowired
        private CustomLoginSuccessHandler customLoginSuccessHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService) {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authProvider)
                        throws Exception {
                http
                                .authenticationProvider(authProvider)
                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers(
                                                                antMatcher("/"),
                                                                antMatcher("/error"),
                                                                antMatcher("/public/**"),
                                                                antMatcher("/auth/login"),
                                                                antMatcher("/auth/register"),
                                                                antMatcher("/auth/doRegister"),
                                                                antMatcher("/auth/doLogin"),
                                                                antMatcher("/css/**"),
                                                                antMatcher("/js/**"),
                                                                antMatcher("/images/**"),
                                                                antMatcher("/webjars/**"))
                                                .permitAll()

                                                .requestMatchers(antMatcher("/admin/**"))
                                                .hasRole("ADMIN")

                                                .requestMatchers(antMatcher("/jobs/**"))
                                                .hasAnyRole("ADMIN", "HR")

                                                .requestMatchers(antMatcher("/applications/**"))
                                                .hasAnyRole("ADMIN", "HR")

                                                .requestMatchers(antMatcher("/ai/**"))
                                                .hasAnyRole("ADMIN", "HR")

                                                .requestMatchers(antMatcher("/candidates/**"))
                                                .hasAnyRole("ADMIN", "HR")

                                                .requestMatchers(antMatcher("/interviews/my/**"))
                                                .hasRole("INTERVIEWER")

                                                .requestMatchers(antMatcher("/interviews/**"))
                                                .hasAnyRole("ADMIN", "HR", "INTERVIEWER")

                                                .requestMatchers(antMatcher("/dashboard/**"))
                                                .hasAnyRole("ADMIN", "HR", "INTERVIEWER")

                                                .requestMatchers(antMatcher("/resumes/**"))
                                                .hasAnyRole("ADMIN", "HR")

                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/auth/login")
                                                .loginProcessingUrl("/auth/doLogin")
                                                .successHandler(customLoginSuccessHandler)
                                                .failureUrl("/auth/login?error=true")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/auth/logout")
                                                .logoutSuccessUrl("/auth/login?logout=true")
                                                .permitAll())
                                .csrf(csrf -> csrf.disable());

                return http.build();
        }
}