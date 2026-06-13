package com.nexus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class HttpApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private static String creds(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private void register(String username) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(creds(username, "Secret123!")))
                .andExpect(status().isCreated());
    }

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(creds(username, "Secret123!")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void registerLoginAndProtectedAccess() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(creds("h_alice", "Secret123!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("h_alice"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(creds("h_alice", "Secret123!")))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(creds("h_weak", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(creds("h_alice", "WrongPass1!")))
                .andExpect(status().isUnauthorized());

        String token = login("h_alice");

        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("h_alice"));
    }

    @Test
    void directMessageRoundTripAndAuthorization() throws Exception {
        register("h_x");
        register("h_y");
        register("h_z");
        String xToken = login("h_x");
        String zToken = login("h_z");

        String send = mvc.perform(post("/api/messages/direct").header("Authorization", "Bearer " + xToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"h_y\",\"content\":\"hello y\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderUsername").value("h_x"))
                .andReturn().getResponse().getContentAsString();
        long conversationId = json.readTree(send).get("conversationId").asLong();

        mvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + zToken))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/messages/direct").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"h_y\",\"content\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
