package org.molgenis.emx2.fairmapper.model;

import java.util.List;

public record Endpoint(String path, List<String> methods, List<Step> steps) {}
