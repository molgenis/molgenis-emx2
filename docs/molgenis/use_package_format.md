# design notes (remove later)

Requirements (to be validated, not all yet implemented)
* package will be used as single entry point for creating all the apps we build
* we want to make a registry with 'generic' and 'specific' packages to distinghuis reusable and non-reusable packages
* a package should be able to configure all molgenis aspects (schema, settings, scripts, migrations, data, ontologies, demo data, also custom apps)
* we use 'name: xxx' instead of using the names as keys
* we enable custom package structure, using the 'imports' to clarify what goes where (instead we could do default folder structure)
* we can use imports to define what we currently do with profiles (do we like that or prefer the profile tags still?)
* imports can only select, not alter the imported table/column definitions
* we want multi inheritance so we can combine at runtime
* in theory, an import could also be remote (e.g. to load from an external ontology server)
* we want to be able to pass a github repository and then all packages from that repository should be choosable
* demo data and ontologies should also be possible to load as yaml (I see us struggling with demo data maintenance, would that be easier in yaml?)
* table hierarchies should be in one file, not split, so we can enforce column order

# MOLGENIS 'package' format

MOLGENIS platform allows to fully customize behavior. The results of such customization have great value to be shared. Therefore MOLGENIS comes with a
'package' format that allows these customizations to be systematically managed, bundled, shared and reused within and between MOLGENIS user communities.

The customizations that can be shared are:

* schemas and/or value sets/ontologies (i.e. table structures resulting in standard UI forms/explorers, APIs, upload/download file formats)
* custom UI screens (either by configuration or by adding custom HTML/javascript code)
* scripts (python scripts that can run on user command or as a cron job)
* queries (custom SQL or graphql queries that can be shared as a service)
* services (e.g. adapters between your data schema and external standard APIs, such as DCAT (RDF) and Beacon (web service based))

## Basic structure

Below a minimal example

```yaml
# molgenis.yaml
molgenis:
  package:
    title: My first package
    version: 1.0.0
    homepage: https://github.com/some/place
  schema:
    ${schemaName}:
      test
    tables:
    - name: Pets
      columns:
        - name: name
          key: 1
        - name: description
          type: text
        - name: category
          type: ref
          refSchema: lookups
          refTable: categories
    permissions:
    - role: anonymous
      view: [all]
    - role: pet shop manager
      edit: [all]
    data: ./data
    demoData: ./demodata
  additionalSchemas:
  - name: lookups
    tables: 
      - name: categories
        columns:
          - name: name
            key: 1
    permissions:
      - role: anonymous
        view: [all]
  settings:
    menu:
    - label: home
      path: /${schema}/tables
    - label: settings
      path: /${schema}/settings
```

## A larger example

Within one table definition subtyping can be done as follows:

```yaml
# catalogue.yaml
molgenis:
  packageMetadata: 
    name: Catalogue
    description: a molgenis schema for cohort studies and biobanks tailored towards supporting dcat-ap/health-dcat
    version: 2.0.1
  prefixes:
    foaf: http://xmlns.com/foaf/0.1/
    dct: http://purl.org/dc/terms/
    dcat: http://www.w3.org/ns/dcat#
    r5r: http://data.europa.eu/r5r/
    vcard: http://www.w3.org/2006/vcard/ns#
    org: https://www.w3.org/ns/org#
  settingsSchema: #extensions to the default settings of molgenis, enables settings UI
    menu:
      type: array
      items:
        type: object
        oneOf: 
        - required: [label,page]
        - required: [label,path]
        properties:
          label:
            type: string
          page:
            type: string
            format: page #should be one of the custom or pre-defined pages
          path:
            type: uri
          roles:
            type: array
            items:
              type: string
              format: role #molgenis will know to take from roles
        additionalProperties: false
    theme:
      type: object
      properties:
        logo:
          type: uri
        colors:
          type: object
          properties:
            primary: 
              type: string
    role_permissions:
      description: map of user roles (keys), and for each role their permissions (values)
      type: object
      additionalProperties:
        type: object
        required: [role]
        properties:
          description:
            type: string
            description: description of this role
          view:
            type: array
            description: list of tables that user has view permission on. [all] means all tables
            items:
              type: string
              format: table-name #custom type in molgenis to allow validation
            default: []
          create:
            type: array
            description:
              list of tables that role has create permission on, i.e. users can add rows and subsequently edit their own created rows. The 'role' becomes owner of the row so other users in this role can do the same
              [all] means all tables
            items:
              type: string
              format: table-name
            default: []
          edit:
            type: array
            description: 
              list of tables that user has edit permission on, i.e. users can create and edit all rows in a table. [all] means all tables
            items:
              type: string
              format: table-name
            default: []
          delete:
            type: array
            description:
              list of tables that user has delete permission on, i.e. users can delete rows
            items:
              type: string
              format: table-name
            default: []          
  settings:
    role_permissions:
      anonymous:
        view: [all]
      submitter:
        view: [all]
        create: [all] #can create new rows, and edit those
      reviewer:
        view: [all]
        edit: [Catalogues,Datasets,Subcohorts,Collection events,Contacts] #can only edit the data tables
      curator:
        edit: [all]
    menu: 
    - label: Tables
      page: tables
    - label: Settings
      page: settings
      roles: [editor]
    - label: Dashboard
      page: dashboard
    - label: About
      page: about
    theme:
      logo: /${schema}/catalogue/logo.png
      colors:
        primary: #673bb7
  namedSchemas: 
    # can create named schemas
    - name: CatalogueOntologies
  schema:
  - table: Catalogues
    semantics: dcat:Catalog
    description: A list of catalogues or repositories that hosts the Datasets or Data Services being described
    columns:
    - name: identifier
      description: globally unique identifier such as a doi
      semantics: dct:identifier
      key: 1
    - name: title
      semantics: dct:title
      description: A name given to the Catalogue
      key: 2
    - name: description
      semantics: dct:description
      description: 	A free-text account of the Catalogue
      required: true
    - name: publisher
      semantics: dct:publisher
      type: ref
      refTable: Organisations
      required: true
    - name: datasets
      semantics: dcat:dataset
      type: ref_array
      refTable: Datasets
  - table: Datasets
    semantics: dcat:Dataset
    description: record of an individual data collection
    context: #hardcoded snippet to include in generated json-ld @context
      theme: #ensure we output static theme value as part of rdf mapping
        "@id": dcat:theme
        "@value": <http://publications.europa.eu/resource/authority/data-theme/HEAL>
    subclasses:
    - name: Cohort studies #in graphql will become CohortStudies, it converts sentence to pascal case
      description: Dataset that is a cohort study
      when: type?.find(t => t.name === 'Cohort study')
    - name: Biobanks
      description: Dataset that is a biobank
      when: type?.find(t => t.name === 'Biobank')
    columns:
    - name: identifier
      description: globally unique identifier such as a doi
      semantics: dct:identifier
      key: 1
    - name: title
      semantics: dct:title
      description: A name given to the Dataset
      key: 2
    - name: description
      semantics: dct:description
      description: 	A free-text account of the Dataset
      type: text
    - name: countries
      semantics: dct:spatial
      type: ontology_array 
      refTable: Countries #will automatically create a ontology table with that name
    - name: regions
      semantics: dct:spatial
      type: ontology_array
      refTable: Regions
    - name: topics
      semantics: dcat:keyword
      type: ontology_array
      refTable: Topics
      required: true
    - name: theme
      semantics: dcat:theme
      type: ontology_array
      refTable: Themes
      defaultValue: HEAL #so this will always have the value 'heal'
      required: true
      visible: false
    - name: access rights #in graphql will become accessRights, it converts sentence to camel case
      semantics: dct:accessRights
      type: ontology
      refTable: Right statements
      defaultValue: restricted
    - name: applicable legislation
      semantics: r5r:applicableLegislation
      type: ontology_array
      refTable: Legislation identifiers
      required: true
    - name: contact point
      semantics: dcat:contactPoint
      type: ref_array
      refTable: Contact points
      required: true
    - name: publisher
      semantics: dct:publisher
      type: ref
      refTable: Organisations
      required: true
    - name: type
      type: ontology_array 
      refTable: Resource types
    - name: cohort type
      type: ontology_array
      refTable: Cohort types
      subclass: [Cohort study]
    - name: subcohorts
      description: subpopulations or subcohorts
      type: refback
      refTable: Subcohorts
      refback: partOfCollection
  - table: Subcohorts
    tableType: auxiliary #default tableType='data' or 'ontology'
    columns:
    - name: partOfCollection
      key: 1
    - name: name
      key: 1
  - table: Collection events
    tableType: auxiliary # tableType = data | auxiliary | ontology
    columns:
    - name: partOfCollection
      key: 1
    - name: name
      key: 1
  - table: Contacts
    description: Contact
    semantics: [foaf:Agent, vcard:Kind]
    subclasses:
    - name: Organisations
      semantics: foaf:Organization
      when: type.name === 'Organisation'
    - name: Organisational unit
      semantics: org:OrganizationalUnit
      when: type.name === 'Organisation unit'
    - name: Person
      when: type.name === 'Individual'
    columns:
    - name: identifier
      key: 1
      type: autoid
    - name: name
      description: name of the organisation or person
    - name: type
      type: ontology
      semantics: dct:type
      refTable: Agent types
      required: true
    - name: homepage
      semantics: [foaf:homepage,vcard:hasURL]
      type: url
      description: location to make contact
      required: true
    - name: email
      semantics: [foaf:email, vcard:hasEmail]
      type: email
      required: true
    - name: member of organisation
      semantics: org:memberOf
      type: ontology_array
      refTable: Research organisation registry
  migrations:
  - version: 2
    script: migration2.sql
  pages: #these can be used in the menu next to the default stuff
  - name: details
    type: html
    contents: 
      <h1>lorem ipsom html here</h1><p>this is pretty rough but effective</p>
  - name: about
    type: markdown
    import: /pages/about.md #this is nicer for larger stuff to use the import option
  - name: dashboard
    type: configurable #this is a composable format
    elements:
    - kind: title
      title: Dashboard
    - kind: horizontal-bar #this is a container type
      elements:
      - kind: pie-chart
        label: Number of datasets per topic
        data:
          table: Dataset
          groupBy: topic
          aggregator: count
      - kind: pie-chart
        label: Number of datasets per contact
        data:
          table: Dataset
          groupBy: contact
          aggregator: count
```


