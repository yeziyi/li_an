package com.tencent.gamejoy.licode_androidclient.core.rtc;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.gamejoy.licode_androidclient.MainLooper;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.tencent.gamejoy.licode_androidclient.core.rtc.ECClientState.ECClientStateConnected;
import static com.tencent.gamejoy.licode_androidclient.core.rtc.ECClientState.ECClientStateConnecting;
import static com.tencent.gamejoy.licode_androidclient.core.rtc.ECClientState.ECClientStateDisconnected;
import static com.tencent.gamejoy.licode_androidclient.core.rtc.ECClientState.ECClientStateReady;

// TODO Nullable

/**
 * rtc client
 *
 * @author watsonxie
 */
public class ECClient implements PeerConnection.Observer, ECSignalingChannelDelegate {

    private static final String TAG = "ECClient";

    @Nullable
    private PeerConnection peerConnection;
    private PeerConnectionFactory factory;
    private final List<ECSignalingMessage> messageQueue = new ArrayList<>();
    private boolean hasReceivedSdp;
    @Nullable
    private ECSignalingChannel signalingChannel;
    private boolean isInitiator;
    private List<PeerConnection.IceServer> iceServers;
    private Map clientOptions;

    @Nullable
    private SDPHackCallback sdpHackCallback;
    public static String preferredVideoCodec;
    public static String defaultVideoCodec = "VP8";
    public static final String kECAppClientErrorDomain = "ECAppClient";
    public static final int kECAppClientErrorCreateSDP = -3;
    public static final int kECAppClientErrorSetSDP = -4;
    public static final int kKbpsMultiplier = 1000;

    public static final String kClientOptionMaxVideoBW = "maxVideoBW";
    public static final String kClientOptionMaxAudioBW = "maxAudioBW";

    @Nullable
    private ECClientDelegate delegate;/// ECClientDelegate instance.
    private Map serverConfiguration;/// Server configuration for this client.
    private MediaStream localStream;/// Local Stream assigned to this client.
    private long maxBitrate;/// Max bitrate allowed for this client to use.
    private boolean limitBitrate;/// Should bitrate be limited to `maxBitrate` value?
    private String peerSocketId;/// Peer socket id assigned by Licode for signaling P2P connections.
    private String streamId;/// The streamId

    private ECClientState state;
    private String username;


