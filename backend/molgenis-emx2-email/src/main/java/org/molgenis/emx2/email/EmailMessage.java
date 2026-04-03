package org.molgenis.emx2.email;

import java.util.List;
import java.util.Optional;

public record EmailMessage(
    List<String> recipients, String subject, String messageText, Optional<String> bccRecipient) {}
