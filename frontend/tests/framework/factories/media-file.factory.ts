/**
 * Media File Factory
 *
 * Factory for creating various types of test media files
 * Supports video, audio, image, and document files
 *
 * Usage:
 * ```typescript
 * const image = MediaFileFactory.createImage({ format: 'jpg', width: 1920, height: 1080 })
 * const audio = MediaFileFactory.createAudio({ format: 'mp3', duration: 180 })
 * const document = MediaFileFactory.createDocument({ type: 'pdf', pages: 10 })
 * ```
 */

export interface BaseMediaConfig {
  name?: string
  format?: string
  size?: number
  metadata?: Record<string, any>
}

export interface ImageConfig extends BaseMediaConfig {
  format?: 'jpg' | 'jpeg' | 'png' | 'webp' | 'gif' | 'svg' | 'bmp'
  width?: number
  height?: number
  quality?: number // 1-100
  colorSpace?: 'RGB' | 'RGBA' | 'CMYK' | 'Grayscale'
  hasTransparency?: boolean
  dpi?: number
}

export interface AudioConfig extends BaseMediaConfig {
  format?: 'mp3' | 'wav' | 'flac' | 'aac' | 'ogg' | 'm4a'
  duration?: number // in seconds
  sampleRate?: 44100 | 48000 | 96000
  bitrate?: number // for compressed formats
  channels?: 1 | 2 | 5 | 7 // mono, stereo, 5.1, 7.1
  bitDepth?: 16 | 24 | 32
  hasMetadata?: boolean
}

export interface DocumentConfig extends BaseMediaConfig {
  type?: 'pdf' | 'doc' | 'docx' | 'txt' | 'rtf' | 'odt'
  pages?: number
  hasImages?: boolean
  hasTables?: boolean
  language?: string
}

export interface ArchiveConfig extends BaseMediaConfig {
  type?: 'zip' | 'rar' | '7z' | 'tar' | 'gz'
  compressionLevel?: 'store' | 'fast' | 'normal' | 'maximum'
  encrypted?: boolean
  files?: number
}

export interface MediaFileMetadata {
  name: string
  type: string
  format: string
  size: number
  mimeType: string
  createdAt: Date
  metadata: Record<string, any>
  // Image-specific
  width?: number
  height?: number
  // Audio-specific
  duration?: number
  sampleRate?: number
  channels?: number
  // Document-specific
  pages?: number
  // Archive-specific
  compressedSize?: number
  fileCount?: number
}

export class MediaFileFactory {
  /**
   * Create an image file
   */
  static createImage(config: ImageConfig = {}): MediaFileMetadata {
    const {
      name = 'test-image',
      format = 'jpg',
      width = 1920,
      height = 1080,
      quality = 85,
      colorSpace = 'RGB',
      hasTransparency = false,
      dpi = 72
    } = config

    const mimeType = this.getImageMimeType(format)
    const size = this.calculateImageSize(format, width, height, quality, colorSpace)

    return {
      name: `${name}.${format}`,
      type: 'image',
      format: format.toUpperCase(),
      size,
      mimeType,
      createdAt: new Date(),
      metadata: {
        quality,
        colorSpace,
        hasTransparency,
        dpi
      },
      width,
      height
    }
  }

  /**
   * Create an audio file
   */
  static createAudio(config: AudioConfig = {}): MediaFileMetadata {
    const {
      name = 'test-audio',
      format = 'mp3',
      duration = 30,
      sampleRate = 44100,
      bitrate = 128000,
      channels = 2,
      bitDepth = 16,
      hasMetadata = true
    } = config

    const mimeType = this.getAudioMimeType(format)
    const size = this.calculateAudioSize(format, duration, sampleRate, bitrate, channels, bitDepth)

    return {
      name: `${name}.${format}`,
      type: 'audio',
      format: format.toUpperCase(),
      size,
      mimeType,
      createdAt: new Date(),
      metadata: {
        duration,
        sampleRate,
        bitrate,
        channels,
        bitDepth,
        hasMetadata
      },
      duration,
      sampleRate,
      channels
    }
  }

