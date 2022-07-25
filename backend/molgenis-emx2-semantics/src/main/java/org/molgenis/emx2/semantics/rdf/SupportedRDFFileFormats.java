package org.molgenis.emx2.semantics.rdf;

import org.eclipse.rdf4j.rio.RDFFormat;

import java.util.Map;
import java.util.TreeMap;

public class SupportedRDFFileFormats {

	public static Map<String, RDFFormat> RDF_FILE_FORMATS =
			new TreeMap<>(
					Map.of(
							"ttl",
							RDFFormat.TURTLE,
							"n3",
							RDFFormat.N3,
							"ntriples",
							RDFFormat.NTRIPLES,
							"nquads",
							RDFFormat.NQUADS,
							"xml",
							RDFFormat.RDFXML,
							"trig",
							RDFFormat.TRIG,
							"jsonld",
							RDFFormat.JSONLD));
}
