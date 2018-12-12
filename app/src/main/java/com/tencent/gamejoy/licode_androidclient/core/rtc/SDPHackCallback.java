package com.tencent.gamejoy.licode_androidclient.core.rtc;

import org.webrtc.SessionDescription;

public interface SDPHackCallback {

    public SessionDescription sdpHackCallback(SessionDescription description);
}
