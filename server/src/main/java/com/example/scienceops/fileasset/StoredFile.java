package com.example.scienceops.fileasset;

import java.nio.file.Path;

public record StoredFile(
        FileAssetRecord asset,
        Path path
) {
}
