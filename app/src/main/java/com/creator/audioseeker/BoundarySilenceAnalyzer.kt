package com.creator.audioseeker

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

data class BoundarySilenceClip(
    val startPositionMs: Long = 0L,
    val endPositionMs: Long? = null,
) {
    val isClipped: Boolean get() = startPositionMs > 0L || endPositionMs != null
}

/**
 * Detects only the beginning and end of a local audio file. It never examines or removes
 * silence between spoken sections, and returns no trim when a decoder cannot be used safely.
 */
class BoundarySilenceAnalyzer(private val context: Context) {
    fun analyze(track: AudioTrack): BoundarySilenceClip = runCatching {
        val leadingSilenceMs = findLeadingSilenceMs(track)
        val trailingSilenceMs = findTrailingSilenceMs(track)
        val endPositionMs = (track.durationMs - trailingSilenceMs)
            .takeIf { it < track.durationMs && it > leadingSilenceMs + MIN_PLAYABLE_DURATION_MS }
        BoundarySilenceClip(leadingSilenceMs, endPositionMs)
    }.getOrDefault(BoundarySilenceClip())

    private fun findLeadingSilenceMs(track: AudioTrack): Long {
        var firstAudibleUs: Long? = null
        inspectPcm(track, 0L, LEADING_SCAN_US) { timestampUs, _, isSilent ->
            if (!isSilent && firstAudibleUs == null) firstAudibleUs = timestampUs
        }
        val silenceUs = firstAudibleUs ?: LEADING_SCAN_US
        return reduceBoundarySilenceMs(silenceUs / 1_000L)
    }

    private fun findTrailingSilenceMs(track: AudioTrack): Long {
        val durationUs = track.durationMs * 1_000L
        val scanStartUs = (durationUs - TRAILING_SCAN_US).coerceAtLeast(0L)
        var lastAudibleEndUs: Long? = null
        inspectPcm(track, scanStartUs, durationUs) { timestampUs, duration, isSilent ->
            if (!isSilent) lastAudibleEndUs = (timestampUs + duration).coerceAtMost(durationUs)
        }
        val silenceUs = durationUs - (lastAudibleEndUs ?: scanStartUs)
        return reduceBoundarySilenceMs(silenceUs / 1_000L)
    }

    private fun reduceBoundarySilenceMs(silenceMs: Long): Long =
        if (silenceMs < MIN_SILENCE_TO_REDUCE_MS) 0L else min(silenceMs, MAX_REDUCED_SILENCE_MS)

    private fun inspectPcm(
        track: AudioTrack,
        scanStartUs: Long,
        scanEndUs: Long,
        onPcmBuffer: (timestampUs: Long, durationUs: Long, isSilent: Boolean) -> Unit,
    ) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, track.uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return
            val format = extractor.getTrackFormat(trackIndex)
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: return
            extractor.selectTrack(trackIndex)
            if (scanStartUs > 0L) extractor.seekTo(scanStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            codec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(format, null, null, 0)
                start()
            }

            var inputEnded = false
            var outputEnded = false
            var outputFormat: MediaFormat? = null
            val bufferInfo = MediaCodec.BufferInfo()
            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val sampleTimeUs = extractor.sampleTime
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (sampleTimeUs == -1L || sampleTimeUs > scanEndUs || inputBuffer == null) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            inputBuffer.clear()
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = codec.outputFormat
                    MediaCodec.INFO_TRY_AGAIN_LATER,
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED,
                    -> Unit

                    else -> if (outputIndex >= 0) {
                        if (bufferInfo.size > 0 && outputFormat != null) {
                            val durationUs = pcmDurationUs(bufferInfo.size, outputFormat)
                            val endsAfterScanStart = bufferInfo.presentationTimeUs + durationUs > scanStartUs
                            if (endsAfterScanStart && bufferInfo.presentationTimeUs <= scanEndUs) {
                                codec.getOutputBuffer(outputIndex)?.let { buffer ->
                                    onPcmBuffer(
                                        bufferInfo.presentationTimeUs,
                                        durationUs,
                                        isSilentPcm(buffer, bufferInfo, outputFormat),
                                    )
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    }
                }
            }
        } finally {
            codec?.runCatching {
                stop()
                release()
            }
            extractor.release()
        }
    }

    private fun pcmDurationUs(byteCount: Int, format: MediaFormat): Long {
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 0)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 0)
        val encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        val bytesPerSample = when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> return 0L
        }
        if (sampleRate <= 0 || channelCount <= 0) return 0L
        return byteCount.toLong() * 1_000_000L / (sampleRate * channelCount * bytesPerSample)
    }

    private fun isSilentPcm(buffer: ByteBuffer, info: MediaCodec.BufferInfo, format: MediaFormat): Boolean {
        val data = buffer.duplicate().order(ByteOrder.nativeOrder()).apply {
            position(info.offset)
            limit(info.offset + info.size)
        }
        val encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        return when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> {
                while (data.hasRemaining()) {
                    if (abs(data.get().toInt() - 128) > PCM_8BIT_SILENCE_THRESHOLD) return false
                }
                true
            }

            AudioFormat.ENCODING_PCM_16BIT -> {
                val samples = data.asShortBuffer()
                while (samples.hasRemaining()) {
                    if (abs(samples.get().toInt()) > PCM_16BIT_SILENCE_THRESHOLD) return false
                }
                true
            }

            AudioFormat.ENCODING_PCM_FLOAT -> {
                val samples = data.asFloatBuffer()
                while (samples.hasRemaining()) {
                    if (abs(samples.get()) > PCM_FLOAT_SILENCE_THRESHOLD) return false
                }
                true
            }

            else -> false
        }
    }

    private companion object {
        const val DEQUEUE_TIMEOUT_US = 10_000L
        const val LEADING_SCAN_US = 3_000_000L
        const val TRAILING_SCAN_US = 5_000_000L
        const val MIN_SILENCE_TO_REDUCE_MS = 250L
        const val MAX_REDUCED_SILENCE_MS = 1_500L
        const val MIN_PLAYABLE_DURATION_MS = 1_000L
        const val PCM_8BIT_SILENCE_THRESHOLD = 2
        const val PCM_16BIT_SILENCE_THRESHOLD = 104
        const val PCM_FLOAT_SILENCE_THRESHOLD = 0.0032f
    }
}
