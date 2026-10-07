package com.draughts.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GameApiIntegrationTest extends IntegrationTest {

    @Test
    void guestGetsAPersistentIdentity() {
        var client = newClient();
        var first = client.get("/api/me");
        var second = client.get("/api/me");

        assertThat(first.status()).isEqualTo(200);
        assertThat(first.body().get("displayName").asString()).startsWith("Guest-");
        assertThat(second.body().get("id")).isEqualTo(first.body().get("id"));
    }

    @Test
    void playAgainstTheComputer() throws Exception {
        var client = newClient();
        var created = client.post("/api/games", """
                {"opponent":"AI","color":"BLACK","difficulty":"EASY"}""");
        assertThat(created.status()).isEqualTo(201);
        var id = created.body().get("id").asString();
        assertThat(created.body().get("yourColor").asString()).isEqualTo("BLACK");
        assertThat(created.body().get("legalMoves")).hasSize(7);

        var moved = client.post("/api/games/" + id + "/moves", """
                {"from":11,"path":[15],"expectedVersion":0}""");
        assertThat(moved.status()).isEqualTo(200);
        assertThat(moved.body().get("turn").asString()).isEqualTo("WHITE");

        var deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        var game = client.get("/api/games/" + id).body();
        while (game.get("moves").size() < 2 && System.nanoTime() < deadline) {
            Thread.sleep(100);
            game = client.get("/api/games/" + id).body();
        }
        assertThat(game.get("moves")).hasSize(2);
        assertThat(game.get("turn").asString()).isEqualTo("BLACK");
        assertThat(game.get("white").get("computer").asBoolean()).isTrue();

        var history = client.get("/api/games").body();
        assertThat(history.valueStream().map(g -> g.get("id").asString())).contains(id);
    }

    @Test
    void illegalMovesAreRejectedWithProblemDetails() {
        var client = newClient();
        var id = client.post("/api/games", """
                {"opponent":"AI","color":"BLACK","difficulty":"EASY"}""").body().get("id").asString();

        var response = client.post("/api/games/" + id + "/moves", """
                {"from":11,"path":[19]}""");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.body().get("code").asString()).isEqualTo("illegal-move");
        assertThat(newClient().get("/api/games/00000000-0000-0000-0000-000000000000").status()).isEqualTo(404);
    }

    @Test
    void twoPlayersReceiveUpdatesOverWebSocket() throws Exception {
        var alice = newClient();
        var bob = newClient();
        var id = alice.post("/api/games", """
                {"opponent":"HUMAN","color":"BLACK"}""").body().get("id").asString();
        assertThat(bob.get("/api/lobby").body().valueStream().map(g -> g.get("id").asString())).contains(id);
        assertThat(bob.post("/api/games/" + id + "/join", null).body().get("yourColor").asString()).isEqualTo("WHITE");

        var stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new StringMessageConverter() {
            @Override
            protected boolean supportsMimeType(MessageHeaders headers) {
                return true; // read the server's application/json frames as text
            }
        });
        var headers = new WebSocketHttpHeaders();
        headers.add("Cookie", bob.cookieHeader());
        var session = stomp.connectAsync(URI.create("ws://localhost:" + port + "/ws"), headers, new StompHeaders(),
                new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
        BlockingQueue<String> updates = new LinkedBlockingQueue<>();
        session.subscribe("/topic/games/" + id, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                updates.add((String) payload);
            }
        });
        Thread.sleep(300); // let the subscription register

        assertThat(alice.post("/api/games/" + id + "/moves", """
                {"from":11,"path":[15]}""").status()).isEqualTo(200);

        var update = updates.poll(5, TimeUnit.SECONDS);
        assertThat(update).contains("\"notation\":\"11-15\"").contains("\"turn\":\"WHITE\"");

        // Bob replies over STOMP instead of HTTP.
        var send = new StompHeaders();
        send.setDestination("/app/games/" + id + "/move");
        send.setContentType(MimeTypeUtils.APPLICATION_JSON);
        session.send(send, "{\"from\":22,\"path\":[18]}");
        assertThat(updates.poll(5, TimeUnit.SECONDS)).contains("\"notation\":\"22-18\"");
        session.disconnect();
    }
}
