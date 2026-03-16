package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

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

    @InjectMocks
    private MediaService mediaService;

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
}
