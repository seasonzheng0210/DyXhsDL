package com.neoruaa.xhsdn;

final class ImageOrientationUtils {
    static final int ORIENTATION_UNDEFINED = 0;
    static final int ORIENTATION_NORMAL = 1;
    static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    static final int ORIENTATION_ROTATE_180 = 3;
    static final int ORIENTATION_FLIP_VERTICAL = 4;
    static final int ORIENTATION_TRANSPOSE = 5;
    static final int ORIENTATION_ROTATE_90 = 6;
    static final int ORIENTATION_TRANSVERSE = 7;
    static final int ORIENTATION_ROTATE_270 = 8;

    private ImageOrientationUtils() {
    }

    static boolean swapsWidthAndHeight(int orientation) {
        return orientation == ORIENTATION_TRANSPOSE
                || orientation == ORIENTATION_ROTATE_90
                || orientation == ORIENTATION_TRANSVERSE
                || orientation == ORIENTATION_ROTATE_270;
    }

    static int rotationDegrees(int orientation) {
        switch (orientation) {
            case ORIENTATION_TRANSPOSE:
            case ORIENTATION_ROTATE_90:
                return 90;
            case ORIENTATION_ROTATE_180:
            case ORIENTATION_FLIP_VERTICAL:
                return 180;
            case ORIENTATION_TRANSVERSE:
            case ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }
}
