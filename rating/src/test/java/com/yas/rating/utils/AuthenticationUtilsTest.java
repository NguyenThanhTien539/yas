package com.yas.rating.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_whenAnonymous_thenThrowAccessDeniedException() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(anonymous);
        SecurityContextHolder.setContext(context);

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            AuthenticationUtils::extractUserId
        );

        assertEquals("ACCESS_DENIED", exception.getMessage());
    }

    @Test
    void extractUserId_whenJwtToken_thenReturnSubject() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-123");

        JwtAuthenticationToken jwtAuth = mock(JwtAuthenticationToken.class);
        when(jwtAuth.getToken()).thenReturn(jwt);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(jwtAuth);
        SecurityContextHolder.setContext(context);

        String userId = AuthenticationUtils.extractUserId();

        assertEquals("user-123", userId);
    }
}
