package com.ilia.gikashvili.archmain.realtime;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OperationsApplied(UUID projectId, UUID userId, String clientId, long revision, List<Map<String, Object>> operations) {}
