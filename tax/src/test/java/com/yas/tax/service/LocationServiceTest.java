package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.tax.config.ServiceUrlConfig;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class LocationServiceTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        locationService = new LocationService(restClient, serviceUrlConfig);
        when(serviceUrlConfig.location()).thenReturn("http://api.yas.local/location");

        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("token-value");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getStateOrProvinceAndCountryNames_shouldCallLocationApiAndReturnBody() {
        List<Long> ids = List.of(1L, 2L);
        URI uri = UriComponentsBuilder.fromUriString("http://api.yas.local/location")
            .path("/backoffice/state-or-provinces/state-country-names")
            .queryParam("stateOrProvinceIds", ids)
            .build()
            .toUri();

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        List<StateOrProvinceAndCountryGetNameVm> expected = List.of(
            new StateOrProvinceAndCountryGetNameVm(1L, "HCM", "VN")
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(uri)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(expected);

        List<StateOrProvinceAndCountryGetNameVm> result = locationService.getStateOrProvinceAndCountryNames(ids);

        assertEquals(1, result.size());
        assertEquals("HCM", result.getFirst().stateOrProvinceName());
    }

    @Test
    void handleLocationNameListFallback_shouldRethrow() {
        RuntimeException ex = new RuntimeException("location-fallback");

        assertThrows(RuntimeException.class, () -> locationService.handleLocationNameListFallback(ex));
    }
}
