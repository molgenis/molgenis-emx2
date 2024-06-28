package org.molgenis.emx2.web.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AnalyticsTriggerAction(
    @JsonProperty("name") String name, @JsonProperty("cssSelector") String cssSelector) {}
