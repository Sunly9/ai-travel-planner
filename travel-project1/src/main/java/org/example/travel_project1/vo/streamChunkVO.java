package org.example.travel_project1.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * SSE 流式片段（原类名 streamChunkVO 小写开头，已修正）
 */
@Data
@AllArgsConstructor
public class StreamChunkVO {
    private String type = "chunk";
    private String content;

    public static StreamChunkVO of(String content) {
        return new StreamChunkVO("chunk", content);
    }
}
