package ru.nifreebie.infoseclab1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.nifreebie.infoseclab1.repository.LogRepository;
import ru.nifreebie.infoseclab1.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecureLogApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogRepository logRepository;

    @BeforeEach
    void cleanDatabase() {
        logRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationReturnsUuidAndStoresOnlyPasswordHash() throws Exception {
        String response = register("Student", "StrongPassword123!");

        UUID returnedId = UUID.fromString(objectMapper.readTree(response).path("id").asText());
        var savedUser = userRepository.findById(returnedId).orElseThrow();

        assertThat(savedUser.getUsername()).isEqualTo("student");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("StrongPassword123!");
        assertThat(savedUser.getPasswordHash()).startsWith("$2");
    }

    @Test
    void protectedEndpointRejectsRequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateAndReadEscapedLog() throws Exception {
        register("student", "StrongPassword123!");
        String token = login("student", "StrongPassword123!");
        String payload = objectMapper.writeValueAsString(Map.of(
                "level", "WARNING",
                "source", "<service>",
                "message", "<script>alert('x')</script>",
                "eventTime", Instant.now().minusSeconds(1).toString()
        ));

        String created = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.source").value("&lt;service&gt;"))
                .andExpect(jsonPath("$.message").value("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"))
                .andReturn().getResponse().getContentAsString();

        UUID.fromString(objectMapper.readTree(created).path("id").asText());

        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("&lt;service&gt;"));
    }

    @Test
    void usersCannotSeeEachOthersLogs() throws Exception {
        register("alice", "StrongPassword123!");
        register("bob", "AnotherPassword456!");
        String aliceToken = login("alice", "StrongPassword123!");
        String bobToken = login("bob", "AnotherPassword456!");
        String payload = objectMapper.writeValueAsString(Map.of(
                "level", "INFO",
                "source", "billing-service",
                "message", "Invoice created",
                "eventTime", Instant.now().minusSeconds(1).toString()
        ));

        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void loginDoesNotRevealWhetherUsernameExists() throws Exception {
        register("student", "StrongPassword123!");
        String wrongPasswordResponse = failedLogin("student", "WrongPassword123!");
        String unknownUserResponse = failedLogin("unknown", "WrongPassword123!");

        JsonNode wrongPassword = objectMapper.readTree(wrongPasswordResponse);
        JsonNode unknownUser = objectMapper.readTree(unknownUserResponse);
        assertThat(wrongPassword.path("message").asText())
                .isEqualTo(unknownUser.path("message").asText())
                .isEqualTo("Invalid username or password");
    }

    private String register(String username, String password) throws Exception {
        return mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private String failedLogin(String username, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
    }
}
