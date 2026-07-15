package com.neoruaa.xhsdn;

public interface DownloadCallback {
    void onFileDownloaded(String filePath);
    void onDownloadProgress(String status);
    void onDownloadProgressUpdate(long downloaded, long total);
    void onDownloadError(String status, String originalUrl);
    void onVideoDetected();
    
    /**
     * Check if the download should be cancelled
     * @return true if cancelled
     */
    default boolean isCancelled() {
        return false;
    }
}