  /**
   * Create a document file
   */
  static createDocument(config: DocumentConfig = {}): MediaFileMetadata {
    const {
      name = 'test-document',
      type = 'pdf',
      pages = 1,
      hasImages = false,
      hasTables = false,
      language = 'en'
    } = config

    const mimeType = this.getDocumentMimeType(type)
    const size = this.calculateDocumentSize(type, pages, hasImages, hasTables)

    return {
      name: `${name}.${type}`,
      type: 'document',
      format: type.toUpperCase(),
      size,
      mimeType,
      createdAt: new Date(),
      metadata: {
        pages,
        hasImages,
        hasTables,
        language
      },
      pages
    }
  }

  /**
   * Create an archive file
   */
  static createArchive(config: ArchiveConfig = {}): MediaFileMetadata {
    const {
      name = 'test-archive',
      type = 'zip',
      compressionLevel = 'normal',
      encrypted = false,
      files = 10
    } = config

    const mimeType = this.getArchiveMimeType(type)
    const size = this.calculateArchiveSize(type, files, compressionLevel)

    return {
      name: `${name}.${type}`,
      type: 'archive',
      format: type.toUpperCase(),
      size,
      mimeType,
      createdAt: new Date(),
      metadata: {
        compressionLevel,
        encrypted,
        fileCount: files
      },
      fileCount: files,
      compressedSize: Math.floor(size * 0.7) // Assume 30% compression
    }
  }

  /**
   * Create a large image for performance testing
   */
  static createLargeImage(width: number = 3840, height: number = 2160): MediaFileMetadata {
    return this.createImage({
      width,
      height,
      quality: 90,
      name: 'large-image'
    })
  }

  /**
   * Create a high-quality image
   */
  static createHighQualityImage(config: Partial<ImageConfig> = {}): MediaFileMetadata {
    return this.createImage({
      quality: 100,
      dpi: 300,
      colorSpace: 'RGB',
      ...config
    })
  }

  /**
   * Create a transparent PNG
   */
  static createTransparentImage(config: Partial<ImageConfig> = {}): MediaFileMetadata {
    return this.createImage({
      format: 'png',
      hasTransparency: true,
      colorSpace: 'RGBA',
      ...config
    })
  }

  /**
   * Create a lossless audio file
   */
  static createLosslessAudio(config: Partial<AudioConfig> = {}): MediaFileMetadata {
    return this.createAudio({
      format: 'flac',
      bitrate: undefined, // Lossless
      sampleRate: 96000,
      bitDepth: 24,
      ...config
    })
  }

  /**
   * Create a compressed audio file
   */
  static createCompressedAudio(config: Partial<AudioConfig> = {}): MediaFileMetadata {
    return this.createAudio({
      format: 'mp3',
      bitrate: 320000, // High quality MP3
      ...config
    })
  }

  /**
   * Create a multi-channel audio file
   */
  static createSurroundAudio(config: Partial<AudioConfig> = {}): MediaFileMetadata {
    return this.createAudio({
      channels: 5.1 as any,
      format: 'flac',
      ...config
    })
  }

  /**
   * Create a PDF document
   */
  static createPDF(pages: number = 1, config: Partial<DocumentConfig> = {}): MediaFileMetadata {
    return this.createDocument({
      type: 'pdf',
      pages,
      ...config
    })
  }

  /**
   * Create a Word document
   */
  static createWordDocument(config: Partial<DocumentConfig> = {}): MediaFileMetadata {
    return this.createDocument({
      type: 'docx',
      ...config
    })
  }

  /**
   * Create a text document
   */
  static createTextDocument(lines: number = 100): MediaFileMetadata {
    return this.createDocument({
      type: 'txt',
      pages: Math.ceil(lines / 50),
      metadata: { lineCount: lines }
    })
  }

  /**
   * Create a compressed archive
   */
  static createCompressedArchive(config: Partial<ArchiveConfig> = {}): MediaFileMetadata {
    return this.createArchive({
      compressionLevel: 'maximum',
      ...config
    })
  }

  /**
   * Create an encrypted archive
   */
  static createEncryptedArchive(config: Partial<ArchiveConfig> = {}): MediaFileMetadata {
    return this.createArchive({
      encrypted: true,
      ...config
    })
  }

