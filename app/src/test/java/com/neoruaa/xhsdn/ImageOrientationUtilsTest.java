package com.neoruaa.xhsdn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ImageOrientationUtilsTest {
    @Test
    public void swapsWidthAndHeight_forQuarterTurns() {
        assertTrue(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_ROTATE_90));
        assertTrue(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_ROTATE_270));
        assertTrue(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_TRANSPOSE));
        assertTrue(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_TRANSVERSE));
    }

    @Test
    public void swapsWidthAndHeight_forOtherOrientations_isFalse() {
        assertFalse(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_UNDEFINED));
        assertFalse(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_NORMAL));
        assertFalse(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_FLIP_HORIZONTAL));
        assertFalse(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_ROTATE_180));
        assertFalse(ImageOrientationUtils.swapsWidthAndHeight(ImageOrientationUtils.ORIENTATION_FLIP_VERTICAL));
    }

    @Test
    public void rotationDegrees_matchesExifSemantics() {
        assertEquals(0, ImageOrientationUtils.rotationDegrees(ImageOrientationUtils.ORIENTATION_NORMAL));
        assertEquals(90, ImageOrientationUtils.rotationDegrees(ImageOrientationUtils.ORIENTATION_ROTATE_90));
        assertEquals(180, ImageOrientationUtils.rotationDegrees(ImageOrientationUtils.ORIENTATION_ROTATE_180));
        assertEquals(270, ImageOrientationUtils.rotationDegrees(ImageOrientationUtils.ORIENTATION_ROTATE_270));
        assertEquals(90, ImageOrientationUtils.rotationDegrees(ImageOrientationUtils.ORIENTATION_TRANSPOSE));
        assertEquals(270, ImageOrientationUtils.rotationDegrees(ImageOrientationUtils.ORIENTATION_TRANSVERSE));
    }
}
