/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.transformer.mh;

import static com.google.android.exoplayer2.testutil.BitmapPixelTestUtil.MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE_DIFFERENT_DEVICE;
import static com.google.android.exoplayer2.testutil.BitmapPixelTestUtil.maybeSaveTestBitmap;
import static com.google.android.exoplayer2.testutil.BitmapPixelTestUtil.readBitmap;
import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import static com.google.android.exoplayer2.util.VideoFrameProcessor.INPUT_TYPE_BITMAP;
import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.effect.DefaultVideoFrameProcessor;
import com.google.android.exoplayer2.testutil.BitmapPixelTestUtil;
import com.google.android.exoplayer2.testutil.VideoFrameProcessorTestRunner;
import com.google.android.exoplayer2.util.VideoFrameProcessor;
import com.google.android.exoplayer2.video.ColorInfo;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link DefaultVideoFrameProcessor} texture output.
 *
 * <p>Confirms that the output timestamps are correct for each frame, and that the output pixels are
 * correct for the first frame of each bitmap.
 */
@RunWith(AndroidJUnit4.class)
public class DefaultVideoFrameProcessorMultipleTextureOutputPixelTest {
  private static final String ORIGINAL_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/electrical_colors/original.png";
  private static final String MEDIA3_TEST_PNG_ASSET_PATH =
      "media/bitmap/input_images/media3test.png";
  private static final String SRGB_TO_ELECTRICAL_ORIGINAL_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/electrical_colors/srgb_to_electrical_original.png";
  private static final String SRGB_TO_ELECTRICAL_MEDIA3_TEST_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/electrical_colors/srgb_to_electrical_media3test.png";

  private @MonotonicNonNull VideoFrameProcessorTestRunner videoFrameProcessorTestRunner;

  private @MonotonicNonNull TextureBitmapReader textureBitmapReader;

  @After
  public void release() {
    checkNotNull(videoFrameProcessorTestRunner).release();
  }

  @Test
  public void textureOutput_queueBitmap_matchesGoldenFile() throws Exception {
    String testId = "textureOutput_queueBitmap_matchesGoldenFile";
    videoFrameProcessorTestRunner = getFrameProcessorTestRunnerBuilder(testId).build();

    long offsetUs = 1_000_000L;
    videoFrameProcessorTestRunner.queueInputBitmap(
        readBitmap(ORIGINAL_PNG_ASSET_PATH),
        /* durationUs= */ 3 * C.MICROS_PER_SECOND,
        /* offsetToAddUs= */ offsetUs,
        /* frameRate= */ 1);
    videoFrameProcessorTestRunner.endFrameProcessing();

    TextureBitmapReader textureBitmapReader = checkNotNull(this.textureBitmapReader);
    Set<Long> outputTimestamps = textureBitmapReader.getOutputTimestamps();
    assertThat(outputTimestamps)
        .containsExactly(
            offsetUs, offsetUs + C.MICROS_PER_SECOND, offsetUs + 2 * C.MICROS_PER_SECOND);
    Bitmap actualBitmap = textureBitmapReader.getBitmap(offsetUs);
    maybeSaveTestBitmap(testId, /* bitmapLabel= */ "actual", actualBitmap, /* path= */ null);
    float averagePixelAbsoluteDifference =
        BitmapPixelTestUtil.getBitmapAveragePixelAbsoluteDifferenceArgb8888(
            readBitmap(SRGB_TO_ELECTRICAL_ORIGINAL_PNG_ASSET_PATH), actualBitmap, testId);
    assertThat(averagePixelAbsoluteDifference)
        .isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE_DIFFERENT_DEVICE);
  }

  @Test
  public void textureOutput_queueTwoBitmaps_matchesGoldenFiles() throws Exception {
    String testId = "textureOutput_queueTwoBitmaps_matchesGoldenFiles";
    videoFrameProcessorTestRunner = getFrameProcessorTestRunnerBuilder(testId).build();

    long offsetUs1 = 1_000_000L;
    videoFrameProcessorTestRunner.queueInputBitmap(
        readBitmap(ORIGINAL_PNG_ASSET_PATH),
        /* durationUs= */ C.MICROS_PER_SECOND,
        /* offsetToAddUs= */ offsetUs1,
        /* frameRate= */ 2);
    long offsetUs2 = 2_000_000L;
    videoFrameProcessorTestRunner.queueInputBitmap(
        readBitmap(MEDIA3_TEST_PNG_ASSET_PATH),
        /* durationUs= */ 3 * C.MICROS_PER_SECOND,
        /* offsetToAddUs= */ offsetUs2,
        /* frameRate= */ 1);
    videoFrameProcessorTestRunner.endFrameProcessing();

    TextureBitmapReader textureBitmapReader = checkNotNull(this.textureBitmapReader);
    Set<Long> outputTimestamps = textureBitmapReader.getOutputTimestamps();
    assertThat(outputTimestamps)
        .containsExactly(
            offsetUs1,
            offsetUs1 + C.MICROS_PER_SECOND / 2,
            offsetUs2,
            offsetUs2 + C.MICROS_PER_SECOND,
            offsetUs2 + 2 * C.MICROS_PER_SECOND);
    Bitmap actualBitmap1 = textureBitmapReader.getBitmap(offsetUs1);
    maybeSaveTestBitmap(testId, /* bitmapLabel= */ "actual1", actualBitmap1, /* path= */ null);
    Bitmap actualBitmap2 = textureBitmapReader.getBitmap(offsetUs2);
    maybeSaveTestBitmap(testId, /* bitmapLabel= */ "actual2", actualBitmap2, /* path= */ null);
    float averagePixelAbsoluteDifference1 =
        BitmapPixelTestUtil.getBitmapAveragePixelAbsoluteDifferenceArgb8888(
            readBitmap(SRGB_TO_ELECTRICAL_ORIGINAL_PNG_ASSET_PATH), actualBitmap1, testId);
    assertThat(averagePixelAbsoluteDifference1)
        .isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE_DIFFERENT_DEVICE);
    float averagePixelAbsoluteDifference2 =
        BitmapPixelTestUtil.getBitmapAveragePixelAbsoluteDifferenceArgb8888(
            readBitmap(SRGB_TO_ELECTRICAL_MEDIA3_TEST_PNG_ASSET_PATH), actualBitmap2, testId);
    assertThat(averagePixelAbsoluteDifference2)
        .isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE_DIFFERENT_DEVICE);
  }

  private VideoFrameProcessorTestRunner.Builder getFrameProcessorTestRunnerBuilder(String testId) {
    textureBitmapReader = new TextureBitmapReader();
    VideoFrameProcessor.Factory defaultVideoFrameProcessorFactory =
        new DefaultVideoFrameProcessor.Factory.Builder()
            .setTextureOutput(
                textureBitmapReader::readBitmapFromTexture, /* textureOutputCapacity= */ 1)
            .build();
    return new VideoFrameProcessorTestRunner.Builder()
        .setTestId(testId)
        .setVideoFrameProcessorFactory(defaultVideoFrameProcessorFactory)
        .setInputType(INPUT_TYPE_BITMAP)
        .setInputColorInfo(ColorInfo.SRGB_BT709_FULL)
        .setBitmapReader(textureBitmapReader);
  }
}
