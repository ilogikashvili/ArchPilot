package com.ilia.gikashvili.archmain.common.event;

/**
 * Marker interface for domain events.
 * Modules publish domain events and other modules listen to them via Spring events,
 * keeping modules decoupled.
 */
public interface DomainEvent {
}
