package org.salvationarmy.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkValidateResponse {
    @Builder.Default
    private List<BulkImportRowResult> blocked = new ArrayList<>();
    @Builder.Default
    private List<BulkImportRowResult> warnings = new ArrayList<>();
    @Builder.Default
    private List<BulkImportRowResult> clean = new ArrayList<>();
}
