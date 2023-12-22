package miju.rpg.ugmt;

/**
 * Simple Mime type implementation.
 */
public enum MimeType {
    HTML(".html", "text/html"),
    TXT(".txt", "text/text"),
    XML(".xml", "text/xml"),
    XSL(".xsl", "application/xml"),
    JS(".js", "text/javascript"),
    CSS(".css", "text/css"),
    PNG(".png", "image/png"),
    ICO(".ico", "image/png"),
    JPG(".jpg", "image/jpeg"),
    GIF(".gif", "image/gif"),
    PDF(".pdf", "application/pdf"),
    WAV(".wav", "audio/x-wav"),
    OGG(".ogg", "audio/ogg"),
    SVG(".svg", "image/svg+xml");

    /** File extension. */
    private final String extension;

    /** HTTP Mime Type. */
    private final String mimeType;

    /**
     * Constructor.
     * @param anExtension file extension
     * @param aMimeType mime type
     */
    MimeType(final String anExtension, final String aMimeType) {
        this.extension = anExtension;
        this.mimeType = aMimeType;
    }

    /** Getter. @return extension */
    public String getExtension() {
        return extension;
    }

    /** Getter. @return mime-type */
    public String getMimeType() {
        return mimeType;
    }
}
