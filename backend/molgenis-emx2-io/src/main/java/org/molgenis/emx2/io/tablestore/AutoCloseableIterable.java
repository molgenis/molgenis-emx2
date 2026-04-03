package org.molgenis.emx2.io.tablestore;

public interface AutoCloseableIterable<T extends Object> extends Iterable<T>, AutoCloseable {}
