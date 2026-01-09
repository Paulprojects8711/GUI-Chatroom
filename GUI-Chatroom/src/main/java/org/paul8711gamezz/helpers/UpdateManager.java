package org.paul8711gamezz.helpers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public class UpdateManager {

    private static final String OWNER = "Paulprojects8711";
    private static final String REPO = "GUI-Chatroom";

    private static final Gson GSON = new Gson();

    // checks for updates and triggers callback if new version (basically tells the GUI to ask if you want to download now
    // TriConsumer "values": latestVersion, localVersion, releaseJson
    public static void checkForUpdate(TriConsumer<String, String, JsonObject> updateAvailableCallback) {
        try {
            URL apiUrl = new URL("https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "Java-Updater");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return;

            JsonObject release;
            try (InputStream is = conn.getInputStream()) {
                release = GSON.fromJson(new String(is.readAllBytes()), JsonObject.class);
            }

            String latestTag = release.get("tag_name").getAsString().replace("v", "").trim();
            String localVersion = getLocalVersion();

            boolean updateAvailable = false;

            if (localVersion != null) {
                String[] localVersionSplit = localVersion.replace("-SNAPSHOT", "").split("\\.", 2);

                String[] latestTagSplit = latestTag.split("\\.", 2);
                if (Integer.parseInt(localVersionSplit[0]) <= Integer.parseInt(latestTagSplit[0]) && Integer.parseInt(localVersionSplit[1]) < Integer.parseInt(latestTagSplit[1])) {
                    updateAvailable = true;
                }
            }

            // only trigger callback if update is available
            if (localVersion == null || updateAvailable) {
                if (updateAvailableCallback != null) {
                    updateAvailableCallback.accept(latestTag, localVersion, release);
                }
            }

        } catch (IOException e) {
            System.out.println("Network error: " + e.getMessage());
        }
    }

    // downloads the update
    // latestTag: latest version
    // oldVersion: current version we have locally
    // release: JSON object from github
    // progressCallback: callback for progress bar
    // returns true if download succeded
    public static boolean downloadUpdate(String latestTag, String oldVersion, JsonObject release, Consumer<Integer> progressCallback) {
        try {
            // Find first JAR file on github
            JsonArray assets = release.getAsJsonArray("assets");
            String downloadUrl = null;
            for (int i = 0; i < assets.size(); i++) {
                JsonObject asset = assets.get(i).getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (name.endsWith(".jar")) {
                    downloadUrl = asset.get("browser_download_url").getAsString();
                    break;
                }
            }
            if (downloadUrl == null) {
                System.out.println("No JAR asset found in latest release.");
                return false;
            }

            // Backup old JAR (if exists, probably does)
            if (oldVersion != null) {
                Path oldJar = getLocalJarPath(oldVersion);
                Path backupJar = getBackupJarPath(oldVersion);
                if (Files.exists(oldJar)) {
                    Files.copy(oldJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Backup created: " + backupJar);
                }
            }

            // download new
            Path newJar = getLocalJarPath(latestTag);
            boolean success = downloadJarWithProgress(downloadUrl, newJar, progressCallback);
            if (!success) {
                // restore backup if download failed
                if (oldVersion != null) {
                    Path backupJar = getBackupJarPath(oldVersion);
                    Path oldJar = getLocalJarPath(oldVersion);
                    if (Files.exists(backupJar)) {
                        Files.copy(backupJar, oldJar, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Backup restored: " + oldJar);
                    }
                }
                return false;
            }

            System.out.println("Updated to version " + latestTag);
            return true;

        } catch (IOException e) {
            System.out.println("Error during download/update: " + e.getMessage());
            return false;
        }
    }

    private static boolean downloadJarWithProgress(String urlString, Path destinationJar, Consumer<Integer> progressCallback) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Java-Updater");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            int contentLength = conn.getContentLength();
            if (contentLength <= 0) contentLength = -1;

            // downloading witchcraft
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(destinationJar.toFile())) {
                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (contentLength > 0 && progressCallback != null) {
                        totalRead += read;
                        int percent = (int) ((totalRead * 100) / contentLength);
                        progressCallback.accept(percent);
                    }
                }
            }
            if (progressCallback != null) progressCallback.accept(100);
            return true;

        } catch (IOException e) {
            System.out.println("Error downloading JAR: " + e.getMessage());
            return false;
        }
    }

    // returns the version of the current jar
    public static String getLocalVersion() {
        Package pkg = UpdateManager.class.getPackage();
        return pkg != null ? pkg.getImplementationVersion() : null;
    }

    // restart the jar so it uses the new version
    public static void restartJar(String latestTag) {
        try {
            String javaBin = System.getProperty("java.home") + "/bin/java";
            String jarPath = getLocalJarPath(latestTag).toAbsolutePath().toString();

            ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", jarPath);
            builder.inheritIO();
            builder.start();
            System.out.println("Restarting application...");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Failed to restart JAR: " + e.getMessage());
        }
    }

    // gets path to local (new) jar by version
    private static Path getLocalJarPath(String version) {
        return Paths.get(System.getProperty("user.dir"), "GUI-Chatroom-" + version + ".jar");
    }

    // gets the path to the backupped jar with the oldVersion
    private static Path getBackupJarPath(String oldVersion) {
        return Paths.get(System.getProperty("user.dir"), "GUI-Chatroom-" + oldVersion + "-backup.jar");
    }
}