# Imports

if one wants to make a subset / combine data models or split your molgenis config over multipe files this could be done by importing files

The `import` mechanism allows Molgenis configurations to be split across multiple files and recombined into a single effective schema and/or settings model.

## General principles

- An `import` object **may appear anywhere** a list of schema objects is allowed (for example: `settings`, `schema`, `columns`).
- The **scope of an import is determined by its position** in the document:
    - Under `settings`: imports settings fragments
    - Under `schema`: imports tables
    - Under `columns`: imports columns only (not the table definition)
- An `import` object **must be the only key at its level** (it must not be combined with other keys).

## Import source

A 'package' is minimally a molgenis.yaml file, but usually will be a folder containing different parts of the application. Below an example.

```
molgenis.yaml   contains metadata that describes the package
/schema          contains data models
/data           contains data to be loaded
/demodata       contains demo data, when provided demo data
/ui             contains ui configs or full ui 'spa' apps
/scripts        contains scripts

```

This splitting can be done using the 'imports' feature.

- Imports reference another Molgenis YAML file using `path`.
- The referenced file must contain a valid `molgenis` root element.
- Imports may be nested and resolved recursively.

### Example

```yaml
# example.yaml
molgenis: 
  settings:
  - imports:
      path: ./settings.yaml #some settings are in this yaml file
  schema: #extra schema definitions
  - imports: # at this level will import whole schema of catalogue.yaml, optionally filtered 
      path: ./catalogue.yaml 
      include:
      - table: Catalogues
        columns:
        - identifier
        - title
        - datasets
      - table: Datasets
        subclasses: 
        - Cohort studies
        columns:
        - identifier
        - title
  - table: my own table
    columns:
    - imports: #silly example, at column level import will only import the columns, not the table metadata
        path: ./catalogue.yaml
        include:
        - table: Catalogues
          columns:
          - identifier
          - title
    - name: identifier
      description: this overrides the imported description, last entry wins
    - import:
        path: mycolumns.yaml #no filter means include all columns from that file

---
# mycolumns.yaml, contains only some columns I want to reuse as library
molgenis:
  columns:
  - name: my extra special column
    description: this is just a silly example to demo modular molgenis files for data model fragments
---
# settings.yaml
molgenis:
  settings:
    menu:
    # contents ommited for brevity
```

## Package metadata

Using the package metadata you can describe your package as a whole which is useful when adding your package to a molgenis package repository.

```yaml
#package.yaml example
molgenis:
  package:
    # platform dependency
    # this id should be unique when sharing in a registry, e.g. in molgenis-packages
    id: @molgenis/first
    version: 2.0.1
    # user friendly title and description
    title: my first package
    description:
      this is a very minimal example package. It will create a static lookup schema if not exists, and then will add a schema which name depends on
      schemaName parameter. In addition it bundles to custom UI.
    keywords:
    - free form
    - keywords
    - to ease findability
    - in a registry
    parameters:
    - name: schemaName
      type: string
      description: this parameter for the user to determine define the schema instance name of the newly created schema when applying this mod
    repository: http://github.com/my/repo
    homepage: http://my.molgenis.org
    documentation: http://mydocs.molgenis.org
    license: http://my/licence
    citations:
      doi: 10.1234/example.registry.2024
    authors:
    - name: John Doe
      email: redacted
      role: Lead
```

## Creating custom settings

It is possible to define new custom settings, by adding a settingsSchema in json schema format