    @Override
    public void onSignalingChange(final PeerConnection.SignalingState signalingState) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Signaling state changed: " + signalingState);
            }
        });
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, "ICE state changed: " + iceConnectionState);
        switch (iceConnectionState) {
            case NEW:
            case COMPLETED:
                break;
            case CHECKING:
                break;
            case CONNECTED: {
                setState(ECClientStateConnected);
                break;
            }
            case FAILED: {
                Log.w(TAG, "RTCIceConnectionStateFailed " + peerConnection);
                break;
            }
            case CLOSED:
            case DISCONNECTED: {
                disconnect();
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
    }

    @Override
    public void onIceGatheringChange(final PeerConnection.IceGatheringState iceGatheringState) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "ICE gathering state changed: " + iceGatheringState);
                if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE && signalingChannel != null) {
                    signalingChannel.drainMessageQueueForStreamId(streamId, peerSocketId);
                }
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate iceCandidate) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Generated ICE candidate: " + iceCandidate);
                {
                    MainLooper.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            ECSignalingMessage.ECICECandidateMessage message = new ECSignalingMessage.ECICECandidateMessage(iceCandidate, streamId, peerSocketId);
                            if (signalingChannel != null) {
                                signalingChannel.enqueueSignalingMessage(message);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] iceCandidates) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Remove ICE candidates: " + iceCandidates);
            }
        });
    }

    @Override
    public void onAddStream(final MediaStream mediaStream) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Received " + mediaStream.videoTracks.size() + " video tracks and " + mediaStream.audioTracks.size() + " audio tracks");
                if (delegate != null) {
                    delegate.didReceiveRemoteStream(ECClient.this, mediaStream, streamId);
                }
            }
        });
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream mediaStream = " + mediaStream);
    }

    @Override
    public void onDataChannel(final DataChannel dataChannel) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "DataChannel Did open DataChannel " + dataChannel);
            }
        });
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.w(TAG, "Renegotiation needed but unimplemented.");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {
    }

    public ECClient() {
        limitBitrate = false;
        maxBitrate = 1000;
    }

    public ECClient(ECClientDelegate delegate) {
        this();
        this.delegate = delegate;
        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder();
        this.factory = builder.createPeerConnectionFactory();
    }

    public ECClient(ECClientDelegate delegate, PeerConnectionFactory factory) {
        this();
        this.delegate = delegate;
        this.factory = factory;
    }

    public ECClient(ECClientDelegate delegate, PeerConnectionFactory factory, String peerSocketId) {
        this(delegate, factory);
        this.peerSocketId = peerSocketId;
    }

    public ECClient(ECClientDelegate delegate, PeerConnectionFactory factory, String streamId, String peerSocketId) {
        this(delegate, factory, peerSocketId);
        this.streamId = streamId;
    }

    public ECClient(ECClientDelegate delegate, PeerConnectionFactory factory, String streamId, String peerSocketId, Map options) {
        this(delegate, factory, streamId, peerSocketId);
        this.clientOptions = options;
    }

    public void setState(ECClientState newState) {
        if (this.state == newState) {
            return;
        }
        state = newState;
        if (this.delegate != null) {
            this.delegate.didChangeState(this, state);
        }
    }

    public void disconnect() {
        if (state == ECClientStateDisconnected) {
            return;
        }
        if (peerConnection != null) {
            peerConnection.close();
        }
        isInitiator = false;
        hasReceivedSdp = false;
        messageQueue.clear();
        setState(ECClientStateDisconnected);
    }

    private MediaConstraints defaultPeerConnectionConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        return constraints;
    }

    private MediaConstraints defaultAnswerConstraints() {
        return defaultOfferConstraints();
    }

    private MediaConstraints defaultOfferConstraints() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        return constraints;
    }


    @Override
    public void signalingChannelDidOpenChannel(ECSignalingChannel signalingChannel) {
        this.signalingChannel = signalingChannel;
        Map iceServers = this.delegate.appClientRequestICEServers(this);
        setupICEServers(iceServers);
        setState(ECClientStateReady);
    }

    @Override
    public void didReceiveMessage(ECSignalingChannel channel, ECSignalingMessage message) {
        if (message == null) {
            return;
        }
        switch (message.type) {
            case kECSignalingMessageTypeOffer:
            case kECSignalingMessageTypeAnswer:
                hasReceivedSdp = true;
                messageQueue.add(0, message);
                break;
            case kECSignalingMessageTypeCandidate:
                messageQueue.add(message);
                break;
            case kECSignalingMessageTypeStarted:
            case kECSignalingMessageTypeReady:
            case kECSignalingMessageTypeBye:
                processSignalingMessage(message);
                return;
            default:
                Log.i(TAG, "didReceiveMessage message = " + message);
                break;
        }
        drainMessageQueueIfReady();
    }

    @Override
    public void readyToPublishStreamId(ECSignalingChannel signalingChannel, String streamId, String peerSocketId) {
        this.streamId = streamId;
        this.peerSocketId = peerSocketId;
        if (!TextUtils.isEmpty(peerSocketId)) {
            isInitiator = false;
            startPublishSignaling();
        } else {
            isInitiator = true;
            startPublishSignaling();
        }
    }

    @Override
    public void signalingChannelPublishFailed(ECSignalingChannel signalingChannel) {
    }

    @Override
    public void readyToSubscribeStreamId(ECSignalingChannel channel, String streamId, String peerSocketId) {
        isInitiator = false;
        this.streamId = streamId;
        this.peerSocketId = peerSocketId;
        startSubscribeSignaling();
    }

    private void setupICEServers(Map ICEServersConfiguration) {
        this.iceServers = new ArrayList<>();
        if (ICEServersConfiguration == null) {
            return;
        }
        for (Object obj : ICEServersConfiguration.values()) {
            if (obj instanceof Map) {
                Map dict = (Map) obj;
                String username = dict.get("username") != null ? dict.get("username").toString() : "";
                String password = dict.get("credential") != null ? dict.get("credential").toString() : "";
                String urls = dict.get("url") != null ? dict.get("url").toString() : "";
                if (TextUtils.isEmpty(urls)) {
                    Log.e(TAG, "No url found for ICEServer!");
                    continue;
                }
                PeerConnection.IceServer iceServer = new PeerConnection.IceServer(urls, username, password);
                this.iceServers.add(iceServer);
            }
        }
        Log.d(TAG, "Ice Servers: " + iceServers);
    }

    private void startPublishSignaling() {
        if (!TextUtils.isEmpty(peerSocketId)) {
            Log.i(TAG, "Start publish P2P signaling");
        } else {
            Log.i(TAG, "Start publish signaling");
        }
        setState(ECClientStateConnecting);
        Log.i(TAG, "Creating PeerConnection");
        MediaConstraints constraints = defaultPeerConnectionConstraints();
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(this.iceServers);
        peerConnection = factory.createPeerConnection(config, constraints, this);

        Log.i(TAG, "Adding local media stream to PeerConnection");

        localStream = this.delegate.streamToPublishByAppClient(this);
        peerConnection.addStream(localStream);
        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                didCreateSessionDescription(peerConnection, sessionDescription, null);
            }

            @Override
            public void onSetSuccess() {
            }

            @Override
            public void onCreateFailure(String errMsg) {
                didCreateSessionDescription(peerConnection, null, errMsg);
            }

            @Override
            public void onSetFailure(String s) {
            }
        }, defaultOfferConstraints());
    }

    private void startSubscribeSignaling() {
        Log.i(TAG, "Start subscribe signaling");
        setState(ECClientStateConnecting);

        Log.i(TAG, "Creating PeerConnection");
        MediaConstraints constraints = defaultPeerConnectionConstraints();
        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(this.iceServers);
        peerConnection = factory.createPeerConnection(config, constraints, this);
        if (!TextUtils.isEmpty(peerSocketId)) {// TODO 什么意思？
            drainMessageQueueIfReady();
        } else {
            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    didCreateSessionDescription(peerConnection, sessionDescription, null);
                }

                @Override
                public void onSetSuccess() {
                }

                @Override
                public void onCreateFailure(String errMsg) {
                    didCreateSessionDescription(peerConnection, null, errMsg);
                }

                @Override
                public void onSetFailure(String s) {
                }
            }, defaultOfferConstraints());
        }
    }

    private void drainMessageQueueIfReady() {
        if (peerConnection == null || !hasReceivedSdp) {
            return;
        }
        for (ECSignalingMessage message : messageQueue) {
            processSignalingMessage(message);
        }
        messageQueue.clear();
    }

    private void processSignalingMessage(ECSignalingMessage message) {
        Log.i(TAG, "processSignalingMessage message = " + message);
        switch (message.type) {
            case kECSignalingMessageTypeReady:
                break;
            case kECSignalingMessageTypeStarted:
                break;
            case kECSignalingMessageTypeOffer:
            case kECSignalingMessageTypeAnswer: {
                if (message instanceof ECSignalingMessage.ECSessionDescriptionMessage) {
                    ECSignalingMessage.ECSessionDescriptionMessage sdpMessage = (ECSignalingMessage.ECSessionDescriptionMessage) message;
                    SessionDescription description = sdpMessage.sessionDescription;
                    SessionDescription newSDP = SDPUtils.descriptionForDescription(description, getPreferredVideoCodec());
                    newSDP = this.descriptionForDescription(newSDP, clientOptions);
                    peerConnection.setRemoteDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                        }

                        @Override
                        public void onSetSuccess() {
                            didSetSessionDescriptionWithError(peerConnection, null);
                        }

                        @Override
                        public void onCreateFailure(String s) {
                        }

                        @Override
                        public void onSetFailure(String errMsg) {
                            didSetSessionDescriptionWithError(peerConnection, errMsg);
                        }
                    }, newSDP);
                }
                break;
            }
            case kECSignalingMessageTypeCandidate: {
                if (message instanceof ECSignalingMessage.ECICECandidateMessage) {
                    ECSignalingMessage.ECICECandidateMessage candidateMessage = (ECSignalingMessage.ECICECandidateMessage) message;
                    peerConnection.addIceCandidate(candidateMessage.iceCandidate);
                }
                break;
            }
            case kECSignalingMessageTypeBye:
                disconnect();
                break;
            default:
                Log.w(TAG, "Unhandled Message " + message);
                break;
        }
    }

    private void didSetSessionDescriptionWithError(final PeerConnection peerConnection, final String errMsg) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(errMsg)) {
                    Log.e(TAG, "Failed to set session description: " + errMsg);
                    delegate.didError(ECClient.this, kECAppClientErrorSetSDP, errMsg);
                    disconnect();
                    return;
                }
                // If we're answering and we've just set the remote offer we need to create
                // an answer and set the local description.
                if (!isInitiator && peerConnection.getLocalDescription() == null) {
                    final MediaConstraints constraints = defaultAnswerConstraints();
                    peerConnection.createAnswer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            didCreateSessionDescription(peerConnection, sessionDescription, null);
                        }

                        @Override
                        public void onSetSuccess() {

                        }

                        @Override
                        public void onCreateFailure(String errMsg) {
                            didCreateSessionDescription(peerConnection, null, errMsg);
                        }

                        @Override
                        public void onSetFailure(String s) {

                        }
                    }, constraints);
                }
            }
        });
    }

    private void didCreateSessionDescription(final PeerConnection peerConnection, final SessionDescription sdp, final String errMsg) {
        MainLooper.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(errMsg)) {
                    Log.e(TAG, "Failed to create session description. Error: " + errMsg);
                    disconnect();
                    delegate.didError(ECClient.this, kECAppClientErrorCreateSDP, "Failed to create session description.");
                    return;
                }
                Log.i(TAG, "did create a session description!");
                SessionDescription newSDP = SDPUtils.descriptionForDescription(sdp, getPreferredVideoCodec());
                newSDP = descriptionForDescription(newSDP, clientOptions);
                if (sdpHackCallback != null) {
                    newSDP = sdpHackCallback.sdpHackCallback(newSDP);
                }
                final SessionDescription fNewSDP = newSDP;
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                    }

                    @Override
                    public void onSetSuccess() {
                        didSetSessionDescriptionWithError(peerConnection, null);
                        ECSignalingMessage.ECSessionDescriptionMessage message = new ECSignalingMessage.ECSessionDescriptionMessage(fNewSDP, streamId, peerSocketId);
                        signalingChannel.sendSignalingMessage(message);
                        if (limitBitrate) {
                            setMaxBitrateForPeerConnectionVideoSender();
                        }
                    }

                    @Override
                    public void onCreateFailure(String s) {
                    }

                    @Override
                    public void onSetFailure(String errMsg) {
                        didSetSessionDescriptionWithError(peerConnection, errMsg);
                        delegate.didError(ECClient.this, kECAppClientErrorSetSDP, errMsg);// TODO 两次调用didError？
                    }
                }, newSDP);
            }
        });
    }

    private void setMaxBitrateForPeerConnectionVideoSender() {
        List<RtpSender> senders = peerConnection.getSenders();
        for (RtpSender sender : senders) {
            if (sender.track() != null) {
                if (TextUtils.equals(sender.track().kind(), MediaStreamTrack.VIDEO_TRACK_KIND)) {
                    setMaxBitrate(maxBitrate, sender);
                }
            }
        }
    }

    private void setMaxBitrate(long maxBitrate, RtpSender sender) {
        if (maxBitrate <= 0) {
            return;
        }
        RtpParameters parametersToModify = sender.getParameters();
        for (RtpParameters.Encoding encoding : parametersToModify.encodings) {
            encoding.maxBitrateBps = Math.toIntExact(maxBitrate * kKbpsMultiplier);
        }
        sender.setParameters(parametersToModify);
    }

    private SessionDescription descriptionForDescription(SessionDescription description, Map options) {
        SessionDescription newSDP = description;
        Object value = options.get(kClientOptionMaxVideoBW);
        if (value instanceof Long || value instanceof Integer) {
            long maxVideoBW = Long.valueOf(value.toString());
            newSDP = SDPUtils.descriptionForDescription(newSDP, maxVideoBW, "video");
        }

        value = options.get(kClientOptionMaxAudioBW);
        if (value instanceof Long || value instanceof Integer) {
            long maxAudioBW = Long.valueOf(value.toString());
            newSDP = SDPUtils.descriptionForDescription(newSDP, maxAudioBW, "audio");
        }
        return newSDP;
    }


    private String clientStateToString(ECClientState state) {
        if (state == null) {
            return "";
        }
        return state.toString();
    }

    private void setPreferredVideoCodec(String codec) {
        preferredVideoCodec = codec;
    }

    public String getPreferredVideoCodec() {
        if (!TextUtils.isEmpty(preferredVideoCodec)) {
            return preferredVideoCodec;
        } else {
            return defaultVideoCodec;
        }
    }

    public void hackSDPWithBlock(SDPHackCallback callback) {
        sdpHackCallback = callback;
    }

}
