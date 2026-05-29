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

                                                // PLATFORM ADMIN
                                                .requestMatchers(antMatcher("/admin/**"))
                                                .hasRole("PLATFORM_ADMIN")

                                                .requestMatchers(antMatcher("/dashboard/platform-admin"))
                                                .hasRole("PLATFORM_ADMIN")

                                                // COMPANY OWNER
                                                .requestMatchers(antMatcher("/company/**"))
                                                .hasRole("COMPANY_OWNER")

                                                .requestMatchers(antMatcher("/dashboard/company-owner"))
                                                .hasRole("COMPANY_OWNER")

                                                // JOBS
                                                .requestMatchers(antMatcher("/jobs/create"),
                                                                antMatcher("/jobs/edit/**"),
                                                                antMatcher("/jobs/delete/**"),
                                                                antMatcher("/jobs/change-status/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR")

                                                .requestMatchers(antMatcher("/jobs/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR", "VIEWER")

                                                // CANDIDATES
                                                .requestMatchers(antMatcher("/candidates/create"),
                                                                antMatcher("/candidates/edit/**"),
                                                                antMatcher("/candidates/delete/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR")

                                                .requestMatchers(antMatcher("/candidates/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR", "VIEWER")

                                                // APPLICATIONS
                                                .requestMatchers(antMatcher("/applications/create"),
                                                                antMatcher("/applications/edit/**"),
                                                                antMatcher("/applications/delete/**"),
                                                                antMatcher("/applications/change-status/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR")

                                                .requestMatchers(antMatcher("/applications/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR", "VIEWER")

                                                // RESUMES + AI
                                                .requestMatchers(antMatcher("/resumes/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR")

                                                .requestMatchers(antMatcher("/ai/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR")

                                                // VERIFICATIONS
                                                .requestMatchers(antMatcher("/verifications/**"),
                                                                antMatcher("/api/verifications/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR", "VIEWER")

                                                // INTERVIEWS
                                                .requestMatchers(antMatcher("/interviews/my/**"))
                                                .hasRole("INTERVIEWER")

                                                .requestMatchers(antMatcher("/interviews/**"))
                                                .hasAnyRole("COMPANY_OWNER", "HR", "INTERVIEWER")

                                                // DASHBOARDS
                                                .requestMatchers(antMatcher("/dashboard/hr"))
                                                .hasRole("HR")

                                                .requestMatchers(antMatcher("/dashboard/interviewer"))
                                                .hasRole("INTERVIEWER")

                                                .requestMatchers(antMatcher("/dashboard/viewer"))
                                                .hasRole("VIEWER")

                                                .requestMatchers(antMatcher("/dashboard/**"))
                                                .hasAnyRole("PLATFORM_ADMIN", "COMPANY_OWNER", "HR", "INTERVIEWER",
                                                                "VIEWER")

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