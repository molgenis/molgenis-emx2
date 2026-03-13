package org.molgenis.emx2.yaml.format;

import lombok.Builder;

@Builder
public record YamlColumn(
    String name,
    String label,
    String formLabel,
    String type,
    Integer key,
    String[] semantics,
    String description,
    String refTable,
    String refSchema,
    String refBack,
    String defaultValue,
    String validation,
    String visible,
    String subclass) {}
