package org.paul8711gamezz.helpers;

// used to store status of users (if they are muted and stuff)
public class VCInfo {
    public boolean inVC;
    public boolean mute;
    public boolean deaf;

    public VCInfo(boolean inVC, boolean mute, boolean deaf) {
        this.inVC = inVC;
        this.mute = mute;
        this.deaf = deaf;
    }
}
