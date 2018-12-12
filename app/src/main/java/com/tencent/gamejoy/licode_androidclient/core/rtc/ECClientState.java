package com.tencent.gamejoy.licode_androidclient.core.rtc;

public enum ECClientState {
    ECClientStateDisconnected,/// Disconnected
    ECClientStateReady,/// Ready to signaling
    ECClientStateConnecting,/// Signaling proccess
    ECClientStateConnected/// Signlaning done
}