/**
 * Video Factory
 *
 * Factory for creating test video files with various configurations
 * Supports multiple formats, resolutions, codecs, and quality profiles
 *
 * Usage:
 * ```typescript
 * const video = VideoFactory.create({
 *   format: 'mp4',
 *   resolution: '1080p',
 *   codec: 'H.264',
 *   duration: 60
 * })
 *
 * const streamingVideo = VideoFactory.createStreamingVideo({
 *   qualities: ['2160p', '1080p', '720p'],
 *   drm: true
 * })
 * ```
 */

export interface VideoConfig {
  format?: 'mp4' | 'webm' | 'ogg' | 'avi' | 'mov'
  resolution?: '2160p' | '1440p' | '1080p' | '720p' | '480p' | '360p' | '240p'
  codec?: 'H.264' | 'H.265' | 'VP8' | 'VP9' | 'AV1'
  duration?: number
  framerate?: 24 | 25 | 30 | 60 | 120
  bitrate?: number
  audioCodec?: 'AAC' | 'MP3' | 'Opus' | 'Vorbis'
  hasAudio?: boolean
  fileSize?: number
  drm?: boolean
  drmType?: 'Widevine' | 'PlayReady' | 'FairPlay'
}

export interface StreamingVideoConfig extends VideoConfig {
  qualities: string[]
  adaptiveBitrate?: boolean
  segmentDuration?: number
  manifestUrl?: string
  streamUrl?: string
}

export interface VideoMetadata {
  filename: string
  format: string
  codec: string
  resolution: { width: number; height: number }
  duration: number
  framerate: number
  bitrate: number
  fileSize: number
  hasAudio: boolean
  drm?: {
    enabled: boolean
    type?: string
  }
  qualities?: Array<{
    name: string
    resolution: { width: number; height: number }
    bitrate: number
  }>
}

export class VideoFactory {
  private static resolutionMap: Record<string, { width: number; height: number }> = {
    '2160p': { width: 3840, height: 2160 }, // 4K UHD
    '1440p': { width: 2560, height: 1440 }, // QHD
    '1080p': { width: 1920, height: 1080 }, // Full HD
    '720p': { width: 1280, height: 720 },   // HD
    '480p': { width: 854, height: 480 },    // SD
    '360p': { width: 640, height: 360 },    // Low
    '240p': { width: 426, height: 240 }     // Very Low
  }

  private static codecBitrateMap: Record<string, number> = {
    'H.264': 5000000, // 5 Mbps default
    'H.265': 3500000, // 3.5 Mbps (more efficient)
    'VP8': 4500000,   // 4.5 Mbps
    'VP9': 3000000,   // 3 Mbps
    'AV1': 2500000    // 2.5 Mbps (most efficient)
  }

  /**
   * Create a standard video file
   */
  static create(config: VideoConfig = {}): VideoMetadata {
    const {
      format = 'mp4',
      resolution = '1080p',
      codec = 'H.264',
      duration = 30,
      framerate = 30,
      bitrate,
      audioCodec = 'AAC',
      hasAudio = true,
      drm = false,
      drmType
    } = config

    const resolutionInfo = this.resolutionMap[resolution] || this.resolutionMap['1080p']
    const codecBitrate = bitrate || this.codecBitrateMap[codec] || 5000000
    const videoBitrate = hasAudio ? codecBitrate * 0.9 : codecBitrate // 90% video, 10% audio
    const audioBitrate = hasAudio ? codecBitrate * 0.1 : 0

    // Calculate file size
    const fileSize = Math.floor(((videoBitrate + audioBitrate) * duration) / 8)

    return {
      filename: `test-video-${resolution}.${format}`,
      format: format.toUpperCase(),
      codec,
      resolution: resolutionInfo,
      duration,
      framerate,
      bitrate: codecBitrate,
      fileSize,
      hasAudio,
      drm: drm ? { enabled: true, type: drmType } : undefined
    }
  }

