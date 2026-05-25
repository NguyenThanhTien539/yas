package com.yas.media.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void create_whenValidInput_thenReturn200WithNoFileMediaVm() {
        Media media = new Media();
        media.setId(1L);
        media.setCaption("caption");
        media.setFileName("test.png");
        media.setMediaType("image/png");

        MediaPostVm postVm = new MediaPostVm(
            "caption",
            new MockMultipartFile("file", "test.png", "image/png", new byte[0]),
            null
        );
        when(mediaService.saveMedia(any())).thenReturn(media);

        ResponseEntity<Object> response = controller.create(postVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void delete_whenValidId_thenReturn204() throws Exception {
        mockMvc.perform(delete("/medias/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void get_whenMediaFound_thenReturn200WithBody() throws Exception {
        MediaVm mediaVm = new MediaVm(1L, "caption", "test.png", "image/png",
            "http://localhost/medias/1/file/test.png");
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        mockMvc.perform(get("/medias/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.caption").value("caption"))
            .andExpect(jsonPath("$.fileName").value("test.png"));
    }

    @Test
    void get_whenMediaNotFound_thenReturn404() throws Exception {
        when(mediaService.getMediaById(99L)).thenReturn(null);

        mockMvc.perform(get("/medias/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getByIds_whenMediasFound_thenReturn200WithList() throws Exception {
        List<MediaVm> medias = List.of(
            new MediaVm(1L, "caption", "test.png", "image/png", "http://localhost/url")
        );
        when(mediaService.getMediaByIds(any())).thenReturn(medias);

        mockMvc.perform(get("/medias").param("ids", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getByIds_whenEmpty_thenReturn404() throws Exception {
        when(mediaService.getMediaByIds(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/medias").param("ids", "999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getFile_whenFound_thenReturn200WithContentDisposition() throws Exception {
        ByteArrayInputStream content = new ByteArrayInputStream("file-content".getBytes());
        MediaDto mediaDto = MediaDto.builder()
            .content(content)
            .mediaType(MediaType.IMAGE_JPEG)
            .build();
        when(mediaService.getFile(1L, "test.jpg")).thenReturn(mediaDto);

        mockMvc.perform(get("/medias/1/file/test.jpg"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"test.jpg\""));
    }
}
