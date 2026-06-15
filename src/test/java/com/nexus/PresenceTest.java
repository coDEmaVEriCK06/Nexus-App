package com.nexus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PresenceTest extends AbstractIntegrationTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper json;

    @Test
    void contactIsNotifiedWhenAUserConnectsAndDisconnects() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String alice = "pres_alice_" + suffix;
        String bob = "pres_bob_" + suffix;
        register(alice);
        register(bob);
        String aliceToken = login(alice);
        String bobToken = login(bob);

        // a DM makes alice and bob contacts, so they hear each other's presence
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(aliceToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        rest.postForEntity("/api/messages/direct",
                new HttpEntity<>("{\"recipientUsername\":\"" + bob + "\",\"content\":\"hi\"}", headers),
                String.class);

        // bob connects and watches for presence updates
        WebSocketStompClient bobClient = stompClient();
        StompSession bobSession = connect(bobClient, bobToken);
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        bobSession.subscribe("/user/queue/presence", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders h) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders h, Object payload) {
                received.add((Map<String, Object>) payload);
            }
        });
        Thread.sleep(500);

        // alice connects -> bob should be told alice is online
        WebSocketStompClient aliceClient = stompClient();
        StompSession aliceSession = connect(aliceClient, aliceToken);

        Map<String, Object> online = received.poll(5, TimeUnit.SECONDS);
        assertThat(online).isNotNull();
        assertThat(online.get("username")).isEqualTo(alice);
        assertThat(online.get("online")).isEqualTo(true);

        // alice disconnects -> bob should be told alice is offline
        aliceSession.disconnect();
        aliceClient.stop();

        Map<String, Object> offline = received.poll(5, TimeUnit.SECONDS);
        assertThat(offline).isNotNull();
        assertThat(offline.get("username")).isEqualTo(alice);
        assertThat(offline.get("online")).isEqualTo(false);

        bobSession.disconnect();
        bobClient.stop();
    }

    private WebSocketStompClient stompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        return client;
    }

    private StompSession connect(WebSocketStompClient client, String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
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
