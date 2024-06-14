package org.molgenis.emx2.cafevariome;

public class CafeVariomeQueries {

  protected static final String fullQuery =
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
			      "sim": [
			        {
			          "r": "0.75",
			          "s": "2",
			          "ORPHA": "true",
			          "ids": [
			            "HP:0003144",
			            "HP:0011158"
			          ]
			        }
			      ],
			      "ordo": [
			        {
			          "r": "0.75",
			          "s": "57",
			          "HPO": "true",
			          "id": [
			            "ORPHA:139036"
			          ]
			        }
			      ],
			      "reactome": [
			        {
			          "reactom_id": "R-HSA-3371378",
			          "protein_effect": [

			          ],
			          "af": "200"
			        }
			      ],
			      "gene": [
			        {
			          "gene_id": "ADGRF3",
			          "protein_effect": [

			          ],
			          "af": "200"
			        },
			        {
			          "gene_id": "AKIRIN2",
			          "protein_effect": [

			          ],
			          "af": "200"
			        }
			      ],
			      "demography": [
			        {
			          "minAge": [
			            "0"
			          ],
			          "maxAge": [
			            "99"
			          ],
			          "affected": [
			            "Affected"
			          ],
			          "gender": [
			            "male"
			          ],
			          "family_type": [
			            "singleton"
			          ]
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

  protected static final String ordoQuery =
      """
			{
			  "query": {
			    "components": {
			    "ordo": [
			        {
			          "r": "0.75",
			          "s": "57",
			          "HPO": "true",
			          "id": [
			            "ORPHA:1955"
			          ]
			        }
			      ]
			    }
			  }
			}
			""";

  protected static final String hpoQuery =
      """
			{
			  "query": {
			    "components": {
			      "sim": [
			        {
			          "r": "0.75",
			          "s": "2",
			          "ORPHA": "true",
			          "ids": [
			            "HP:0012651"
			          ]
			        }
			      ]
			    }
			  }
			}
			""";

  protected static final String combinationQuery =
      """
			{
			  "query": {
			    "components": {
			    "gene": [
			        {
			          "gene_id": "COL7A1"
			        }
			      ],
			      "demography": [
			        {
			          "minAge": [
			            "0"
			          ],
			          "maxAge": [
			            "99"
			          ],
			          "affected": [
			            "Affected"
			          ],
			          "gender": [
			            "GSSO_009509"
			          ],
			          "family_type": [
			            "singleton"
			          ]
			        }
			      ]
			    }
			  }
			}
			""";

  protected static final String multiGeneQuery =
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
