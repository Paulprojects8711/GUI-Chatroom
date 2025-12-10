package org.paul8711gamezz.helpers;

import java.util.function.Consumer;

public interface ClientUIHandler {
    void onError(String msg);
    void onDisconnect(String reason);
    void onAuthRequest(Consumer<String> callback);
    void onAuthCorrect();
}