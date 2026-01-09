package org.paul8711gamezz.helpers;

import javax.sound.sampled.Mixer;

public class SpeakerItem {
    public String name;
    public Mixer.Info mixerInfo;

    public SpeakerItem(String name, Mixer.Info mixerInfo) {
        this.name = name;
        this.mixerInfo = mixerInfo;
    }
}
