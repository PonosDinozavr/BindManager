package org.example.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.example.client.config.BindConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BindConfigStore {
    private static final String PROFILES_DIR = "changeofcontrol/profiles";

    private final Path profilesPath;
    private final Map<String, BindConfig> profiles = new LinkedHashMap<>();
    private String activeProfileName;

    public BindConfigStore(MinecraftClient client) {
        this.profilesPath = client.runDirectory.toPath().resolve("config").resolve(PROFILES_DIR);
        try {
            Files.createDirectories(profilesPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadAllProfiles();
    }

    private void loadAllProfiles() {
        profiles.clear();
        try {
            if (Files.exists(profilesPath)) {
                try (var stream = Files.list(profilesPath)) {
                    stream.filter(p -> p.toString().endsWith(".json"))
                            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                            .forEach(this::loadProfile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProfile(Path path) {
        try {
            String content = Files.readString(path);
            BindConfig config = BindConfig.fromJsonString(content);
            profiles.put(config.getName(), config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path getProfilePath(String name) {
        return profilesPath.resolve(sanitizeFileName(name) + ".json");
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    public void saveProfile(String name) {
        saveProfile(name, null, false, BindConfig.DEFAULT_COLOR);
    }

    public void saveProfile(String name, Boolean favorite, int color) {
        saveProfile(name, null, favorite != null && favorite, color);
    }

    public void saveProfile(String name, Map<String, String> existingBindings, boolean favorite, int color) {
        BindConfig config = new BindConfig(name);
        if (existingBindings != null) {
            config.getKeyBindings().putAll(existingBindings);
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null) {
                for (KeyBinding binding : client.options.allKeys) {
                    String boundKey = binding.getBoundKeyTranslationKey();
                    if (boundKey != null && !boundKey.isEmpty() && !"key.keyboard.unknown".equals(boundKey)) {
                        config.setKeyBinding(binding.getTranslationKey(), boundKey);
                    }
                }
            }
        }
        config.setFavorite(favorite);
        config.setColor(color);
        while (profiles.containsKey(config.getName())) {
            int counter = 1;
            while (profiles.containsKey(name + " (" + counter + ")")) {
                counter++;
            }
            config.setName(name + " (" + counter + ")");
        }
        profiles.put(config.getName(), config);
        saveProfileToFile(config);
    }

    public void saveExistingProfile(BindConfig config) {
        profiles.put(config.getName(), config);
        saveProfileToFile(config);
    }

    private void saveProfileToFile(BindConfig config) {
        try {
            Path path = getProfilePath(config.getName());
            Files.writeString(path, config.toJsonString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteProfile(String name) {
        profiles.remove(name);
        try {
            Files.deleteIfExists(getProfilePath(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void renameProfile(String oldName, String newName) {
        BindConfig config = profiles.remove(oldName);
        if (config != null) {
            try {
                Files.deleteIfExists(getProfilePath(oldName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            config.setName(newName);
            profiles.put(newName, config);
            saveProfileToFile(config);
        }
    }

    public void moveProfile(int fromIndex, int toIndex) {
        List<BindConfig> list = new ArrayList<>(profiles.values());
        if (fromIndex < 0 || fromIndex >= list.size() || toIndex < 0 || toIndex >= list.size()) return;
        BindConfig moved = list.remove(fromIndex);
        list.add(toIndex, moved);
        profiles.clear();
        for (BindConfig c : list) {
            profiles.put(c.getName(), c);
        }
    }

    public String getActiveProfileName() {
        return activeProfileName;
    }

    public BindConfig getActiveProfile() {
        return activeProfileName != null ? profiles.get(activeProfileName) : null;
    }

    public void setActiveProfile(String name) {
        this.activeProfileName = name;
    }

    public void loadProfile(String name) {
        BindConfig config = profiles.get(name);
        if (config == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;

        GameOptions options = client.options;
        for (KeyBinding binding : options.allKeys) {
            String boundKeyStr = config.getBoundKey(binding.getTranslationKey());
            if (!boundKeyStr.isEmpty()) {
                InputUtil.Key key = InputUtil.fromTranslationKey(boundKeyStr);
                binding.setBoundKey(key);
            }
        }
        options.write();
        activeProfileName = name;
    }

    public List<BindConfig> getProfiles() {
        return new ArrayList<>(profiles.values());
    }

    public BindConfig getProfile(String name) {
        return profiles.get(name);
    }

    public boolean profileExists(String name) {
        return profiles.containsKey(name);
    }

    public void refresh() {
        loadAllProfiles();
    }
}
