package com.ilia.gikashvili.archmain.review.dto;

import java.util.UUID;

/**
 * Request to start an architecture review. When aiKeyId is set, one of the caller's saved AI provider keys
 * also reviews the design as a senior architect, on top of the built-in senior rules.
 */
public record StartReviewRequest(String engineType, UUID aiKeyId) {}
