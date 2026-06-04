package com.naijavehicle.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_verifications")
@CompoundIndexes({
        @CompoundIndex(name = "user_plate_idx", def = "{'userId': 1, 'plateNumber': 1}", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVerification {

    @Id
    private String id;

    private String userId;

    private String plateNumber;

    private LocalDateTime verifiedAt;
}
