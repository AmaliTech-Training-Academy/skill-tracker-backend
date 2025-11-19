package com.amalitech.notification.service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class NotificationDocument {

    @Id
    private String id;
    
    private UUID userId;
    
    private UUID submissionId;
    
    private String type;
    
    private String title;
    
    private String message;

    @Indexed(name = "created_at_ttl", expireAfterSeconds = 2592000)
    private LocalDateTime createdAt;
    
    private boolean read;

    private Map<String, Object> context;
}
