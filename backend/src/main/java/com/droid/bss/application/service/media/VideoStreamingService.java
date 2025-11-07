/**
 * Video Streaming Service
 *
 * Manages video streams with adaptive bitrate streaming (ABR)
 * Supports multiple quality levels and DRM
 */

package com.droid.bss.application.service.media;

import com.droid.bss.domain.media.VideoStream;
import com.droid.bss.domain.media.VideoStream.StreamQuality;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoStreamingService {

    /**
     * Create video stream for uploaded file
     */
    public VideoStream createVideoStream(
            String fileId,
            String tenantId,
            List<StreamQuality> qualities
    ) {
        VideoStream stream = new VideoStream();
        stream.setTenantId(tenantId);
        stream.setFileId(fileId);
        stream.setStreamUrl(generateStreamUrl(fileId));
        stream.setManifestUrl(generateManifestUrl(fileId));
        stream.setStatus(VideoStream.StreamStatus.PROCESSING);
        stream.setQualities(qualities);
        stream.setDefaultQuality("auto");
        stream.setAdaptiveBitrate(true);
        stream.setCodec("H.264");
        stream.setContainer("MP4");
        stream.setDuration(calculateDuration(qualities));

        // Set default qualities if not provided
        if (qualities == null || qualities.isEmpty()) {
            stream.setQualities(generateDefaultQualities(stream));
        }

        return stream;
    }

    /**
     * Get available qualities for stream
     */
    public List<StreamQuality> getAvailableQualities(VideoStream stream, String connectionSpeed) {
        List<StreamQuality> available = new ArrayList<>();

        // Determine connection speed
        int speedMbps = parseConnectionSpeed(connectionSpeed);

        for (StreamQuality quality : stream.getQualities()) {
            if (quality.isAvailable()) {
                // Add quality if suitable for connection speed
                long qualityMbps = quality.getBitrate() / 1000000;
                if (speedMbps == 0 || qualityMbps <= speedMbps * 0.8) {
                    available.add(quality);
                }
            }
        }

        // Sort by resolution (highest first)
        available.sort((q1, q2) -> q2.getResolutionWidth().compareTo(q1.getResolutionWidth()));

        return available;
    }

    /**
     * Select optimal quality for connection
     */
    public StreamQuality selectOptimalQuality(
            VideoStream stream,
            String connectionSpeed,
            String userAgent,
            String bandwidth
    ) {
        List<StreamQuality> qualities = getAvailableQualities(stream, connectionSpeed);

        if (qualities.isEmpty()) {
            return null;
        }

        // Auto mode - select based on bandwidth
        long bandwidthBytes = parseBandwidth(bandwidth);

        // Simulate bandwidth detection
        int detectedMbps = bandwidthBytes > 0 ? (int) (bandwidthBytes / 125000) : 5;

        // Find best quality for bandwidth
        for (StreamQuality quality : qualities) {
            long qualityMbps = quality.getBitrate() / 1000000;
            if (qualityMbps <= detectedMbps) {
                quality.incrementViewCount();
                return quality;
            }
        }

        // Return lowest quality if none match
        StreamQuality lowest = qualities.get(qualities.size() - 1);
        lowest.incrementViewCount();
        return lowest;
    }

    /**
     * Get quality by name
     */
    public StreamQuality getQualityByName(VideoStream stream, String qualityName) {
        return stream.getQualities().stream()
            .filter(q -> q.getQualityName().equalsIgnoreCase(qualityName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Switch quality during playback
     */
    public StreamSwitchResult switchQuality(
            VideoStream stream,
            String fromQuality,
            String toQuality,
            String sessionId
    ) {
        StreamQuality fromQ = getQualityByName(stream, fromQuality);
        StreamQuality toQ = getQualityByName(stream, toQuality);

        if (fromQ == null || toQ == null) {
            return new StreamSwitchResult(false, "Quality not found", null);
        }

        if (!toQ.isAvailable()) {
            return new StreamSwitchResult(false, "Quality not available", fromQ);
        }

        // Simulate quality switch
        long switchTime = 200; // milliseconds

        // Update metrics
        toQ.incrementViewCount();
        stream.addBandwidthUsage(toQ.getBitrate() / 8); // Convert to bytes

        return new StreamSwitchResult(true, "Quality switched successfully", toQ, switchTime);
    }

    /**
     * Get streaming statistics
     */
    public StreamingStatistics getStatistics(VideoStream stream) {
        StreamingStatistics stats = new StreamingStatistics();
        stats.setStreamId(stream.getId());
        stats.setTotalViews(stream.getTotalViews());
        stats.setActiveViewers(stream.getActiveViewers());
        stats.setBandwidthUsage(stream.getBandwidthUsage());
        stats.setErrorCount(stream.getErrorCount());

        // Calculate average quality
        double avgBitrate = stream.getQualities().stream()
            .mapToLong(StreamQuality::getBitrate)
            .average()
            .orElse(0);
        stats.setAverageBitrate((long) avgBitrate);

        // Quality distribution
        Map<String, Long> qualityDistribution = new HashMap<>();
        for (StreamQuality quality : stream.getQualities()) {
            qualityDistribution.put(quality.getQualityName(), quality.getViewCount());
        }
        stats.setQualityDistribution(qualityDistribution);

        return stats;
    }

    /**
     * Enable DRM for stream
     */
    public VideoStream enableDRM(
            VideoStream stream,
            String drmType,
            String licenseUrl
    ) {
        stream.setDrmEnabled(true);
        stream.setDrmType(drmType);
        stream.setDrmLicenseUrl(licenseUrl);
        return stream;
    }

    /**
     * Disable DRM for stream
     */
    public VideoStream disableDRM(VideoStream stream) {
        stream.setDrmEnabled(false);
        stream.setDrmType(null);
        stream.setDrmLicenseUrl(null);
        return stream;
    }

    /**
     * Get DRM information
     */
    public Map<String, String> getDRMInfo(VideoStream stream) {
        if (!stream.isDRMEnabled()) {
            return Collections.emptyMap();
        }

        Map<String, String> drmInfo = new HashMap<>();
        drmInfo.put("type", stream.getDrmType());
        drmInfo.put("licenseUrl", stream.getDrmLicenseUrl());
        drmInfo.put("drmEnabled", "true");

        return drmInfo;
    }

    /**
     * Generate default quality levels
     */
    private List<StreamQuality> generateDefaultQualities(VideoStream stream) {
        List<StreamQuality> qualities = new ArrayList<>();

        // 2160p (4K)
        qualities.add(createQuality(stream, "2160p", 3840, 2160, 15000000L));

        // 1080p (Full HD)
        qualities.add(createQuality(stream, "1080p", 1920, 1080, 5000000L));

        // 720p (HD)
        qualities.add(createQuality(stream, "720p", 1280, 720, 3000000L));

        // 480p (SD)
        qualities.add(createQuality(stream, "480p", 854, 480, 1000000L));

        // 360p (Low)
        qualities.add(createQuality(stream, "360p", 640, 360, 500000L));

        return qualities;
    }

    /**
     * Create a stream quality
     */
    private StreamQuality createQuality(
            VideoStream stream,
            String name,
            int width,
            int height,
            long bitrate
    ) {
        StreamQuality quality = new StreamQuality();
        quality.setStream(stream);
        quality.setQualityName(name);
        quality.setResolutionWidth(width);
        quality.setResolutionHeight(height);
        quality.setBitrate(bitrate);
        quality.setFramerate(30);
        quality.setCodec("H.264");
        quality.setSegmentDuration(6);
        quality.setStatus(StreamQuality.QualityStatus.AVAILABLE);
        quality.setViewCount(0L);

        // Calculate file size (bitrate * duration / 8)
        int duration = stream.getDuration() != null ? stream.getDuration() : 300; // Default 5 minutes
        quality.setFileSize((bitrate * duration) / 8);
        quality.setSegmentCount(duration / 6);

        return quality;
    }

    /**
     * Calculate duration from qualities
     */
    private Integer calculateDuration(List<StreamQuality> qualities) {
        if (qualities == null || qualities.isEmpty()) {
            return 300; // Default 5 minutes
        }
        return qualities.get(0).getSegmentCount() * qualities.get(0).getSegmentDuration();
    }

    /**
     * Generate stream URL
     */
    private String generateStreamUrl(String fileId) {
        return "https://streaming.example.com/streams/" + fileId;
    }

    /**
     * Generate manifest URL
     */
    private String generateManifestUrl(String fileId) {
        return "https://streaming.example.com/manifests/" + fileId + ".m3u8";
    }

    /**
     * Parse connection speed
     */
    private int parseConnectionSpeed(String connectionSpeed) {
        if (connectionSpeed == null || connectionSpeed.isEmpty()) {
            return 0; // Unknown
        }

        String lower = connectionSpeed.toLowerCase();
        if (lower.contains("wifi") || lower.contains("ethernet")) {
            return 100; // Assume fast connection
        } else if (lower.contains("4g") || lower.contains("lte")) {
            return 25;
        } else if (lower.contains("3g")) {
            return 5;
        } else if (lower.contains("2g")) {
            return 1;
        }

        return 0;
    }

    /**
     * Parse bandwidth
     */
    private long parseBandwidth(String bandwidth) {
        if (bandwidth == null || bandwidth.isEmpty()) {
            return 0;
        }

        try {
            String lower = bandwidth.toLowerCase();
            double value = Double.parseDouble(lower.replaceAll("[^0-9.]", ""));

            if (lower.contains("mbps") || lower.contains("mb/s")) {
                return (long) (value * 1000000 / 8);
            } else if (lower.contains("kbps") || lower.contains("kb/s")) {
                return (long) (value * 1000 / 8);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }

        return 0;
    }

    // Result classes

    public static class StreamSwitchResult {
        private boolean success;
        private String message;
        private StreamQuality newQuality;
        private long switchTimeMs;

        public StreamSwitchResult(boolean success, String message, StreamQuality newQuality) {
            this.success = success;
            this.message = message;
            this.newQuality = newQuality;
        }

        public StreamSwitchResult(boolean success, String message, StreamQuality newQuality, long switchTimeMs) {
            this.success = success;
            this.message = message;
            this.newQuality = newQuality;
            this.switchTimeMs = switchTimeMs;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public StreamQuality getNewQuality() { return newQuality; }
        public long getSwitchTimeMs() { return switchTimeMs; }
    }

    public static class StreamingStatistics {
        private String streamId;
        private int totalViews;
        private int activeViewers;
        private long bandwidthUsage;
        private int errorCount;
        private long averageBitrate;
        private Map<String, Long> qualityDistribution;

        // Getters and setters
        public String getStreamId() { return streamId; }
        public void setStreamId(String streamId) { this.streamId = streamId; }

        public int getTotalViews() { return totalViews; }
        public void setTotalViews(int totalViews) { this.totalViews = totalViews; }

        public int getActiveViewers() { return activeViewers; }
        public void setActiveViewers(int activeViewers) { this.activeViewers = activeViewers; }

        public long getBandwidthUsage() { return bandwidthUsage; }
        public void setBandwidthUsage(long bandwidthUsage) { this.bandwidthUsage = bandwidthUsage; }

        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

        public long getAverageBitrate() { return averageBitrate; }
        public void setAverageBitrate(long averageBitrate) { this.averageBitrate = averageBitrate; }

        public Map<String, Long> getQualityDistribution() { return qualityDistribution; }
        public void setQualityDistribution(Map<String, Long> qualityDistribution) { this.qualityDistribution = qualityDistribution; }
    }
}
