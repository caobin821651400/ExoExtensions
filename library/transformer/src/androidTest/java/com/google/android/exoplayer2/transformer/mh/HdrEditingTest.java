/*
 * Copyright 2022 The Android Open Source Project
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
package com.google.android.exoplayer2.transformer.mh;

import static com.google.android.exoplayer2.transformer.AndroidTestUtil.FORCE_TRANSCODE_VIDEO_EFFECTS;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_1080P_5_SECOND_HLG10;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_1080P_5_SECOND_HLG10_FORMAT;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_720P_4_SECOND_HDR10;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_720P_4_SECOND_HDR10_FORMAT;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_DOLBY_VISION_HDR;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.MP4_ASSET_DOLBY_VISION_HDR_FORMAT;
import static com.google.android.exoplayer2.transformer.AndroidTestUtil.recordTestSkipped;
import static com.google.android.exoplayer2.transformer.mh.FileUtil.assertFileHasColorTransfer;
import static com.google.android.exoplayer2.util.MimeTypes.VIDEO_H265;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.transformer.AndroidTestUtil;
import com.google.android.exoplayer2.transformer.EditedMediaItem;
import com.google.android.exoplayer2.transformer.EncoderUtil;
import com.google.android.exoplayer2.transformer.ExportException;
import com.google.android.exoplayer2.transformer.ExportTestResult;
import com.google.android.exoplayer2.transformer.TransformationRequest;
import com.google.android.exoplayer2.transformer.Transformer;
import com.google.android.exoplayer2.transformer.TransformerAndroidTestRunner;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.ColorInfo;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link Transformer} instrumentation test for applying an {@linkplain
 * TransformationRequest#HDR_MODE_KEEP_HDR HDR frame edit}.
 */
@RunWith(AndroidJUnit4.class)
public final class HdrEditingTest {
  private static final ColorInfo HDR10_DEFAULT_COLOR_INFO =
      new ColorInfo.Builder()
          .setColorSpace(C.COLOR_SPACE_BT2020)
          .setColorRange(C.COLOR_RANGE_LIMITED)
          .setColorTransfer(C.COLOR_TRANSFER_ST2084)
          .build();
  private static final ColorInfo HLG10_DEFAULT_COLOR_INFO =
      new ColorInfo.Builder()
          .setColorSpace(C.COLOR_SPACE_BT2020)
          .setColorRange(C.COLOR_RANGE_LIMITED)
          .setColorTransfer(C.COLOR_TRANSFER_HLG)
          .build();

