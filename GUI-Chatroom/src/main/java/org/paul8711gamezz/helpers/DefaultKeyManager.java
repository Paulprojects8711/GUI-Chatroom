package org.paul8711gamezz.helpers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class DefaultKeyManager {

    private static final String GITHUB_URL = "https://raw.githubusercontent.com/Paulprojects8711/GUI-Chatroom/default-key/default_key.json";

    private static final Path CACHE_FILE = Paths.get(System.getProperty("user.home"), ".gui-chatroom", "default_key.json");

    public static JsonObject getDefaultKey() {
        try {
            // Ensure folder exists
            if (!Files.exists(CACHE_FILE.getParent())) {
                Files.createDirectories(CACHE_FILE.getParent());
            }

            // Try to download from GitHub
            try {
                URL url = new URL(GITHUB_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    String content = new String(is.readAllBytes());
                    Files.writeString(CACHE_FILE, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    is.close();
                }
            } catch (IOException e) {
                System.out.println("Could not download default key from GitHub, using cached version if available.");
            }

            // Load from cache
            if (Files.exists(CACHE_FILE)) {
                String jsonContent = Files.readString(CACHE_FILE);
                Gson gson = new Gson();
                return gson.fromJson(jsonContent, JsonObject.class);
            } else {
                System.out.println("No cached default key found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        JsonObject keyData = getDefaultKey();
        if (keyData != null) {
            System.out.println("Default Key Version: " + keyData.get("version").getAsInt());
            System.out.println("Default Key: " + keyData.get("key").getAsString());
        }
    }
}
