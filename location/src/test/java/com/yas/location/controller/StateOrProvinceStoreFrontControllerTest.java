package com.yas.location.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.location.service.StateOrProvinceService;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StateOrProvinceStoreFrontController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class StateOrProvinceStoreFrontControllerTest {

    @MockitoBean
    private StateOrProvinceService stateOrProvinceService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getStateOrProvince_shouldReturnStatesByCountryId() throws Exception {
        given(stateOrProvinceService.getAllByCountryId(1L)).willReturn(List.of(
            new StateOrProvinceVm(100L, "Ho Chi Minh", "HCM", "Municipality", 1L),
            new StateOrProvinceVm(101L, "Ha Noi", "HN", "Municipality", 1L)
        ));

        mockMvc.perform(get("/storefront/state-or-provinces/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(100))
            .andExpect(jsonPath("$[0].name").value("Ho Chi Minh"))
            .andExpect(jsonPath("$[1].code").value("HN"));
    }
}