  @Test
  public void export_transmuxHdr10File() throws Exception {
    String testId = "export_transmuxHdr10File";
    Context context = ApplicationProvider.getApplicationContext();

    if (Util.SDK_INT < 24) {
      // TODO: b/285543404 - Remove suppression once we can transmux H.265/HEVC before API 24.
      recordTestSkipped(context, testId, /* reason= */ "Can't transmux H.265/HEVC before API 24");
      return;
    }

    if (AndroidTestUtil.skipAndLogIfFormatsUnsupported(
        context,
        testId,
        /* inputFormat= */ MP4_ASSET_720P_4_SECOND_HDR10_FORMAT,
        /* outputFormat= */ null)) {
      return;
    }

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_720P_4_SECOND_HDR10));

    ExportTestResult exportTestResult =
        new TransformerAndroidTestRunner.Builder(context, transformer)
            .build()
            .run(testId, mediaItem);
    assertFileHasColorTransfer(context, exportTestResult.filePath, C.COLOR_TRANSFER_ST2084);
  }

  @Test
  public void export_transmuxHlg10File() throws Exception {
    String testId = "export_transmuxHlg10File";
    Context context = ApplicationProvider.getApplicationContext();

    if (Util.SDK_INT < 24) {
      // TODO: b/285543404 - Remove suppression once we can transmux H.265/HEVC before API 24.
      recordTestSkipped(context, testId, /* reason= */ "Can't transmux H.265/HEVC before API 24");
      return;
    }

    if (AndroidTestUtil.skipAndLogIfFormatsUnsupported(
        context,
        testId,
        /* inputFormat= */ MP4_ASSET_1080P_5_SECOND_HLG10_FORMAT,
        /* outputFormat= */ null)) {
      return;
    }

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_1080P_5_SECOND_HLG10));

    ExportTestResult exportTestResult =
        new TransformerAndroidTestRunner.Builder(context, transformer)
            .build()
            .run(testId, mediaItem);
    assertFileHasColorTransfer(context, exportTestResult.filePath, C.COLOR_TRANSFER_HLG);
  }

  @Test
  public void exportAndTranscode_hdr10File_whenHdrEditingIsSupported() throws Exception {
    String testId = "exportAndTranscode_hdr10File_whenHdrEditingIsSupported";
    Context context = ApplicationProvider.getApplicationContext();
    if (!deviceSupportsHdrEditing(VIDEO_H265, HDR10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device lacks HDR10 editing support.");
      return;
    }

    if (AndroidTestUtil.skipAndLogIfFormatsUnsupported(
        context,
        testId,
        /* inputFormat= */ MP4_ASSET_720P_4_SECOND_HDR10_FORMAT,
        /* outputFormat= */ MP4_ASSET_720P_4_SECOND_HDR10_FORMAT)) {
      return;
    }

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_720P_4_SECOND_HDR10));
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(FORCE_TRANSCODE_VIDEO_EFFECTS).build();

    ExportTestResult exportTestResult =
        new TransformerAndroidTestRunner.Builder(context, transformer)
            .build()
            .run(testId, editedMediaItem);
    assertFileHasColorTransfer(context, exportTestResult.filePath, C.COLOR_TRANSFER_ST2084);
  }

  @Test
  public void exportAndTranscode_hlg10File_whenHdrEditingIsSupported() throws Exception {
    String testId = "exportAndTranscode_hlg10File_whenHdrEditingIsSupported";
    Context context = ApplicationProvider.getApplicationContext();
    if (!deviceSupportsHdrEditing(VIDEO_H265, HLG10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device lacks HLG10 editing support.");
      return;
    }

    if (AndroidTestUtil.skipAndLogIfFormatsUnsupported(
        context,
        testId,
        /* inputFormat= */ MP4_ASSET_1080P_5_SECOND_HLG10_FORMAT,
        /* outputFormat= */ MP4_ASSET_1080P_5_SECOND_HLG10_FORMAT)) {
      return;
    }

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_1080P_5_SECOND_HLG10));
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(FORCE_TRANSCODE_VIDEO_EFFECTS).build();

    ExportTestResult exportTestResult =
        new TransformerAndroidTestRunner.Builder(context, transformer)
            .build()
            .run(testId, editedMediaItem);
    assertFileHasColorTransfer(context, exportTestResult.filePath, C.COLOR_TRANSFER_HLG);
  }

  @Test
  public void exportAndTranscode_dolbyVisionFile_whenHdrEditingIsSupported() throws Exception {
    String testId = "exportAndTranscode_dolbyVisionFile_whenHdrEditingIsSupported";
    Context context = ApplicationProvider.getApplicationContext();
    // This dolby vision file has a ColorInfo identical to HLG10.
    if (!deviceSupportsHdrEditing(VIDEO_H265, HLG10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device lacks HLG10 editing support.");
      return;
    }

    if (AndroidTestUtil.skipAndLogIfFormatsUnsupported(
        context,
        testId,
        /* inputFormat= */ MP4_ASSET_DOLBY_VISION_HDR_FORMAT,
        /* outputFormat= */ MP4_ASSET_DOLBY_VISION_HDR_FORMAT)) {
      return;
    }

    Transformer transformer = new Transformer.Builder(context).build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_DOLBY_VISION_HDR));
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(FORCE_TRANSCODE_VIDEO_EFFECTS).build();

    ExportTestResult exportTestResult =
        new TransformerAndroidTestRunner.Builder(context, transformer)
            .build()
            .run(testId, editedMediaItem);
    assertFileHasColorTransfer(context, exportTestResult.filePath, C.COLOR_TRANSFER_HLG);
  }

  @Test
  public void exportAndTranscode_hdr10File_whenHdrEditingUnsupported_toneMapsOrThrows()
      throws Exception {
    String testId = "exportAndTranscode_hdr10File_whenHdrEditingUnsupported_toneMapsOrThrows";
    Context context = ApplicationProvider.getApplicationContext();
    if (deviceSupportsHdrEditing(VIDEO_H265, HDR10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device supports HDR10 editing.");
      return;
    }

    if (AndroidTestUtil.skipAndLogIfFormatsUnsupported(
        context,
        testId,
        /* inputFormat= */ MP4_ASSET_720P_4_SECOND_HDR10_FORMAT,
        /* outputFormat= */ MP4_ASSET_720P_4_SECOND_HDR10_FORMAT
            .buildUpon()
            .setColorInfo(ColorInfo.SDR_BT709_LIMITED)
            .build())) {
      return;
    }

    AtomicBoolean isFallbackListenerInvoked = new AtomicBoolean();
    AtomicBoolean isToneMappingFallbackApplied = new AtomicBoolean();
    Transformer transformer =
        new Transformer.Builder(context)
            .addListener(
                new Transformer.Listener() {
                  @Override
                  public void onFallbackApplied(
                      MediaItem inputMediaItem,
                      TransformationRequest originalTransformationRequest,
                      TransformationRequest fallbackTransformationRequest) {
                    isFallbackListenerInvoked.set(true);
                    assertThat(originalTransformationRequest.hdrMode)
                        .isEqualTo(TransformationRequest.HDR_MODE_KEEP_HDR);
                    isToneMappingFallbackApplied.set(
                        fallbackTransformationRequest.hdrMode
                            == TransformationRequest.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC);
                  }
                })
            .build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_720P_4_SECOND_HDR10));
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(FORCE_TRANSCODE_VIDEO_EFFECTS).build();

    try {
      ExportTestResult exportTestResult =
          new TransformerAndroidTestRunner.Builder(context, transformer)
              .build()
              .run(testId, editedMediaItem);
      assertThat(isToneMappingFallbackApplied.get()).isTrue();
      assertFileHasColorTransfer(context, exportTestResult.filePath, C.COLOR_TRANSFER_SDR);
    } catch (ExportException exception) {
      if (exception.getCause() != null
          && (Objects.equals(
                  exception.getCause().getMessage(),
                  "Tone-mapping HDR is not supported on this device.")
              || Objects.equals(
                  exception.getCause().getMessage(),
                  "Tone-mapping requested but not supported by the decoder."))) {
        // Expected on devices without a tone-mapping plugin for the this codec.
        return;
      }
      throw exception;
    }
  }

  @Test
  public void exportAndTranscode_hlg10File_whenHdrEditingUnsupported_toneMapsOrThrows()
      throws Exception {
    String testId = "exportAndTranscode_hlg10File_whenHdrEditingUnsupported_toneMapsOrThrows";
    Context context = ApplicationProvider.getApplicationContext();
    if (deviceSupportsHdrEditing(VIDEO_H265, HLG10_DEFAULT_COLOR_INFO)) {
      recordTestSkipped(context, testId, /* reason= */ "Device supports HLG10 editing.");
      return;
    }

    if (AndroidTestUtil.skipAndLogIfFormatsUnsupported(
        context,
        testId,
        /* inputFormat= */ MP4_ASSET_1080P_5_SECOND_HLG10_FORMAT,
        /* outputFormat= */ MP4_ASSET_1080P_5_SECOND_HLG10_FORMAT
            .buildUpon()
            .setColorInfo(ColorInfo.SDR_BT709_LIMITED)
            .build())) {
      return;
    }

    AtomicBoolean isFallbackListenerInvoked = new AtomicBoolean();
    AtomicBoolean isToneMappingFallbackApplied = new AtomicBoolean();
    Transformer transformer =
        new Transformer.Builder(context)
            .addListener(
                new Transformer.Listener() {
                  @Override
                  public void onFallbackApplied(
                      MediaItem inputMediaItem,
                      TransformationRequest originalTransformationRequest,
                      TransformationRequest fallbackTransformationRequest) {
                    isFallbackListenerInvoked.set(true);
                    assertThat(originalTransformationRequest.hdrMode)
                        .isEqualTo(TransformationRequest.HDR_MODE_KEEP_HDR);
                    isToneMappingFallbackApplied.set(
                        fallbackTransformationRequest.hdrMode
                            == TransformationRequest.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC);
                  }
                })
            .build();
    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(MP4_ASSET_1080P_5_SECOND_HLG10));
    EditedMediaItem editedMediaItem =
        new EditedMediaItem.Builder(mediaItem).setEffects(FORCE_TRANSCODE_VIDEO_EFFECTS).build();

    try {
      ExportTestResult exportTestResult =
          new TransformerAndroidTestRunner.Builder(context, transformer)
              .build()
              .run(testId, editedMediaItem);
      assertThat(isToneMappingFallbackApplied.get()).isTrue();
      assertFileHasColorTransfer(context, exportTestResult.filePath, C.COLOR_TRANSFER_SDR);
    } catch (ExportException exception) {
      if (exception.getCause() != null
          && (Objects.equals(
                  exception.getCause().getMessage(),
                  "Tone-mapping HDR is not supported on this device.")
              || Objects.equals(
                  exception.getCause().getMessage(),
                  "Tone-mapping requested but not supported by the decoder."))) {
        // Expected on devices without a tone-mapping plugin for this codec.
        return;
      }
      throw exception;
    }
  }

  private static boolean deviceSupportsHdrEditing(String mimeType, ColorInfo colorInfo) {
    return !EncoderUtil.getSupportedEncodersForHdrEditing(mimeType, colorInfo).isEmpty();
  }
}
