package org.molgenis.emx2.fairmapper.pipeline.definition;

import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;

/**
 * Runtime dependencies that components need but that can't be written in a pipeline file. Built
 * once, after the pipeline file has been read, and handed to every spec when it creates its
 * component.
 *
 * @param client client for the EMX2 instance configured in the pipeline file
 * @param schema metadata of the target schema
 */
public record PipelineContext(GraphqlClient client, SchemaMetadata schema) {}
