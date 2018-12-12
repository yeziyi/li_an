package com.tencent.gamejoy.licode_androidclient.core.rtc;

import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.util.Map;

/**
 * Classes that implement this protocol will be called for RTC Client
 * event notification.
 *
 * @author watsonxie
 */
public interface ECClientDelegate {

    public void didChangeState(ECClient appClient, ECClientState state);

    public void didChangeConnectionState(ECClient appClient, PeerConnection.IceConnectionState state);

    public void didReceiveRemoteStream(ECClient appClient, MediaStream remoteStream, String streamId);

    public void didError(ECClient appClient, int errCode, String errMsg);

    public MediaStream streamToPublishByAppClient(ECClient appClient);

    public Map appClientRequestICEServers(ECClient appClient);
}
