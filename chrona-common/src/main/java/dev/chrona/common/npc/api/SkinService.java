package dev.chrona.common.npc.api;

import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinService {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final Gson gson = new Gson();
    private final Map<String, Skin> cacheByName = new ConcurrentHashMap<>();

    public Skin fetchByPlayerName(String name) throws Exception {
        String key = name.toLowerCase();
        Skin cached = cacheByName.get(key);
        if (cached != null)
            return cached;

        HttpRequest r1 = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + key))
                .timeout(Duration.ofSeconds(5))
                .GET().build();
        HttpResponse<String> resp1 = http.send(r1, HttpResponse.BodyHandlers.ofString());
        if (resp1.statusCode() == 204 || resp1.statusCode() == 404)
            throw new IllegalArgumentException("Player not found: " + name);
        if (resp1.statusCode() / 100 != 2)
            throw new IllegalStateException("Mojang name->uuid failed: " + resp1.statusCode());

        JsonObject j1 = JsonParser.parseString(resp1.body()).getAsJsonObject();
        String uuidNoDash = j1.get("id").getAsString();

        HttpRequest r2 = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoDash + "?unsigned=false"))
                .timeout(Duration.ofSeconds(5))
                .GET().build();
        HttpResponse<String> resp2 = http.send(r2, HttpResponse.BodyHandlers.ofString());
        if (resp2.statusCode() / 100 != 2)
            throw new IllegalStateException("SessionServer profile fetch failed: " + resp2.statusCode());

        JsonObject j2 = JsonParser.parseString(resp2.body()).getAsJsonObject();
        var props = j2.getAsJsonArray("properties");
        if (props == null || props.isEmpty())
            throw new IllegalStateException("No texture properties for " + name);

        JsonObject textures = props.get(0).getAsJsonObject();
        String value = textures.get("value").getAsString();
        String signature = textures.get("signature").getAsString();

        Skin skin = new Skin(value, signature);
        cacheByName.put(key, skin);
        return skin;
    }
}
