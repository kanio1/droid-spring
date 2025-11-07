/**
 * Video Stream REST Controller
 *
 * Provides REST endpoints for video streaming operations:
 * - Create video stream
 * - Get stream details
 * - List available qualities
 * - Switch quality
 * - Get streaming statistics
 */

package com.droid.bss.api.media;

import com.droid.bss.application.service.media.VideoStreamingService;
import com.droid.bss.domain.media.VideoStream;
import com.droid.bss.domain.media.VideoStream.StreamQuality;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video-streams")
@Tag(name = "Video Stream", description = "Video streaming management API")
public class VideoStreamController {

    private final VideoStreamingService videoStreamingService;

    public VideoStreamController(VideoStreamingService videoStreamingService) {
        this.videoStreamingService = videoStreamingService;
    }

    @PostMapping
    @Operation(summary = "Create video stream", description = "Create a new video stream for uploaded file")
    public ResponseEntity<VideoStream> createStream(
            @RequestParam String fileId,
            @RequestParam String tenantId,
            @RequestBody(required = false) List<StreamQuality> qualities
    ) {
        VideoStream stream = videoStreamingService.createVideoStream(fileId, tenantId, qualities);
        return ResponseEntity.ok(stream);
    }

    @GetMapping("/{streamId}")
    @Operation(summary = "Get stream details", description = "Retrieve video stream details")
    public ResponseEntity<VideoStream> getStream(@PathVariable String streamId) {
        // In real implementation, would fetch from repository
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{streamId}/qualities")
    @Operation(summary = "List available qualities", description = "Get available quality levels for stream")
    public ResponseEntity<List<StreamQuality>> getQualities(
            @PathVariable String streamId,
            @RequestParam(required = false) String connectionSpeed
    ) {
        // Mock implementation - would fetch from repository
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{streamId}/switch-quality")
    @Operation(summary = "Switch quality", description = "Switch video quality during playback")
    public ResponseEntity<VideoStreamingService.StreamSwitchResult> switchQuality(
            @PathVariable String streamId,
            @RequestParam String fromQuality,
            @RequestParam String toQuality,
            @RequestParam String sessionId
    ) {
        // Mock implementation - would fetch stream and switch
        VideoStreamingService.StreamSwitchResult result =
            new VideoStreamingService.StreamSwitchResult(true, "Quality switched successfully", null);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{streamId}/statistics")
    @Operation(summary = "Get streaming statistics", description = "Retrieve streaming metrics and statistics")
    public ResponseEntity<VideoStreamingService.StreamingStatistics> getStatistics(@PathVariable String streamId) {
        // Mock implementation - would fetch from repository
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{streamId}/enable-drm")
    @Operation(summary = "Enable DRM", description = "Enable DRM protection for stream")
    public ResponseEntity<VideoStream> enableDRM(
            @PathVariable String streamId,
            @RequestParam String drmType,
            @RequestParam String licenseUrl
    ) {
        // Mock implementation - would fetch stream, enable DRM, and save
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{streamId}/disable-drm")
    @Operation(summary = "Disable DRM", description = "Disable DRM protection for stream")
    public ResponseEntity<VideoStream> disableDRM(@PathVariable String streamId) {
        // Mock implementation - would fetch stream, disable DRM, and save
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{streamId}/drm-info")
    @Operation(summary = "Get DRM information", description = "Retrieve DRM configuration for stream")
    public ResponseEntity<Map<String, String>> getDRMInfo(@PathVariable String streamId) {
        // Mock implementation - would fetch from repository
        return ResponseEntity.ok(Map.of());
    }
}
