package com.tencent.gamejoy.licode_androidclient.core.rtc;

public interface ECSignalingChannelDelegate {

    /**
     * Event fired when Erizo server has validated our token.
     *
     * @param signalingChannel ECSignalingChannel the channel that emit the message.
     */
    public void signalingChannelDidOpenChannel(ECSignalingChannel signalingChannel);

    /**
     * Event fired each time ECSignalingChannel has received a new ECSignalingMessage.
     *
     * @param channel ECSignalingChannel the channel that emit the message.
     * @param message ECSignalingMessage received by channel.
     */
    public void didReceiveMessage(ECSignalingChannel channel, ECSignalingMessage message);

    /**
     * Event fired when Erizo is ready to receive a publishing stream.
     *
     * @param signalingChannel ECSignalingChannel the channel that emit the message.
     * @param peerSocketId     Id of the socket in a p2p publishing without MCU. Pass nil if
     *                         you are not setting a P2P room.
     */
    public void readyToPublishStreamId(ECSignalingChannel signalingChannel, String streamId, String peerSocketId);

    /**
     * Event fired when Erizo failed to publishing stream.
     *
     * @param signalingChannel ECSignalingChannel the channel that emit the message.
     */
    public void signalingChannelPublishFailed(ECSignalingChannel signalingChannel);

    /**
     * Event fired each time ECSignalingChannel has received a confirmation from the server
     * to subscribe a stream.
     * This event is fired to let Client know that it can start signaling to subscribe the stream.
     *
     * @param channel      ECSignalingChannel the channel that emit the message.
     * @param streamId     Id of the stream that will be subscribed.
     * @param peerSocketId pass nil if is MCU being used.
     */
    public void readyToSubscribeStreamId(ECSignalingChannel channel, String streamId, String peerSocketId);

}