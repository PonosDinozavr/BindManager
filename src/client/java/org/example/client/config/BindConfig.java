package org.example.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class BindConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String name;
    private final Map<String, String> keyBindings;

    public BindConfig(String name) {
        this.name = name;
        this.keyBindings = new LinkedHashMap<>();
    }

    public BindConfig(String name, Map<String, String> keyBindings) {
        this.name = name;
        this.keyBindings = new LinkedHashMap<>(keyBindings);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getKeyBindings() {
        return keyBindings;
    }

    public void setKeyBinding(String translationKey, String boundKey) {
        keyBindings.put(translationKey, boundKey);
    }

    public void removeKeyBinding(String translationKey) {
        keyBindings.remove(translationKey);
    }

    public String getBoundKey(String translationKey) {
        return keyBindings.getOrDefault(translationKey, "");
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        JsonObject binds = new JsonObject();
        for (Map.Entry<String, String> entry : keyBindings.entrySet()) {
            binds.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("key_bindings", binds);
        return json;
    }

    public static BindConfig fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        Map<String, String> bindings = new LinkedHashMap<>();
        if (json.has("key_bindings")) {
            JsonObject binds = json.getAsJsonObject("key_bindings");
            for (String key : binds.keySet()) {
                bindings.put(key, binds.get(key).getAsString());
            }
        }
        return new BindConfig(name, bindings);
    }

    public String toJsonString() {
        return GSON.toJson(toJson());
    }

    public static BindConfig fromJsonString(String json) {
        return fromJson(GSON.fromJson(json, JsonObject.class));
    }

    public BindConfig copy() {
        return new BindConfig(name, new LinkedHashMap<>(keyBindings));
    }
}
