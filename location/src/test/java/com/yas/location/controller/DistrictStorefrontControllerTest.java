package com.yas.location.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.location.service.DistrictService;
import com.yas.location.viewmodel.district.DistrictGetVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DistrictStorefrontController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class DistrictStorefrontControllerTest {

    @MockitoBean
    private DistrictService districtService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getList_byStorefrontPath_shouldReturnDistricts() throws Exception {
        given(districtService.getList(1L)).willReturn(List.of(
            new DistrictGetVm(10L, "District 1"),
            new DistrictGetVm(20L, "District 2")
        ));

        mockMvc.perform(get("/storefront/district/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].name").value("District 1"))
            .andExpect(jsonPath("$[1].id").value(20));
    }

    @Test
    void getList_byBackofficePath_shouldReturnDistricts() throws Exception {
        given(districtService.getList(2L)).willReturn(List.of(new DistrictGetVm(30L, "District 3")));

        mockMvc.perform(get("/backoffice/district/2").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(30))
            .andExpect(jsonPath("$[0].name").value("District 3"));
    }
}
