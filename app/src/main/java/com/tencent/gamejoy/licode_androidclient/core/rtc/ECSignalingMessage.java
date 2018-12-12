package com.tencent.gamejoy.licode_androidclient.core.rtc;

import android.text.TextUtils;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class ECSignalingMessage {

    public ECSignalingMessageType type;

    private String streamId;
    private String peerSocketId;

    public ECSignalingMessage(ECSignalingMessageType type, String streamId, String peerSocketId) {
        assert !TextUtils.isEmpty(streamId);
        this.type = type;
        this.streamId = streamId;
        this.peerSocketId = peerSocketId;
    }

    public static class ECICECandidateMessage extends ECSignalingMessage {

        public IceCandidate iceCandidate;

        public ECICECandidateMessage(IceCandidate iceCandidate, String streamId, String peerSocketId) {
            super(ECSignalingMessageType.kECSignalingMessageTypeCandidate, streamId, peerSocketId);
            this.iceCandidate = iceCandidate;
        }
    }

    public static class ECSessionDescriptionMessage extends ECSignalingMessage {

        public SessionDescription sessionDescription;

        public ECSessionDescriptionMessage(SessionDescription sessionDescription, String streamId, String peerSocketId) {
            super(ECSignalingMessageType.kECSignalingMessageTypeOffer, streamId, peerSocketId);
            if (sessionDescription.type == SessionDescription.Type.OFFER) {
                type = ECSignalingMessageType.kECSignalingMessageTypeOffer;
            } else if (sessionDescription.type == SessionDescription.Type.ANSWER) {
                type = ECSignalingMessageType.kECSignalingMessageTypeAnswer;
            } else {
                throw new AssertionError("Unexpected sdp type: " + sessionDescription.type);
            }
            this.sessionDescription = sessionDescription;
        }
    }
}



