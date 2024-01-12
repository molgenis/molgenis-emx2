package org.molgenis.emx2.datamodels.test;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.DATE;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class JsonLdExample {
  public static void create(Schema schema) {
    Table personTable =
        schema.create(
            table("Person")
                .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#Person")
                .add(
                    column("name")
                        .setPkey()
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#name"),
                    column("jobTitle")
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#jobTitle")
                        .setRequired(true),
                    column("telephone")
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#telephone"),
                    column("url")
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#url")));

    personTable.insert(
        row(
            "name",
            "Jane Doe",
            "jobTitle",
            "Professor",
            "telephone",
            "(425) 123-4567",
            "url",
            "http://www.janedoe.com"),
        row("id", "2", "name", "Mary Stone", "jobTitle", "Cook"));

    Table recipeTable =
        schema.create(
            table("Recipe")
                .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#Recipe")
                .add(
                    column("name")
                        .setPkey()
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#name"),
                    column("author")
                        .setType(REF)
                        .setRefTable("Person")
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#author")
                        .setRequired(true),
                    column("datePublished")
                        .setType(DATE)
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#datePublished")
                        .setRequired(true),
                    column("prepTime")
                        .setSemantics("https://schema.org/docs/jsonldcontext.jsonld#prepTime")
                        .setRequired(true)));

    recipeTable.insert(
        row(
            "name",
            "Mary's Cookies",
            "author",
            "Mary Stone",
            "datePublished",
            "2018-03-10",
            "prepTime",
            "PT20M"));
  }
}
