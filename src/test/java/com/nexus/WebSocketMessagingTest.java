package com.nexus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketMessagingTest extends AbstractIntegrationTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper json;

    @Test
    void messageSentOverRestArrivesOnSubscribersWebSocket() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String alice = "w_alice_" + suffix;
        String bob = "w_bob_" + suffix;
        register(alice);
        register(bob);
        String aliceToken = login(alice);
        String bobToken = login(bob);

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + bobToken);

        StompSession session = client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) payload;
                received.add(body);
            }
        });

        // let the SUBSCRIBE frame register before sending
        Thread.sleep(500);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(aliceToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity("/api/messages/direct",
                new HttpEntity<>("{\"recipientUsername\":\"" + bob + "\",\"content\":\"live hello\"}", headers),
                String.class);

        Map<String, Object> message = received.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.get("senderUsername")).isEqualTo(alice);
        assertThat(message.get("content")).isEqualTo("live hello");

        session.disconnect();
        client.stop();
    }

    @Test
    void groupRoleChangeArrivesAsSystemMessageOverWebSocket() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String admin = "ws_admin_" + suffix;
        String member = "ws_member_" + suffix;
        register(admin);
        register(member);
        String adminToken = login(admin);
        String memberToken = login(member);

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);

        String groupBody = rest.postForEntity("/api/groups",
                new HttpEntity<>("{\"name\":\"ws team\",\"memberUsernames\":[\"" + member + "\"]}", adminHeaders),
                String.class).getBody();
        long conversationId = json.readTree(groupBody).get("conversationId").asLong();

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + memberToken);

        StompSession session = client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) payload;
                received.add(body);
            }
        });

        Thread.sleep(500);

        // admin promotes the member -> a SYSTEM notice fans out to the member's socket
        rest.exchange("/api/groups/" + conversationId + "/members/" + member + "/role",
                HttpMethod.PUT,
                new HttpEntity<>("{\"role\":\"ADMIN\"}", adminHeaders),
                String.class);

        Map<String, Object> message = received.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message.get("type")).isEqualTo("SYSTEM");
        assertThat(message.get("content").toString()).contains("promoted " + member + " to admin");

        session.disconnect();
        client.stop();
    }

    @Test
    void connectingWithoutTokenIsRejected() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());

        assertThatThrownBy(() -> client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        new StompHeaders(),
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);

        client.stop();
    }

    private void register(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity("/api/auth/register",
                new HttpEntity<>("{\"username\":\"" + username + "\",\"password\":\"Secret123!\"}", headers),
                String.class);
    }

    private String login(String username) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = rest.postForEntity("/api/auth/login",
                new HttpEntity<>("{\"username\":\"" + username + "\",\"password\":\"Secret123!\"}", headers),
                String.class).getBody();
        return json.readTree(body).get("token").asText();
    }
}
