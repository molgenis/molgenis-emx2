# Introduction

[MOLGENIS](http://molgenis.org) is the world's most customisable platform for curation of (scientific) data. At the core, is EMX2&mdash;a flexible markup format built on FAIR principles (findability, accessibility, interoperability and reusability)&mdash; that allows users to quickly define a data model that contains tables and columns, as well as the relationships between them. Using your EMX2 model, MOLGENIS generates a complete database application with advanced data entry forms, powerful data up/download options and flexible query tools to help you collect, analyse and share your data.

For developers, you can use PostgreSQL, GraphQL api, batch web services or linked data RDF/TTL interface to query/update the data, and use VueJS to create your own 'apps'.

To get started with MOLGENIS, we have created several guides.

| Guide                       | Description                                                                                                                  |
|:----------------------------|:-----------------------------------------------------------------------------------------------------------------------------|
| [User guide](./use)         | reference for how to create and interact with MOLGENIS databases                                                             |
| [Installation Guide](./run) | step-by-step guide for installing and configuring the MOLGENIS software                                                      |
| [Developers Guide](./dev)   | learn about the architecture of MOLGENIS, how to contribute to the code base, how to build custom vue applications, and more |

## Differences with previous version of MOLGENIS

[molgenis-emx2](http://github.com/molgenis/molgenis-emx2) (or EMX2) is the newest iteration of the MOLGENIS platform. It builds on the previous version ([molgenis/molgenis](https://github.com/molgenis/molgenis), or EMX1) by:

- **Simpler setup and operation**: We simplified the core structure and EMX2 is built using PostgreSQL, Java, GraphQL, and Vue.
- **Enhanced data modelling features**: we enabled the option for multiple data schemas, schema templates, and FAIR data improvements (e.g., the `ONTOLOGY` data type). For developers, the data API specification is now auto-generated. Any changes to the schema will be automatically documented in the GraphQL API
- **Improved data APIs**: In EMX2, we've incorporated [GraphQL](https://graphql.org). This allows users to write more advanced queries

We are still in the process of adding EMX1 features into EMX2. For now, EMX1 will be maintained but not actively developed. It will eventually be deprecated. Please check the [molgenis/molgenis](https://github.com/molgenis/molgenis) for any updates.
