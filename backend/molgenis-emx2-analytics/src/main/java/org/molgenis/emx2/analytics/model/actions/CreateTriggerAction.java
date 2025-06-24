package org.molgenis.emx2.analytics.model.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateTriggerAction(
    @JsonProperty("name") String name, @JsonProperty("cssSelector") String cssSelector) {}
