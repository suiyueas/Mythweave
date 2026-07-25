package com.novelcraft.web.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SentinelStats {
    private long total;
    private long foreshadowing;
    private long logic;
    private long character;
    private long rhythm;
    private long resolved;
    private long unresolved;
}