  /**
   * Get MIME type for image format
   */
  private static getImageMimeType(format: string): string {
    const mimeTypes: Record<string, string> = {
      'jpg': 'image/jpeg',
      'jpeg': 'image/jpeg',
      'png': 'image/png',
      'webp': 'image/webp',
      'gif': 'image/gif',
      'svg': 'image/svg+xml',
      'bmp': 'image/bmp'
    }
    return mimeTypes[format] || 'image/jpeg'
  }

  /**
   * Get MIME type for audio format
   */
  private static getAudioMimeType(format: string): string {
    const mimeTypes: Record<string, string> = {
      'mp3': 'audio/mpeg',
      'wav': 'audio/wav',
      'flac': 'audio/flac',
      'aac': 'audio/aac',
      'ogg': 'audio/ogg',
      'm4a': 'audio/mp4'
    }
    return mimeTypes[format] || 'audio/mpeg'
  }

  /**
   * Get MIME type for document format
   */
  private static getDocumentMimeType(type: string): string {
    const mimeTypes: Record<string, string> = {
      'pdf': 'application/pdf',
      'doc': 'application/msword',
      'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'txt': 'text/plain',
      'rtf': 'application/rtf',
      'odt': 'application/vnd.oasis.opendocument.text'
    }
    return mimeTypes[type] || 'application/octet-stream'
  }

  /**
   * Get MIME type for archive format
   */
  private static getArchiveMimeType(type: string): string {
    const mimeTypes: Record<string, string> = {
      'zip': 'application/zip',
      'rar': 'application/vnd.rar',
      '7z': 'application/x-7z-compressed',
      'tar': 'application/x-tar',
      'gz': 'application/gzip'
    }
    return mimeTypes[type] || 'application/zip'
  }

  /**
   * Calculate image file size
   */
  private static calculateImageSize(
    format: string,
    width: number,
    height: number,
    quality: number,
    colorSpace: string
  ): number {
    const pixels = width * height
    let bytesPerPixel = 3 // RGB

    if (colorSpace === 'RGBA') bytesPerPixel = 4
    if (colorSpace === 'CMYK') bytesPerPixel = 4
    if (colorSpace === 'Grayscale') bytesPerPixel = 1

    // Compression ratio based on format and quality
    let compressionRatio = 0.1
    if (format === 'jpg' || format === 'jpeg') {
      compressionRatio = quality / 1000 // 0.1 - 1.0
    } else if (format === 'png') {
      compressionRatio = 0.5 // PNG typically compresses well
    } else if (format === 'webp') {
      compressionRatio = (quality / 100) * 0.3
    }

    return Math.floor(pixels * bytesPerPixel * compressionRatio)
  }

  /**
   * Calculate audio file size
   */
  private static calculateAudioSize(
    format: string,
    duration: number,
    sampleRate: number,
    bitrate: number | undefined,
    channels: number,
    bitDepth: number
  ): number {
    if (format === 'wav' || format === 'flac') {
      // Uncompressed or lossless
      return Math.floor((duration * sampleRate * channels * bitDepth) / 8)
    } else {
      // Compressed (MP3, AAC, OGG, etc.)
      const bps = bitrate || 128000
      return Math.floor((duration * bps) / 8)
    }
  }

  /**
   * Calculate document file size
   */
  private static calculateDocumentSize(
    type: string,
    pages: number,
    hasImages: boolean,
    hasTables: boolean
  ): number {
    const baseSizePerPage = 50000 // 50KB per page
    let size = pages * baseSizePerPage

    if (hasImages) size += pages * 100000 // Additional 100KB per page with images
    if (hasTables) size += pages * 30000 // Additional 30KB per page with tables

    // Format-specific adjustments
    if (type === 'pdf') size *= 1.2
    if (type === 'docx') size *= 0.8
    if (type === 'txt') size *= 0.1

    return Math.floor(size)
  }

  /**
   * Calculate archive file size
   */
  private static calculateArchiveSize(
    type: string,
    files: number,
    compressionLevel: string
  ): number {
    const avgFileSize = 1024 * 1024 // 1MB per file
    let baseSize = files * avgFileSize

    // Compression ratios
    const compressionRatios: Record<string, number> = {
      'store': 1.0,
      'fast': 0.7,
      'normal': 0.5,
      'maximum': 0.3
    }

    const ratio = compressionRatios[compressionLevel] || 0.5
    return Math.floor(baseSize * ratio)
  }
}
