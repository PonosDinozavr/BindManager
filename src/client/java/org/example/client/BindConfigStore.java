package org.example.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PROFILES_DIR = "bindmanager/profiles";

    private final Path profilesPath;
    private final Map<String, BindConfig> profiles = new LinkedHashMap<>();

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
        BindConfig config = new BindConfig(name);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            for (KeyBinding binding : client.options.allKeys) {
                InputUtil.Key boundKey = binding.getBoundKey();
                if (boundKey != null) {
                    config.setKeyBinding(binding.getTranslationKey(), boundKey.getTranslationKey());
                }
            }
        }
        profiles.put(name, config);
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
        KeyBinding.updateKeysByCode();
        options.write();
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
