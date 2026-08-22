package com.mythweave.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsPendingWriteBuffer {

    private static final int MAX_BUFFER_SIZE = 1000;
    private final ConcurrentLinkedQueue<PendingWrite> buffer = new ConcurrentLinkedQueue<>();

    public enum WriteType { CHAPTER_CONTENT, ENTITY }

    public record PendingWrite(
            WriteType type, Long novelId, Long chapterId,
            String chunkType, String text, long bufferedAt
    ) {}

    public boolean bufferChapterContent(Long novelId, Long chapterId, String content) {
        if (buffer.size() >= MAX_BUFFER_SIZE) {
            log.warn("ES write buffer full({})", MAX_BUFFER_SIZE);
            return false;
        }
        buffer.offer(new PendingWrite(WriteType.CHAPTER_CONTENT, novelId, chapterId, null, content, System.currentTimeMillis()));
        log.info("ES down, chapter {} content buffered (depth: {})", chapterId, buffer.size());
        return true;
    }

    public boolean bufferEntity(Long novelId, String chunkType, String text) {
        if (buffer.size() >= MAX_BUFFER_SIZE) {
            log.warn("ES write buffer full({})", MAX_BUFFER_SIZE);
            return false;
        }
        buffer.offer(new PendingWrite(WriteType.ENTITY, novelId, null, chunkType, text, System.currentTimeMillis()));
        log.info("ES down, [{}] entity buffered (depth: {})", chunkType, buffer.size());
        return true;
    }

    public int replay(EmbeddingService embeddingService) {
        if (buffer.isEmpty()) return 0;
        List<PendingWrite> pending = new ArrayList<>();
        PendingWrite pw;
        while ((pw = buffer.poll()) != null) {
            pending.add(pw);
        }
        int successCount = 0;
        for (PendingWrite write : pending) {
            try {
                switch (write.type()) {
                    case CHAPTER_CONTENT -> {
                        embeddingService.indexChapterContent(write.novelId(), write.chapterId(), write.text());
                        successCount++;
                    }
                    case ENTITY -> {
                        embeddingService.indexEntity(write.novelId(), write.chunkType(), write.text());
                        successCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("Replay failed: type={}, novelId={}, error={}", write.type(), write.novelId(), e.getMessage());
                buffer.offer(write);
            }
        }
        log.info("ES write buffer replay done: {}/{}", successCount, pending.size());
        return successCount;
    }

    public int getBufferSize() { return buffer.size(); }
    public boolean isEmpty() { return buffer.isEmpty(); }
}
