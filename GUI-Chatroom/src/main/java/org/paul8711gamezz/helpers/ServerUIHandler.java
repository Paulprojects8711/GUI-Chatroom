package org.paul8711gamezz.helpers;

import java.util.Map;

// ui handler for the server (basically everything that the server would otherwise dump to console is sent to the ui)
public interface ServerUIHandler {
    void onError(String msg);
    void onStop(String reason);
    void onUserListUpdate(Map<String, String> userMap);
    void onVCListUpdate(Map<String, VCInfo> vcStatus, Map<String, String> userMap);
    void onLog(String msg);
}
