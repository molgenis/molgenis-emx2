package org.molgenis.emx2.web.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendMailAction(
    @JsonProperty("to") String to, @JsonProperty("subject") String subject, String body) {}
