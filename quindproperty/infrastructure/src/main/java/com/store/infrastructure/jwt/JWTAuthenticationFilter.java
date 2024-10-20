package com.store.infrastructure.jwt;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import com.store.application.port.in.JWTUseCase;
import com.store.domain.dto.UserClaims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {
  private final JWTUseCase jwtService;
  private final UserDetailsService userDetailsService;
  
  @Override
  @SneakyThrows
  protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
      @NonNull FilterChain filterChain) {

    String bearerTk = req.getHeader("Authorization");

    if (bearerTk == null || !bearerTk.startsWith("Bearer ")) {
      filterChain.doFilter(req, res);
      return;
    }

    bearerTk = bearerTk.replace("Bearer ", "");

    UserClaims data = jwtService.verifyToken(bearerTk);

    UserDetails userDetails = userDetailsService.loadUserByUsername(data.getEmail());
    
    if (data.getEmail().equals(userDetails.getUsername())) {
      UsernamePasswordAuthenticationToken userPassAuthToken = new UsernamePasswordAuthenticationToken(data, null,
          userDetails.getAuthorities());
  
      userPassAuthToken.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(req));
  
      SecurityContextHolder.getContext().setAuthentication(userPassAuthToken);
    }

    filterChain.doFilter(req, res);
  }

}
