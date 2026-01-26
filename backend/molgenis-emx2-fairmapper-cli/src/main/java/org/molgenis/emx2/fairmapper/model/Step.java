package org.molgenis.emx2.fairmapper.model;

import java.util.List;

public record Step(String transform, String query, List<TestCase> tests) {}
