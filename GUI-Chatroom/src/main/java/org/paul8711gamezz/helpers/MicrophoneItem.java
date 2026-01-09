package org.paul8711gamezz.helpers;

import javax.sound.sampled.Mixer;

public class MicrophoneItem {
    public String name;
    public Mixer.Info mixerInfo;

    public MicrophoneItem(String name, Mixer.Info mixerInfo) {
        this.name = name;
        this.mixerInfo = mixerInfo;
    }
}