An example, using existing molgenis settings as illustration:
```yaml
molgenis:
  settingsSchema: #extensions to the default settings of molgenis, enables settings UI
  menu:
    type: array
    items:
      type: object
      oneOf:
      - required: [label,page]
      - required: [label,path]
      properties:
        label:
          type: string
        page:
          type: string
          format: page #should be one of the custom or pre-defined pages
        path:
          type: uri
        roles:
          type: array
          items:
            type: string
            format: role #molgenis will know to take from roles
      additionalProperties: false
  theme:
    type: object
    properties:
      logo:
        type: uri
      colors:
        type: object
        properties:
          primary:
            type: string
  role_permissions:
    description: map of user roles (keys), and for each role their permissions (values)
    type: object
    additionalProperties:
      type: object
      required: [role]
      properties:
        description:
          type: string
          description: description of this role
        view:
          type: array
          description: list of tables that user has view permission on. [all] means all tables
          items:
            type: string
            format: table-name #custom type in molgenis to allow validation
          default: []
        create:
          type: array
          description:
            list of tables that role has create permission on, i.e. users can add rows and subsequently edit their own created rows. The 'role' becomes owner of the row so other users in this role can do the same
            [all] means all tables
          items:
            type: string
            format: table-name
          default: []
        edit:
          type: array
          description:
            list of tables that user has edit permission on, i.e. users can create and edit all rows in a table. [all] means all tables
          items:
            type: string
            format: table-name
          default: []
        delete:
          type: array
          description:
            list of tables that user has delete permission on, i.e. users can delete rows
          items:
            type: string
            format: table-name
          default: []
  settings:
    role_permissions:
      anonymous:
        view: [all]
      submitter:
        view: [all]
        create: [all] #can create new rows, and edit those
      reviewer:
        view: [all]
        edit: [Catalogues,Datasets,Subcohorts,Collection events,Contacts] #can only edit the data tables
      curator:
        edit: [all]
    menu:
    - label: Tables
      page: tables
    - label: Settings
      page: settings
      roles: [editor]
    - label: Dashboard
      page: dashboard
    - label: About
      page: about
    theme:
      logo: /${schema}/catalogue/logo.png
      colors:
        primary: #673bb7

```

## Multiple inheritance

MOLGENIS supports a form of multiple inheritance, but with a strict limitation:
- each inheritance tree must have ONE base table (which minimally will define a common primary key)
- if tables inherit from multiple tables, then these tables should have same base table
- individual rows in 

This feature allows data modelers to create a modular table, where depending on 



# some notes and drafts below

```yaml


  # schemas contains a list of schemas in this package
  schemas:
  - id: lookups
    label: my lookup schema
    description:
      This package uses a central schema for all my lookups. Is an emerging best practice in MOLGENIS to allow multiple databases to share the same lookup
      lists.
    data: lookups/data
    # will load the data from lookupSchema directory. This path is the default so would have been included anyway
  - id: patient-registry
    targetSchema: ${schemaName}
    # ${schemaName} is a way to parameterize the actual schema name to be deployed to
    demoData: patient registry schema/demo
    # demo data is used when user chooses to instantiate the mod with demo data
    description: a data model for a patient registry, showing off some off some of the features
    settings:
      menu:
      - label: Home
        link: /${schema}/home
      - label: Tables
        link: /${schema}/tables
        # tables is a standard app
      - label: About
        link: /${schema}/about
        # about is a custom app, see below







```

## UI and pages

```yaml
    ui:
    # this is ui specific to this schema. UI can also be 'global'
    # ui can either be a configuration or simply link to a html/js dist bundle
    # this would also work if you would create a folder 'ui/myapp'
    # this app will only live in context of instances of this schema
    - path: home
      description: this path will produce a page based on configuration. This is fake example below
      config:
      - component: PageContainer
        elements:
        - component: Heading
          settings:
            title: Welcome friend
        - component: Paragraph
          settings:
            contents:
              This is a snippet of <b>html</b> that will be wrapped in a paragraph on this homepage
        - component: CTA panel
          settings:
            links:
            - title: Background
              link: ${schema}/background
            - title: About
              link: ${schema}/about
            - title: View data
              link: ${schema}/tables
    - path: about
      # dist contains the path of the compiled html/javascript, should have an index.html
      dist: ui/about
      # this ui is a separate app; this path would be default
    - path: background
      config:
      - component: PageContainer
        elements:
        - component: Paragraph
          settings:
            contents:
              Some background comes here.
```

# Feature checklist

Not to forget:

- [] when having  multiple imports mixed with my own fields I would like to sort in one go
- [] can create multiple schemas
- [x] can define cross schema (global) roles