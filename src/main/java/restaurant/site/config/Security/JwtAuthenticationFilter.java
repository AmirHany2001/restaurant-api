package restaurant.site.config.Security;


import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.jsonwebtoken.JwtException;
import restaurant.site.services.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    @NullMarked
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isTokenValid(token, email)) {
                    String role = jwtService.extractRole(token);

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Bad, expired or tampered token: do NOT throw (an exception thrown in a filter
            // never reaches @RestControllerAdvice). Leave the request unauthenticated; a
            // protected URL is then answered with 401 by SecurityErrorHandler.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }



    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)  {

        // "/auth/**" is a pattern, not a path, so equals() never matched
        return request.getRequestURI().startsWith("/auth/");

    }
}
