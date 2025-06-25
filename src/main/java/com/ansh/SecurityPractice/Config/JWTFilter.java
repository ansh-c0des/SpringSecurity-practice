package com.ansh.SecurityPractice.Config;

import com.ansh.SecurityPractice.Service.JWTService;
import com.ansh.SecurityPractice.Service.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Marks this class as a Spring component so it gets detected and managed by the Spring container
@Component
public class JWTFilter extends OncePerRequestFilter {

    // Injects the JWTService, responsible for token generation, extraction, and validation
    @Autowired
    private JWTService jwtService;

    // Injects the Spring application context to get beans dynamically, e.g., UserDetailsService
    @Autowired
    ApplicationContext context;

    // This method runs once per request to apply custom filtering logic
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the 'Authorization' header from the incoming request
        String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        // Check if the header exists and starts with 'Bearer '
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Remove 'Bearer ' prefix to extract the actual token
            token = authHeader.substring(7);
            // Extract the username (subject) from the token using JWTService
            username = jwtService.extractUserName(token);
        }
        /**
         * If username is successfully extracted and no authentication is present in the security context,
         * this means the user is not yet authenticated for this request
         */
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Dynamically fetch the UserDetailsService bean to load user details by username
            UserDetails userDetails = context.getBean(MyUserDetailsService.class).loadUserByUsername(username);

            // Validate the token using JWTService (checks signature, expiration, etc.)
            if (jwtService.validateToken(token, userDetails)) {

                // Create an authentication token with the user details and authorities (roles/permissions)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Attach additional request-specific details to the authentication token
                authToken.setDetails(new WebAuthenticationDetailsSource()
                        .buildDetails(request));

                // Set the authenticated user in the security context for the current thread
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // Continue with the filter chain, passing the request and response
        filterChain.doFilter(request, response);
    }
}
