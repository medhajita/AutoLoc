package autoloc;

import autoloc.config.SecurityConfig;
import autoloc.controller.SecurityDemoController;
import autoloc.repository.AgenceRepository;
import autoloc.repository.CompteClientRepository;
import autoloc.repository.VehiculeRepository;
import autoloc.web.controller.VehiculeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SecurityDemoController.class, VehiculeController.class})
@Import(SecurityConfig.class)
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehiculeRepository vehiculeRepository;

    @MockitoBean
    private AgenceRepository agenceRepository;

    @MockitoBean
    private CompteClientRepository compteClientRepository;

    @Test
    void publicPingIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("AutoLoc API is running"));
    }

    @Test
    void vehiclesRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/vehicules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vehiclesAcceptAdminAuthentication() throws Exception {
        when(vehiculeRepository.findAll()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/vehicules").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void adminDashboardRejectsClientRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").with(user("marie.dupont@autoloc.com").roles("CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDashboardAcceptsAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin dashboard — restricted access"));
    }
}
