package org.molgenis.emx2.rdf.writers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.io.Closeable;

abstract class RdfWriter implements Closeable {
    abstract void consumeNamespace(Namespace namespace);

    abstract void consumeTriple(Statement statement);

    abstract void consumeTriple(Resource subject, IRI predicate, Value object);
}
