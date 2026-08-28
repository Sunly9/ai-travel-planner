package org.example.travel_project1.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class streamChunkVO {
    private String type = "chunk";
    private String content;

    public static streamChunkVO of(String content){
        return new streamChunkVO("chunk",content);
    }

}
