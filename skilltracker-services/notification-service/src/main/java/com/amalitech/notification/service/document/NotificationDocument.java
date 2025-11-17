package com.amalitech.notification.service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
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
    
    private LocalDateTime createdAt;
    
    private boolean read;
    
    private Object data;
}
