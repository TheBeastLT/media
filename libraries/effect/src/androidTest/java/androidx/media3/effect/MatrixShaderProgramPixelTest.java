/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.effect;

import static androidx.media3.test.utils.BitmapPixelTestUtil.MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE;
import static androidx.media3.test.utils.BitmapPixelTestUtil.createArgb8888BitmapFromCurrentGlFramebuffer;
import static androidx.media3.test.utils.BitmapPixelTestUtil.createGlTextureFromBitmap;
import static androidx.media3.test.utils.BitmapPixelTestUtil.getBitmapAveragePixelAbsoluteDifferenceArgb8888;
import static androidx.media3.test.utils.BitmapPixelTestUtil.maybeSaveTestBitmapToCacheDirectory;
import static androidx.media3.test.utils.BitmapPixelTestUtil.readBitmap;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import androidx.media3.common.FrameProcessingException;
import androidx.media3.common.util.GlUtil;
import androidx.media3.test.utils.BitmapPixelTestUtil;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pixel test for texture processing via {@link MatrixShaderProgram}.
 *
 * <p>Expected images are taken from an emulator, so tests on different emulators or physical
 * devices may fail. To test on other devices, please increase the {@link
 * BitmapPixelTestUtil#MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE} and/or inspect the saved output
 * bitmaps as recommended in {@link GlEffectsFrameProcessorPixelTest}.
 */
@RunWith(AndroidJUnit4.class)
public final class MatrixShaderProgramPixelTest {
  public static final String ORIGINAL_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/electrical_colors/original.png";
  public static final String TRANSLATE_RIGHT_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/electrical_colors/translate_right.png";
  public static final String SCALE_NARROW_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/electrical_colors/scale_narrow.png";
  public static final String ROTATE_90_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/electrical_colors/rotate90.png";

  private final Context context = getApplicationContext();

  private @MonotonicNonNull EGLDisplay eglDisplay;
  private @MonotonicNonNull EGLContext eglContext;
  private @MonotonicNonNull SingleFrameGlShaderProgram matrixShaderProgram;
  private int inputTexId;
  private int inputWidth;
  private int inputHeight;

  @Before
  public void createGlObjects() throws IOException, GlUtil.GlException {
    eglDisplay = GlUtil.createEglDisplay();
    eglContext = GlUtil.createEglContext(eglDisplay);
    EGLSurface placeholderEglSurface = GlUtil.focusPlaceholderEglSurface(eglContext, eglDisplay);

    Bitmap inputBitmap = readBitmap(ORIGINAL_PNG_ASSET_PATH);
    inputWidth = inputBitmap.getWidth();
    inputHeight = inputBitmap.getHeight();
    inputTexId = createGlTextureFromBitmap(inputBitmap);
    int outputTexId =
        GlUtil.createTexture(inputWidth, inputHeight, /* useHighPrecisionColorComponents= */ false);
    int frameBuffer = GlUtil.createFboForTexture(outputTexId);
    GlUtil.focusFramebuffer(
        eglDisplay, eglContext, placeholderEglSurface, frameBuffer, inputWidth, inputHeight);
  }

  @After
  public void release() throws GlUtil.GlException, FrameProcessingException {
    if (matrixShaderProgram != null) {
      matrixShaderProgram.release();
    }
    if (eglContext != null && eglDisplay != null) {
      GlUtil.destroyEglContext(eglDisplay, eglContext);
    }
  }

  @Test
  public void drawFrame_noEdits_matchesGoldenFile() throws Exception {
    String testId = "drawFrame_noEdits";
    Matrix identityMatrix = new Matrix();
    MatrixTransformation noEditsTransformation = (long presentationTimeUs) -> identityMatrix;
    matrixShaderProgram = noEditsTransformation.toGlShaderProgram(context, /* useHdr= */ false);
    matrixShaderProgram.configure(inputWidth, inputHeight);
    Bitmap expectedBitmap = readBitmap(ORIGINAL_PNG_ASSET_PATH);

    matrixShaderProgram.drawFrame(inputTexId, /* presentationTimeUs= */ 0);
    Bitmap actualBitmap = createArgb8888BitmapFromCurrentGlFramebuffer(inputWidth, inputHeight);

    maybeSaveTestBitmapToCacheDirectory(testId, /* bitmapLabel= */ "actual", actualBitmap);
    // TODO(b/207848601): Switch to using proper tooling for testing against golden data.
    float averagePixelAbsoluteDifference =
        getBitmapAveragePixelAbsoluteDifferenceArgb8888(expectedBitmap, actualBitmap, testId);
    assertThat(averagePixelAbsoluteDifference).isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE);
  }

  @Test
  public void drawFrame_translateRight_matchesGoldenFile() throws Exception {
    String testId = "drawFrame_translateRight";
    Matrix translateRightMatrix = new Matrix();
    translateRightMatrix.postTranslate(/* dx= */ 1, /* dy= */ 0);
    MatrixTransformation translateRightTransformation =
        (long presentationTimeUs) -> translateRightMatrix;
    matrixShaderProgram =
        translateRightTransformation.toGlShaderProgram(context, /* useHdr= */ false);
    matrixShaderProgram.configure(inputWidth, inputHeight);
    Bitmap expectedBitmap = readBitmap(TRANSLATE_RIGHT_PNG_ASSET_PATH);

    matrixShaderProgram.drawFrame(inputTexId, /* presentationTimeUs= */ 0);
    Bitmap actualBitmap = createArgb8888BitmapFromCurrentGlFramebuffer(inputWidth, inputHeight);

    maybeSaveTestBitmapToCacheDirectory(testId, /* bitmapLabel= */ "actual", actualBitmap);
    // TODO(b/207848601): Switch to using proper tooling for testing against golden data.
    float averagePixelAbsoluteDifference =
        getBitmapAveragePixelAbsoluteDifferenceArgb8888(expectedBitmap, actualBitmap, testId);
    assertThat(averagePixelAbsoluteDifference).isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE);
  }

  @Test
  public void drawFrame_scaleNarrow_matchesGoldenFile() throws Exception {
    String testId = "drawFrame_scaleNarrow";
    Matrix scaleNarrowMatrix = new Matrix();
    scaleNarrowMatrix.postScale(.5f, 1.2f);
    MatrixTransformation scaleNarrowTransformation = (long presentationTimeUs) -> scaleNarrowMatrix;
    matrixShaderProgram = scaleNarrowTransformation.toGlShaderProgram(context, /* useHdr= */ false);
    matrixShaderProgram.configure(inputWidth, inputHeight);
    Bitmap expectedBitmap = readBitmap(SCALE_NARROW_PNG_ASSET_PATH);

    matrixShaderProgram.drawFrame(inputTexId, /* presentationTimeUs= */ 0);
    Bitmap actualBitmap = createArgb8888BitmapFromCurrentGlFramebuffer(inputWidth, inputHeight);

    maybeSaveTestBitmapToCacheDirectory(testId, /* bitmapLabel= */ "actual", actualBitmap);
    // TODO(b/207848601): Switch to using proper tooling for testing against golden data.
    float averagePixelAbsoluteDifference =
        getBitmapAveragePixelAbsoluteDifferenceArgb8888(expectedBitmap, actualBitmap, testId);
    assertThat(averagePixelAbsoluteDifference).isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE);
  }

  @Test
  public void drawFrame_rotate90_matchesGoldenFile() throws Exception {
    String testId = "drawFrame_rotate90";
    Matrix rotate90Matrix = new Matrix();
    rotate90Matrix.postRotate(/* degrees= */ 90);
    MatrixTransformation rotate90Transformation = (long presentationTimeUs) -> rotate90Matrix;
    matrixShaderProgram = rotate90Transformation.toGlShaderProgram(context, /* useHdr= */ false);
    matrixShaderProgram.configure(inputWidth, inputHeight);
    Bitmap expectedBitmap = readBitmap(ROTATE_90_PNG_ASSET_PATH);

    matrixShaderProgram.drawFrame(inputTexId, /* presentationTimeUs= */ 0);
    Bitmap actualBitmap = createArgb8888BitmapFromCurrentGlFramebuffer(inputWidth, inputHeight);

    maybeSaveTestBitmapToCacheDirectory(testId, /* bitmapLabel= */ "actual", actualBitmap);
    // TODO(b/207848601): Switch to using proper tooling for testing against golden data.
    float averagePixelAbsoluteDifference =
        getBitmapAveragePixelAbsoluteDifferenceArgb8888(expectedBitmap, actualBitmap, testId);
    assertThat(averagePixelAbsoluteDifference).isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE);
  }

  // TODO(b/227624622): Add a test for HDR input after BitmapPixelTestUtil can read HDR bitmaps.
}
