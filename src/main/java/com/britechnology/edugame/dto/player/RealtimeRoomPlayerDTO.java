package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeRoomPlayerDTO {
    private Long id;
    private String name;
    private String avatar;
    private Integer age;
    private boolean ready;
    private boolean host;
}
