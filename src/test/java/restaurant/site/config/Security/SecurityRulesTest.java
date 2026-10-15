package restaurant.site.config.Security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import restaurant.site.config.MessageSourceConfig;
import restaurant.site.controller.ItemController;
import restaurant.site.controller.OrderController;
import restaurant.site.controller.UserController;
import restaurant.site.dto.ItemDto;
import restaurant.site.dto.PageResponse;
import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import restaurant.site.repo.UserRepository;
import restaurant.site.services.application.ItemService;
import restaurant.site.services.application.OrderService;
import restaurant.site.services.application.UserService;
import restaurant.site.services.language.ChooseLanguageService;
import restaurant.site.services.security.AuthorizationService;
import restaurant.site.services.security.JwtService;
import restaurant.site.services.security.UserDetailsServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real HTTP requests through the REAL security chain: JwtAuthenticationFilter, SecurityConfig,
// @PreAuthorize, the 401/403 handler and the exception handler. Only the services are mocks,
// so no database and no Redis are needed.
@WebMvcTest(controllers = {ItemController.class, UserController.class, OrderController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class, JwtService.class,
        AuthorizationService.class, ChooseLanguageService.class, MessageSourceConfig.class})
@TestPropertySource(properties = {
        // 64 x 'A' = Base64 of 48 zero bytes: a valid (test only!) 384-bit key
        "application.security.jwt.secret-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "application.security.jwt.expiration=3600000"
})
class SecurityRulesTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;

    @MockitoBean UserDetailsServiceImpl userDetailsService; // wired by SecurityConfig, not used by the JWT flow
    @MockitoBean UserRepository userRepository;
    @MockitoBean ItemService itemService;
    @MockitoBean UserService userService;
    @MockitoBean OrderService orderService;

    // ---------- helpers ----------

    private String token(String email, User.Role role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        return jwtService.generateToken(user);
    }

    private String customerToken() { return token("sara@example.com", User.Role.USER); }
    private String adminToken() { return token("admin@example.com", User.Role.ADMIN); }

    private static String bearer(String token) { return "Bearer " + token; }

    private void sarahIsUserSeven() {
        User sara = new User();
        sara.setId(7L);
        sara.setEmail("sara@example.com");
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(sara));
    }

    private static ResultActions expectError(ResultActions result, int status, String messagePart) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message", containsString(messagePart)));
    }

    // ---------- not logged in / bad token: 401 as JSON, never a 500 ----------

    @Test
    void noToken_onAProtectedUrl_is401WithAJsonBody() throws Exception {
        expectError(mvc.perform(get("/orders/1")), 401, "Authentication is required");
    }

    @Test
    void garbageToken_is401_notA500() throws Exception {
        expectError(mvc.perform(get("/orders/1").header("Authorization", "Bearer not-a-real-token")),
                401, "Authentication is required");
    }

    @Test
    void tamperedToken_is401() throws Exception {
        String token = adminToken();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        expectError(mvc.perform(get("/orders/1").header("Authorization", bearer(tampered))), 401, "Authentication is required");
    }

    @Test
    void expiredToken_is401_onAProtectedUrl() throws Exception {
        JwtService shortLived = new JwtService();
        ReflectionTestUtils.setField(shortLived, "secretKey", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        ReflectionTestUtils.setField(shortLived, "expiration", 1L); // 1 millisecond
        ReflectionTestUtils.invokeMethod(shortLived, "init");
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole(User.Role.ADMIN);
        String expired = shortLived.generateToken(admin);
        Thread.sleep(50);

        expectError(mvc.perform(get("/orders/1").header("Authorization", bearer(expired))), 401, "Authentication is required");
    }

    @Test
    void expiredToken_onAPublicUrl_isIgnored_andThePageStillWorks() throws Exception {
        // the menu is public: a stale token must not break browsing
        when(itemService.getItems(any(), anyInt(), anyInt())).thenReturn(new PageResponse<ItemDto.Response>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/items").header("Authorization", "Bearer an-old-useless-token")).andExpect(status().isOk());
    }

    // ---------- the menu is public ----------

    @Test
    void theMenu_canBeReadWithoutLoggingIn() throws Exception {
        when(itemService.getItems(any(), anyInt(), anyInt())).thenReturn(new PageResponse<ItemDto.Response>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/items")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void changingTheMenu_stillNeedsALogin() throws Exception {
        expectError(mvc.perform(delete("/items/1")), 401, "Authentication is required");
        verify(itemService, never()).deleteItem(1L);
    }

    // ---------- roles: a customer is not an admin ----------

    @Test
    void aCustomer_cannotUseAdminEndpoints_403AsJson() throws Exception {
        expectError(mvc.perform(delete("/items/1").header("Authorization", bearer(customerToken()))), 403, "permission");
        expectError(mvc.perform(get("/orders").header("Authorization", bearer(customerToken()))), 403, "permission");
        expectError(mvc.perform(get("/users").header("Authorization", bearer(customerToken()))), 403, "permission");
        expectError(mvc.perform(put("/users/2/role").header("Authorization", bearer(customerToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}")), 403, "permission");

        verify(itemService, never()).deleteItem(1L); // the service was never reached
        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    void anAdmin_canUseAdminEndpoints() throws Exception {
        mvc.perform(delete("/items/1").header("Authorization", bearer(adminToken()))).andExpect(status().isOk());

        verify(itemService).deleteItem(1L);
    }

    // ---------- ownership: you only see your own account and orders ----------

    @Test
    void aCustomer_canReadTheirOwnAccount_butNotSomeoneElses() throws Exception {
        sarahIsUserSeven();
        when(userService.getById(7L)).thenReturn(new UserDto.Response());

        mvc.perform(get("/users/7").header("Authorization", bearer(customerToken()))).andExpect(status().isOk());
        expectError(mvc.perform(get("/users/8").header("Authorization", bearer(customerToken()))), 403, "permission");
        verify(userService, never()).getById(8L);
    }

    @Test
    void aCustomer_canReadTheirOwnOrders_butNotSomeoneElses() throws Exception {
        sarahIsUserSeven();
        when(orderService.getOrdersByUser(any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/orders/user/7").header("Authorization", bearer(customerToken()))).andExpect(status().isOk());
        expectError(mvc.perform(get("/orders/user/8").header("Authorization", bearer(customerToken()))), 403, "permission");
    }

    @Test
    void anAdmin_canReadAnyonesAccount() throws Exception {
        when(userService.getById(8L)).thenReturn(new UserDto.Response());

        mvc.perform(get("/users/8").header("Authorization", bearer(adminToken()))).andExpect(status().isOk());
    }

    // ---------- errors never leak internals ----------

    @Test
    void malformedJson_is400WithOurOwnMessage_noFrameworkTextLeaks() throws Exception {
        mvc.perform(post("/items").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("malformed")))
                .andExpect(content().string(not(containsString("jackson"))))
                .andExpect(content().string(not(containsString("Unexpected"))));
    }

    @Test
    void anUnknownUrl_asAnAdmin_is404Json() throws Exception {
        mvc.perform(get("/nothing-here").header("Authorization", bearer(adminToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
