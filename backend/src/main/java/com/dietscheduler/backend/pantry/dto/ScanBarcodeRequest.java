package com.dietscheduler.backend.pantry.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanBarcodeRequest(@NotBlank String barcode) {
}
