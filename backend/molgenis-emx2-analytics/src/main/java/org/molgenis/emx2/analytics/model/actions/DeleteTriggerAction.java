package org.molgenis.emx2.analytics.model.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeleteTriggerAction(@JsonProperty("name") String name) {}
