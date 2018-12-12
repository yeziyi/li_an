package com.tencent.gamejoy.licode_androidclient.core.rtc;

import android.text.TextUtils;
import android.util.Log;

import org.webrtc.SessionDescription;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class SDPUtils {

    private static final String TAG = "SDPUtils";

    public static SessionDescription descriptionForDescription(SessionDescription description, long bandwidthLimit, String mediaType) {
        String mediaPattern = "m=" + mediaType + ".*";
        Pattern p = Pattern.compile(mediaPattern, CASE_INSENSITIVE);
        StringBuilder newSDP = new StringBuilder();
        boolean mediaFound = false;
        String sdp = description.description;
        BufferedReader reader = new BufferedReader(new StringReader(sdp));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.find()) {
                    mediaFound = true;
                } else if (mediaFound) {
                    String lineStart = line.substring(0, 2);
                    if (!TextUtils.equals(lineStart, "i=") && !TextUtils.equals(lineStart, "c=") && !TextUtils.equals(lineStart, "b=")) {
                        String newLine = "b=AS:" + bandwidthLimit + "\r\n";
                        newSDP.append(newLine);
                        mediaFound = false;
                        Log.d(TAG, "SDP BW Updated: " + newLine);
                    }
                }
                newSDP.append(line).append("\r\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "descriptionForDescription " + e);
        }
        return new SessionDescription(description.type, newSDP.toString());
    }

    public static SessionDescription descriptionForDescription(SessionDescription description, String codec) {
        if (description == null) {
            return null;
        }
        String sdpString = description.description;
        String lineSeparator = "\n";
        String mLineSeparator = " ";
        String[] lines = sdpString.split(lineSeparator);
        // Find the line starting with "m=video".
        int mLineIndex = -1;
        for (int i = 0; i < lines.length; ++i) {
            if (lines[i].startsWith("m=video")) {
                mLineIndex = i;
                break;
            }
        }
        if (mLineIndex == -1) {
            Log.w(TAG, "No m=video line, so can't prefer " + codec);
            return description;
        }
        // An array with all payload types with name |codec|. The payload types are
        // integers in the range 96-127, but they are stored as strings here.
        List<String> codecPayloadTypes = new ArrayList<>();
        // a=rtpmap:<payload type> <encoding name>/<clock rate>
        // [/<encoding parameters>]
        String pattern = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern p = Pattern.compile(pattern);
        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                String group = m.group().replaceFirst("a=rtpmap:", "");
                int index = group.indexOf(codec);
                group = group.substring(0, index).trim();
                codecPayloadTypes.add(group);
            }
        }
        if (codecPayloadTypes.size() <= 0) {
            Log.w(TAG, "No payload types with name " + codec);
            return description;
        }
        String[] origMLineParts = lines[mLineIndex].split(mLineSeparator);
        // The format of ML should be: m=<media> <port> <proto> <fmt> ...
        final int kHeaderLength = 3;
        if (origMLineParts.length <= kHeaderLength) {
            Log.w(TAG, "Wrong SDP media description format: " + lines[mLineIndex]);
            return description;
        }
        // Split the line into header and payloadTypes.
        int headerRangeStart = 0;
        int headerRangeEnd = kHeaderLength;
        int payloadRangeStart = kHeaderLength;
        int payloadRangeEnd = origMLineParts.length;
        String[] header = Arrays.copyOfRange(origMLineParts, headerRangeStart, headerRangeEnd);
        String[] payloadTypes = Arrays.copyOfRange(origMLineParts, payloadRangeStart, payloadRangeEnd);
        // Reconstruct the line with |codecPayloadTypes| moved to the beginning of the
        // payload types.
        List<String> newMLineParts = new ArrayList<>();
        newMLineParts.addAll(Arrays.asList(header));
        newMLineParts.addAll(codecPayloadTypes);
        for (String payload : payloadTypes) {
            if (!codecPayloadTypes.contains(payload)) {
                newMLineParts.add(payload);
            }
        }
        StringBuffer buffer = new StringBuffer();
        for (String str : newMLineParts) {
            buffer.append(str).append(mLineSeparator);
        }
        String newMLine = buffer.toString().trim();
        lines[mLineIndex] = newMLine;
        buffer = new StringBuffer();
        for (String line : lines) {
            buffer.append(line).append(lineSeparator);
        }
        String mangledSdpString = buffer.toString().trim();
        return new SessionDescription(description.type, mangledSdpString);
    }
}