  /**
   * Create a high-quality video (4K)
   */
  static create4K(config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      resolution: '2160p',
      codec: 'H.265',
      bitrate: 15000000,
      ...config
    })
  }

  /**
   * Create a full HD video (1080p)
   */
  static create1080p(config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      resolution: '1080p',
      codec: 'H.264',
      bitrate: 5000000,
      ...config
    })
  }

  /**
   * Create an HD video (720p)
   */
  static create720p(config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      resolution: '720p',
      codec: 'H.264',
      bitrate: 3000000,
      ...config
    })
  }

  /**
   * Create an SD video (480p)
   */
  static create480p(config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      resolution: '480p',
      codec: 'H.264',
      bitrate: 1000000,
      ...config
    })
  }

  /**
   * Create a streaming video with multiple qualities
   */
  static createStreamingVideo(config: StreamingVideoConfig): VideoMetadata & {
    qualities: Array<{
      name: string
      resolution: { width: number; height: number }
      bitrate: number
    }>
    adaptiveBitrate: boolean
    segmentDuration: number
  } {
    const baseConfig: VideoConfig = {
      format: config.format || 'mp4',
      codec: config.codec || 'H.264',
      duration: config.duration || 60,
      framerate: config.framerate || 30,
      drm: config.drm,
      drmType: config.drmType
    }

    const qualities = config.qualities.map(quality => {
      const resolutionInfo = this.resolutionMap[quality] || this.resolutionMap['1080p']
      const bitrate = this.getQualityBitrate(quality)
      return {
        name: quality,
        resolution: resolutionInfo,
        bitrate
      }
    })

    return {
      ...this.create(baseConfig),
      qualities,
      adaptiveBitrate: config.adaptiveBitrate !== false,
      segmentDuration: config.segmentDuration || 6
    }
  }

  /**
   * Create a DRM-protected video
   */
  static createDRMVideo(
    drmType: 'Widevine' | 'PlayReady' | 'FairPlay' = 'Widevine',
    config: Partial<VideoConfig> = {}
  ): VideoMetadata {
    return this.create({
      drm: true,
      drmType,
      ...config
    })
  }

  /**
   * Create a video with specific codec
   */
  static createWithCodec(codec: VideoConfig['codec'], config: Partial<VideoConfig> = {}): VideoMetadata {
    const bitrate = this.codecBitrateMap[codec] || 5000000
    return this.create({
      codec,
      bitrate,
      ...config
    })
  }

  /**
   * Create a short video (for testing)
   */
  static createShort(duration: number = 5, config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      duration,
      ...config
    })
  }

  /**
   * Create a long video (for testing)
   */
  static createLong(duration: number = 300, config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      duration,
      ...config
    })
  }

  /**
   * Create a silent video (no audio)
   */
  static createSilent(config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      hasAudio: false,
      ...config
    })
  }

  /**
   * Create a video with specific framerate
   */
  static createHighFramerate(framerate: 60 | 120 = 60, config: Partial<VideoConfig> = {}): VideoMetadata {
    return this.create({
      framerate,
      bitrate: 10000000, // Higher bitrate for high framerate
      ...config
    })
  }

  /**
   * Get bitrate for quality level
   */
  private static getQualityBitrate(quality: string): number {
    const bitrateMap: Record<string, number> = {
      '2160p': 15000000,
      '1440p': 8000000,
      '1080p': 5000000,
      '720p': 3000000,
      '480p': 1000000,
      '360p': 500000,
      '240p': 250000
    }
    return bitrateMap[quality] || 1000000
  }

  /**
   * Get all available resolutions
   */
  static getAvailableResolutions(): string[] {
    return Object.keys(this.resolutionMap)
  }

  /**
   * Get resolution dimensions
   */
  static getResolution(resolution: string): { width: number; height: number } | null {
    return this.resolutionMap[resolution] || null
  }

  /**
   * Check if resolution is valid
   */
  static isValidResolution(resolution: string): boolean {
    return resolution in this.resolutionMap
  }

  /**
   * Compare two videos
   */
  static compare(video1: VideoMetadata, video2: VideoMetadata): {
    sameFormat: boolean
    sameCodec: boolean
    sameResolution: boolean
    sameDuration: boolean
    qualityDifference: number
  } {
    return {
      sameFormat: video1.format === video2.format,
      sameCodec: video1.codec === video2.codec,
      sameResolution: video1.resolution.width === video2.resolution.width &&
                     video1.resolution.height === video2.resolution.height,
      sameDuration: video1.duration === video2.duration,
      qualityDifference: Math.abs(video1.bitrate - video2.bitrate)
    }
  }

  /**
   * Get file extension from format
   */
  static getFileExtension(format: string): string {
    const extensionMap: Record<string, string> = {
      'mp4': 'mp4',
      'webm': 'webm',
      'ogg': 'ogv',
      'avi': 'avi',
      'mov': 'mov'
    }
    return extensionMap[format] || format
  }

  /**
   * Estimate playback bandwidth requirement
   */
  static estimateBandwidth(video: VideoMetadata): number {
    // Return bitrate in bits per second
    return video.bitrate
  }

  /**
   * Get quality level from resolution
   */
  static getQualityFromResolution(width: number, height: number): string | null {
    for (const [quality, resolution] of Object.entries(this.resolutionMap)) {
      if (resolution.width === width && resolution.height === height) {
        return quality
      }
    }
    return null
  }
}
