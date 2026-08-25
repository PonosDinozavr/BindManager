package org.example.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class BindConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int DEFAULT_COLOR = 0x555555;

    private String name;
    private boolean favorite;
    private int color;
    private final Map<String, String> keyBindings;

    public BindConfig(String name) {
        this(name, new LinkedHashMap<>(), false, DEFAULT_COLOR);
    }

    public BindConfig(String name, Map<String, String> keyBindings) {
        this(name, keyBindings, false, DEFAULT_COLOR);
    }

    public BindConfig(String name, Map<String, String> keyBindings, boolean favorite, int color) {
        this.name = name;
        this.keyBindings = new LinkedHashMap<>(keyBindings);
        this.favorite = favorite;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
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
        json.addProperty("favorite", favorite);
        json.addProperty("color", color);
        JsonObject binds = new JsonObject();
        for (Map.Entry<String, String> entry : keyBindings.entrySet()) {
            binds.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("key_bindings", binds);
        return json;
    }

    public static BindConfig fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        boolean favorite = json.has("favorite") && json.get("favorite").getAsBoolean();
        int color = json.has("color") ? json.get("color").getAsInt() : DEFAULT_COLOR;
        Map<String, String> bindings = new LinkedHashMap<>();
        if (json.has("key_bindings")) {
            JsonObject binds = json.getAsJsonObject("key_bindings");
            for (String key : binds.keySet()) {
                bindings.put(key, binds.get(key).getAsString());
            }
        }
        return new BindConfig(name, bindings, favorite, color);
    }

    public String toJsonString() {
        return GSON.toJson(toJson());
    }

    public static BindConfig fromJsonString(String json) {
        return fromJson(GSON.fromJson(json, JsonObject.class));
    }

    public BindConfig copy() {
        return new BindConfig(name, new LinkedHashMap<>(keyBindings), favorite, color);
    }
}
