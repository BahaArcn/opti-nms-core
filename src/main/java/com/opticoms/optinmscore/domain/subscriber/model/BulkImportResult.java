package com.opticoms.optinmscore.domain.subscriber.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a bulk subscriber import operation")
public class BulkImportResult {

    @Schema(description = "Total records found in the uploaded file")
    private int totalInFile;

    @Schema(description = "Successfully imported count")
    private int successCount;

    @Schema(description = "Failed validation count")
    private int failedValidation;

    @Schema(description = "Skipped because license limit reached")
    private int skippedDueToLicense;

    @Schema(description = "Skipped because duplicate IMSI within the file")
    private int skippedDuplicateInFile;

    @Schema(description = "Skipped because IMSI already exists in database")
    private int skippedDuplicateInDb;

    @Schema(description = "Per-row error details")
    private List<RowError> errors;

    @Schema(description = "Summary message")
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        @Schema(description = "Row number in the file (1-based, 0 if unknown)")
        private int row;
        @Schema(description = "IMSI value (null if IMSI itself is invalid)")
        private String imsi;
        @Schema(description = "Field that caused the error")
        private String field;
        @Schema(description = "Error description")
        private String reason;
    }
}
