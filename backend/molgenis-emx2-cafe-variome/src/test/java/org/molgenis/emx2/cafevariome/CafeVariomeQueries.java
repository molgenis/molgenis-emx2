package org.molgenis.emx2.cafevariome;

public class CafeVariomeQueries {

  protected static final String multiGeneNoHeaderQuery =
      """
			{
			  "query": {
			    "components": {
			      "gene": [
			        {
			          "gene_id": "COL7A1"
			        },
			        {
			          "gene_id": "TTN"
			        },
			        {
			          "gene_id": "CHD7"
			        }
			      ]
			    }
			  }
			}
			""";

  protected static final String geneFullHeaderQuery =
      """
			{
			  "meta": {
			    "request": {
			      "components": {
			        "search": {
			          "subjectVariant": "1.0.0",
			          "eav": "1.0.0",
			          "phenotype": "1.0.0",
			          "queryIdentification": "1.0.0"
			        }
			      }
			    },
			    "apiVersion": "1.0.0",
			    "components": {
			      "queryIdentification": {
			        "queryID": "",
			        "queryLabel": "Search from client X for user on [date]"
			      }
			    }
			  },
			  "requires": {
			    "response": {
			      "components": {
			        "collection": {
			          "exists": "1.0.0",
			          "count": "1.0.0"
			        }
			      }
			    }
			  },
			  "query": {
			    "components": {
			      "gene": [
			        {
			          "gene_id": "COL7A1",
			          "protein_effect": [

			          ],
			          "af": "200"
			        }
			      ]
			    }
			  },
			  "logic": {
			    "-AND": [
			      "/query/components/demography/0",
			      {
			        "-OR": [
			          "/query/components/reactome/0",
			          "/query/components/gene/0",
			          "/query/components/gene/1"
			        ]
			      },
			      "/query/components/ordo/0",
			      "/query/components/sim/0"
			    ]
			  }
			}
			""";
}
