package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ServiceUrlConfig serviceUrlConfig;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @InjectMocks
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token-123").header("alg", "none").claim("sub", "user-1").build();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        lenient().when(authentication.getPrincipal()).thenReturn(jwt);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMedia_whenIdIsNull_shouldReturnDefaultNoFileMediaVm() {
        NoFileMediaVm result = mediaService.getMedia(null);

        assertThat(result.id()).isNull();
        assertThat(result.caption()).isEqualTo("");
        assertThat(result.fileName()).isEqualTo("");
        assertThat(result.mediaType()).isEqualTo("");
        assertThat(result.url()).isEqualTo("");
    }

    @Test
    void getMedia_whenIdExists_shouldCallMediaApiAndReturnResponse() {
        NoFileMediaVm expected = new NoFileMediaVm(11L, "caption", "name", "type", "https://img");

        when(serviceUrlConfig.media()).thenReturn("http://media-service");
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(NoFileMediaVm.class)).thenReturn(expected);

        NoFileMediaVm result = mediaService.getMedia(11L);

        verify(restClient).get();
        verify(requestHeadersUriSpec).retrieve();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void saveFile_shouldSendMultipartRequestWithBearerToken() {
        MultipartFile multipartFile = org.mockito.Mockito.mock(MultipartFile.class);
        NoFileMediaVm expected = new NoFileMediaVm(1L, "caption", "name", "type", "https://img");

        when(serviceUrlConfig.media()).thenReturn("http://media-service");
        when(multipartFile.getResource()).thenReturn(new InputStreamResource(new ByteArrayInputStream("a".getBytes())));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(java.net.URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        org.mockito.Mockito.doReturn(requestBodySpec).when(requestBodySpec)
            .body(any(org.springframework.util.MultiValueMap.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(NoFileMediaVm.class)).thenReturn(expected);

        NoFileMediaVm result = mediaService.saveFile(multipartFile, "caption", "override-name");

        assertThat(result).isEqualTo(expected);
        verify(restClient).post();
        verify(requestBodySpec).headers(any());
    }

    @Test
    void removeMedia_shouldSendDeleteRequestWithBearerToken() {
        when(serviceUrlConfig.media()).thenReturn("http://media-service");
        when(restClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Void.class)).thenReturn(null);

        mediaService.removeMedia(99L);

        verify(restClient).delete();
        verify(requestHeadersUriSpec).headers(any());
        verify(responseSpec).body(Void.class);
    }

    @Test
    void getMedia_whenFallbackHandlerInvokedThroughReflection_shouldRethrow() throws Exception {
        var method = MediaService.class.getDeclaredMethod("handleMediaFallback", Throwable.class);
        method.setAccessible(true);
        RuntimeException error = new RuntimeException("fallback");

        assertThatThrownBy(() -> method.invoke(mediaService, error))
            .hasCause(error);
    }
}
