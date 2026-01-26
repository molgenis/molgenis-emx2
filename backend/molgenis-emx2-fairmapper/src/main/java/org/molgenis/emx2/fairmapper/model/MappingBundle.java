package org.molgenis.emx2.fairmapper.model;

import java.util.List;

public record MappingBundle(String name, String version, List<Endpoint> endpoints) {}
