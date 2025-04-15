# Researcher

The [MOLGENIS Data Catalogue](https://data-catalogue.molgeniscloud.org/) is perfectly suited
to help you find the relevant data for your research question.
Using the catalogue, you can explore a variety of resources and compose a dataset
tailored to your research needs.

## Find rich metadata

Entering the catalogue, the landing page shows a list of available subcatalogues.
This list is divided between thematic catalogues, which cover a specific subject
and originate from collaborations between multiple projects,
and project catalogues, which originate from individual projects.

![Landing page of the MOLGENIS Data Catalogue](../img/cat_homepage.png)

Selecting one of these, e.g. **EHEN**, will take you to a new landing page, where you can choose
to browse either collections, networks, or variables.
Alternatively, if you want to search across all data, regardless of project, you can use the **SEARCH ALL** button.

![Landing page of a subcatalogue, in this case EHEN](../img/cat_catalogue-landing-page.png)

Selecting either **COLLECTIONS** or **NETWORKS** at the next step will provide you with a list of
collections or networks to browse through. To narrow down the list, you can do a text-based search (e.g. 'early pregnancy')
or filter on specific attributes (e.g. collections of type 'biobank').
For more information on searching and filtering, see [this section](#searching-and-filtering) below.

![List of collections, with search and filters](../img/cat_collections-list.png)

Clicking an item in the list will take you to a page containing detailed information on the resource in question.
Here, all information available in the catalogue about the resource is displayed.
You can either scroll through the page or navigate to a specific section of interest using the
index on the left-hand side. Also note that for some types of information, such as collection events or datasets,
clicking on a specific instance will provide more details about it (e.g. inclusion criteria for a certain subpopulation).

![Resource information page for resource BIB](../img/cat_resource-information-page.png)

## Find harmonised variables

Harmonised variables (also called target variables) are variables
which are defined in the context of a common data model. For example, a network of organisations with access
to multiple data sources, like LifeCycle has defined a set of variables, called the common data model,
which all data sources can map their own variables to.

Variable mapping is the process of defining how to convert the collected variable in the data source (source variable)
to the harmonised variable in the common data model (target variable).
For example, a collected variable describing length at birth in inches can be mapped to a harmonised variable
describing length at birth in centimetres. These mappings can then be used as a basis for combined analysis.

From the catalogue landing page, you can find all harmonised variables
by clicking **ALL VARIABLES** in the menu.

![Go to the variable explorer or a project page](../img/cat_homepage-variables.png)

Alternatively, you can choose to only view the harmonised variables within a particular project,
such as the EUChildNetwork or LifeCycle. In that case, first go to the project you are interested in
and then click **VARIABLES**.

![Go to the project variable explorer](../img/cat_project-variables.png)

Please note that the catalogue only describes the harmonised variables,
it does *not* contain the actual data values.

You can use the search bar and filters to subset the harmonised variables.
In the example below, we searched for harmonised variables related to 'diabetes'.
Additionally, you can filter on topics to find relevant variables.
You can also make a selection of sources so that only variables
mapped to by these sources are shown.
For more information on searching and filtering, see [this section](#searching-and-filtering) below.

![Variable explorer with search](../img/cat_variables-search-bar.png)

*In the future you will be able to use a shopping cart to select all variables of interest and create an overview.*

## See harmonisation details

The harmonisation view (click **HARMONISATIONS**) allows you to see
which sources have (partially) harmonised your variables of interest and
thus have those variables available for analysis.

![Variable explorer harmonisation matrix](../img/cat_harmonisation-specifications.png)

By clicking on a variable of interest (here `dia_bf`), a detailed harmonisation overview
of that particular variable is displayed, containing both the harmonisation status per source and
further details about the harmonisation like source variables and harmonisation syntax.

![Variable detail view](../img/cat_detailed-harmonised-variable-view.png)

## Searching and filtering

Since the catalogue contains many resources and variables, there are several ways of narrowing down the lists of results
when exploring.

![Sidebar with search and filters](../img/cat_search-and-filters.png)

The most straightforward way of finding relevant information is using the search bar. Enter your search term of interest
and all resources or variables containing the term in any of their attributes will be returned.
For example, searching for 'diabetes' in collections will return collections with 'diabetes' in their name
as well as those with 'diabetes' in their list of main medical conditions or the names of their subpopulations.

In addition to searching for a term, there is also a set of filters on specific attributes which can be used,
together with the search bar or by themselves. For collections (and networks), these are:

- **Collection type**: the type of collection you are interested in, e.g. biobank, cohort study, health records.
- **Areas of information**: the topics a collection has data on, e.g. education, medication, chemical exposure.
- **Data categories**: the type of data collected by a collection, e.g. medical records, biological samples, surveys.
- **Population age groups**: the age groups a collection has data on, e.g. prenatal, 65+, adolescents.
- **Sample categories**: in case the collection contains biological samples, which types of samples have been collected.
E.g. tears, frozen tissue, stem cells.
- **Cohort types**: in case the collection is a cohort study, what type of cohort study it is,
e.g. birth cohort, population cohort, clinical cohort.
- **Design**: the study design of the collection, e.g. longitudinal, cross-sectional.
- **Diseases**: the diseases the collection has collected data on, using ICD-10 and Orphanet codes,
e.g. Kostmann syndrome, diseases of the respiratory system, Hodgkin lymphoma.

And for variables, these are:

- **Topics**: the variables contain information regarding these topics,
e.g. allergies, lifestyle and behaviours, surgical interventions.
- **Sources**: show only variables originating from these collections, e.g. ABCD, Lifelines, GenR.

![List of collections filtered on collection type 'biobank' and population age group 'child'](../img/cat_filters-collections.png)

Note that, since some filters have a large number of options, each filter also allows for searching
within those filter options by clicking on **Search for options**. Filters can be combined
to make your search more specific. For example, filtering on collection type 'biobank' and population age group 'child'
yields a list of biobanks containing data on children ages 2 to 12.
At the top of the results list you can see which filters are currently applied.
Here you can also remove a specific filter by clicking on it, e.g. **Collection type**,
or remove all filters at once by clicking **Remove all**.
