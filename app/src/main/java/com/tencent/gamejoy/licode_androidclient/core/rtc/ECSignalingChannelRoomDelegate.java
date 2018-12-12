package com.tencent.gamejoy.licode_androidclient.core.rtc;

import java.util.Date;
import java.util.Map;

public interface ECSignalingChannelRoomDelegate {

    /**
     * This event is fired when a token was not successfuly used.
     *
     * @param channel ECSignalingChannel the channel that emit the message.
     * @param reason  String of error returned by the server.
     */
    public void didError(ECSignalingChannel channel, String reason);

    /**
     * Event fired as soon a client connect to a room.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param roomMeta Metadata associated to the room that the client just connect.
     */
    public void didConnectToRoom(ECSignalingChannel channel, Map roomMeta);

    /**
     * Event fired as soon as rtc channels were disconnected and websocket
     * connection is about to be closed.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param roomMeta Metadata associated to the room that the client just connect.
     */
    public void didDisconnectOfRoom(ECSignalingChannel channel, Map roomMeta);

    /**
     * Event fired when a new stream id has been created and server is ready
     * to start publishing it.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param streamId NSString id of the stream that will be published.
     */
    public void didReceiveStreamIdReadyToPublish(ECSignalingChannel channel, String streamId);

    /**
     * Event fired when a recording of a stream has started.
     *
     * @param channel       ECSignalingChannel the channel that emit the message.
     * @param streamId      NSString id of the stream being recorded.
     * @param recordingId   NSString id of the recording id on Erizo server.
     * @param recordingDate NSDate when the server start to recording the stream.
     */
    public void didStartRecordingStreamId(ECSignalingChannel channel, String streamId, String recordingId, Date recordingDate);

    /**
     * Event fired when a recording of a stream has failed.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param streamId NSString id of the stream being recorded.
     * @param errorMsg Error string sent from the server.
     */
    public void didFailStartRecordingStreamId(ECSignalingChannel channel, String streamId, String errorMsg);

    /**
     * Event fired when a new StreamId has been added to a room.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param streamId NSString added to the room.
     * @param event    Event name and data carried
     */
    public void didStreamAddedWithId(ECSignalingChannel channel, String streamId, ECSignalingEvent event);

    /**
     * Event fired when a StreamId has been removed from a room, not necessary this
     * stream has been consumed.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param streamId NSString of the removed stream.
     */
    public void didRemovedStreamId(ECSignalingChannel channel, String streamId);

    /**
     * Event fired when a StreamId previously subscribed has been unsubscribed.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param streamId NSString of the unsubscribed stream.
     */
    public void didUnsubscribeStreamWithId(ECSignalingChannel channel, String streamId);

    /**
     * Event fired when a published stream is being unpublished.
     *
     * @param channel  ECSignalingChannel the channel that emit the message.
     * @param streamId NSString of the stream being unpublished
     */
    public void didUnpublishStreamWithId(ECSignalingChannel channel, String streamId);

    /**
     * Event fired when some peer request to subscribe to a given stream.
     *
     * @param channel      ECSignalingChannel the channel that emit the message.
     * @param streamId     NSString of the unsubscribed stream.
     * @param peerSocketId String that identifies the peer connection for the stream.
     */
    public void didRequestPublishP2PStreamWithId(ECSignalingChannel channel, String streamId, String peerSocketId);

    /**
     * Method called when the signaling channels needs a new client to operate a connection.
     *
     * @param channel ECSignalingChannel the channel that emit the message.
     * @returns ECClientDelegate instance.
     */
    public ECSignalingChannelDelegate clientDelegateRequiredForSignalingChannel(ECSignalingChannel channel);

    /**
     * Event fired when data stream received.
     *
     * @param channel    ECSignalingChannel the channel that emit the message.
     * @param streamId   NSString id of the stream received from.
     * @param dataStream NSDictionary having message and timestamp.
     */
    public void receivedDataStream(ECSignalingChannel channel, String streamId, Map dataStream);

    /**
     * Event fired when stream atrribute updated.
     *
     * @param channel    ECSignalingChannel the channel that emit the message.
     * @param streamId   NSString id of the stream received from.
     * @param attributes NSDictionary having custom attribute.
     */
    public void updateStreamAttributes(ECSignalingChannel channel, String streamId, Map attributes);



}
