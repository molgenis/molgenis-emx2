package org.molgenis.emx2.analytics.model.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateTriggerAction(@JsonProperty("cssSelector") String cssSelector) {}
