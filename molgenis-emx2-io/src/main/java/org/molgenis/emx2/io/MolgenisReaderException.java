package org.molgenis.emx2.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MolgenisReaderException extends IOException {
    private final List<MolgenisReaderMessage> messages = new ArrayList<>();

    public MolgenisReaderException(String message) {
        super(message);
    }

    public MolgenisReaderException(Exception e) {
        super(e);
    }

    public MolgenisReaderException(List<MolgenisReaderMessage> messages) {
        super("MolgenisReader failed: See getMessages() for list of error messages");
        this.messages.addAll(messages);
    }

    public List<MolgenisReaderMessage> getMessages() {
        return Collections.unmodifiableList(this.messages);
    }

}
