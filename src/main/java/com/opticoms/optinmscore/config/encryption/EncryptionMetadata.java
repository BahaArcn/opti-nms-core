package com.opticoms.optinmscore.config.encryption;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "encryption_metadata")
public class EncryptionMetadata {
    @Id
    private String id;
    private String saltBase64;
}
