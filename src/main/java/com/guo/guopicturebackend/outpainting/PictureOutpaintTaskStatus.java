package com.guo.guopicturebackend.outpainting;

public final class PictureOutpaintTaskStatus {

    private PictureOutpaintTaskStatus() {
    }

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    /** 本地超时后向云端确认中 */
    public static final String RECONCILING = "RECONCILING";

    public static boolean isTerminal(String status) {
        return SUCCEEDED.equals(status) || FAILED.equals(status);
    }
}
