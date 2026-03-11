package com.aimaster.service;

import com.aimaster.model.GeneratedVideo;
import com.aimaster.model.VideoGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Separate component so @Async works correctly (avoids Spring self-call proxy bypass).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncVideoWorker {

    private final BedrockRuntimeClient bedrockClient;
    private final StorageService storageService;

    @Async
    public void execute(GeneratedVideo video, VideoGenerationRequest request, String modelId, String bucket, String prefix) {
        try {
            long startTime = System.currentTimeMillis();

            // Nova Reel hard limit: 512 chars on the text prompt
            String prompt = request.getPrompt();
            if (prompt != null && prompt.length() > 512) {
                log.warn("Prompt truncated from {} to 512 chars for video {}", prompt.length(), video.getId());
                prompt = prompt.substring(0, 512);
            }

            Document.MapBuilder textToVideoParams = Document.mapBuilder()
                    .putString("text", prompt != null ? prompt : "");

            if (request.getReferenceImageBase64() != null && !request.getReferenceImageBase64().isEmpty()) {
                // Base64 string may arrive as a data URI: "data:image/jpeg;base64,/9j/..."
                // Nova Reel expects: { "format": "jpeg", "source": { "bytes": "<raw base64>" } }
                // Nova Reel also requires EXACTLY 1280x720 — resize automatically.
                String raw = request.getReferenceImageBase64();
                String format = "jpeg"; // default
                if (raw.startsWith("data:")) {
                    int semicolon = raw.indexOf(';');
                    if (semicolon > 0) {
                        String mime = raw.substring(5, semicolon); // e.g. "image/png"
                        format = mime.contains("/") ? mime.substring(mime.lastIndexOf('/') + 1) : mime;
                    }
                    int comma = raw.indexOf(',');
                    if (comma >= 0) raw = raw.substring(comma + 1);
                }
                // Normalise: Nova Reel only accepts "jpeg" or "png"
                if ("jpg".equalsIgnoreCase(format)) format = "jpeg";

                // Auto-resize to 1280x720 (center-crop, bicubic)
                raw = resizeImageToBase64(raw, format);
                log.info("Reference image resized to 1280x720 for video {}", video.getId());

                textToVideoParams.putDocument("images", Document.listBuilder()
                        .addDocument(Document.mapBuilder()
                                .putString("format", format)
                                .putDocument("source", Document.mapBuilder()
                                        .putString("bytes", raw)
                                        .build())
                                .build())
                        .build());
            }

            Document modelInputDoc = Document.mapBuilder()
                    .putString("taskType", "TEXT_VIDEO")
                    .putDocument("textToVideoParams", textToVideoParams.build())
                    .putDocument("videoGenerationConfig", Document.mapBuilder()
                            .putNumber("durationSeconds", 6)
                            .putNumber("fps", 24)
                            .putString("dimension", "1280x720")
                            .build())
                    .build();

            StartAsyncInvokeRequest asyncRequest = StartAsyncInvokeRequest.builder()
                    .modelId(modelId)
                    .modelInput(modelInputDoc)
                    .outputDataConfig(AsyncInvokeOutputDataConfig.builder()
                            .s3OutputDataConfig(AsyncInvokeS3OutputDataConfig.builder()
                                    .s3Uri("s3://" + bucket + "/" + prefix)
                                    .build())
                            .build())
                    .build();

            StartAsyncInvokeResponse asyncResult = bedrockClient.startAsyncInvoke(asyncRequest);
            String jobArn = asyncResult.invocationArn();

            video.setJobArn(jobArn);
            video.setStatus("PROCESSING");
            storageService.updateVideo(video);

            pollForCompletion(video, jobArn, startTime);

        } catch (Exception e) {
            log.error("Video generation failed for video {}", video.getId(), e);
            video.setStatus("FAILED");
            video.setErrorMessage(e.getMessage());
            video.setCompletedAt(LocalDateTime.now());
            storageService.updateVideo(video);
        }
    }

    /**
     * Resizes a base64-encoded image to exactly 1280x720 using scale-to-cover + center-crop.
     * Nova Reel rejects any reference image that is not exactly that resolution.
     */
    private String resizeImageToBase64(String rawBase64, String format) throws Exception {
        byte[] imageBytes = Base64.getDecoder().decode(rawBase64);
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) throw new IllegalArgumentException("Cannot decode reference image");

        int targetW = 1280, targetH = 720;
        if (original.getWidth() == targetW && original.getHeight() == targetH) {
            return rawBase64; // already perfect
        }

        // Scale-to-cover: enlarge so both dimensions fill the target, then center-crop
        double scaleX = (double) targetW / original.getWidth();
        double scaleY = (double) targetH / original.getHeight();
        double scale  = Math.max(scaleX, scaleY);
        int scaledW = (int) Math.round(original.getWidth()  * scale);
        int scaledH = (int) Math.round(original.getHeight() * scale);

        // Draw scaled
        BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, scaledW, scaledH);
        g.drawImage(original, 0, 0, scaledW, scaledH, null);
        g.dispose();

        // Center-crop to exactly 1280x720
        int cropX = Math.max(0, (scaledW - targetW) / 2);
        int cropY = Math.max(0, (scaledH - targetH) / 2);
        BufferedImage cropped = scaled.getSubimage(cropX, cropY, targetW, targetH);

        // Re-encode as JPEG (always use JPEG for size; PNG is also fine but larger)
        String ioFormat = "png".equalsIgnoreCase(format) ? "png" : "jpg";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!"png".equalsIgnoreCase(format)) {
            // Ensure TYPE_INT_RGB for JPEG (no alpha channel)
            BufferedImage rgb = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = rgb.createGraphics();
            g2.drawImage(cropped, 0, 0, null);
            g2.dispose();
            ImageIO.write(rgb, ioFormat, baos);
        } else {
            ImageIO.write(cropped, ioFormat, baos);
        }

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private void pollForCompletion(GeneratedVideo video, String jobArn, long startTime) {
        int maxAttempts = 120;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(5000);

                GetAsyncInvokeResponse statusResult = bedrockClient.getAsyncInvoke(
                        GetAsyncInvokeRequest.builder().invocationArn(jobArn).build());

                String status = statusResult.statusAsString();

                if ("Completed".equalsIgnoreCase(status)) {
                    long generationTime = System.currentTimeMillis() - startTime;
                    video.setStatus("COMPLETED");
                    video.setGenerationTimeMs(generationTime);
                    video.setCompletedAt(LocalDateTime.now());

                    if (statusResult.outputDataConfig() != null
                            && statusResult.outputDataConfig().s3OutputDataConfig() != null) {
                        video.setS3Uri(statusResult.outputDataConfig().s3OutputDataConfig().s3Uri());
                    }

                    storageService.updateVideo(video);
                    log.info("Video generation completed for job: {}", jobArn);
                    return;

                } else if ("Failed".equalsIgnoreCase(status)) {
                    video.setStatus("FAILED");
                    video.setCompletedAt(LocalDateTime.now());
                    video.setErrorMessage("Video generation job failed on AWS");
                    storageService.updateVideo(video);
                    log.error("Video generation failed for job: {}", jobArn);
                    return;
                }

                attempt++;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                video.setStatus("FAILED");
                video.setErrorMessage("Interrupted");
                storageService.updateVideo(video);
                return;
            } catch (Exception e) {
                log.error("Error checking video status", e);
                attempt++;
            }
        }

        video.setStatus("FAILED");
        video.setErrorMessage("Timeout: Video generation took too long");
        storageService.updateVideo(video);
    }
}
