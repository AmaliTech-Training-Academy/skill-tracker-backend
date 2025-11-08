package com.amalitech.analytics.service.exception;

import java.util.UUID;

public class EntityNotFoundException extends AnalyticsException {
    public EntityNotFoundException(String entityName, UUID entityId) {
        super(String.format("%s with ID %s not found.", entityName, entityId));
    }

    public EntityNotFoundException(String entityName, String identifier) {
        super(String.format("%s identified by '%s' not found.", entityName, identifier));
    }
}
