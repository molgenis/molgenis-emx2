<template>
  <Page id="page-home">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="About the ERN CRANIO registry"
      height="medium"
    />
    <Breadcrumbs />
    <PageSection
      id="section-intro-title"
      aria-labelledby="section-intro-title"
      :verticalPadding="2"
    >
      <h2 id="section-intro-title">ERN CRANIO registry</h2>
      <p>
        In 2019, ERN CRANIO responded to a call under the third programme for
        the union's action in the field of health (2014-2020) and applied for
        financial support to develop a patient registry. The network has
        successfully obtained this support to develop this registry.
      </p>
      <p>
        ERN CRANIO is developing a registry focused on the outcome of treatment
        using standardised diagnosis specific outcome sets (starting with
        craniosynostosis and cleft lip/palate) - including patient reported
        outcome measures and outcomes on patients' quality of life.
      </p>
      <p>
        To support the goals of ERN CRANIO, to pool together disease-specific
        expertise, knowledge and resources from across Europe, a European-wide
        registry is being developed for patients with rare and complex
        craniofacial anomalies, and ENT disorders. This registry allows for
        secure collection of patient data from across Europe, which will improve
        the understanding and treatment of these conditions. It will also
        facilitate research and lead to better care for these patients.
      </p>
      <p>
        The registry covers a wide range of rare craniofacial anomalies and ENT
        disorders, including craniosynostosis, cleft lip and palate, rare
        genetic deafness, craniofacial microsomia, and orodental anomalies. For
        each diagnosis, a group of European specialists worked together to reach
        consensus on relevant outcome measures and treatment endpoints. This
        will allow for comparison of care on a level not previously seen, which
        will greatly increase opportunities for improvement.
      </p>
      <p>
        The registry was built and is hosted in collaboration with MOLGENIS, a
        company that specializes in registry development and support. The
        platform is user-friendly and provides the highest levels of patient
        data security. Additionally, it supports FAIR interfaces for data
        exchange.
      </p>
      <p>
        Currently the datasets used for craniosynostosis, and cleft lip and
        palate are ready for implementation. The ERN CRANIO registry team and
        European specialists are currently working on finalizing datasets for
        rare genetic deafness and craniofacial microsomia. In case of any
        questions regarding the ERN CRANIO registry, please email to:
        <a href="mailto:ern-cranio@erasmusmc.nl">ern-cranio@erasmusmc.nl</a>.
      </p>
      <DataTable
        tableId="workstreams"
        :data="data"
        :columnOrder="['workstream', 'ERN CRANIO dataset manager']"
        :enableRowHighlighting="false"
      />
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection, DataTable } from "molgenis-viz";
import Breadcrumbs from "../components/breadcrumbs.vue";

const data = [
  {
    workstream: "Craniosynostosis",
    "ERN CRANIO dataset manager": "Linda Gaillard",
  },
  {
    workstream: "Cleft lip and Palate",
    "ERN CRANIO dataset manager": "Philip van der Goes & Victor van Roey",
  },
  {
    workstream: "Rare genetic deafness",
    "ERN CRANIO dataset manager": "Philip van der Goes",
  },
  {
    workstream: "Craniofacial microsomia",
    "ERN CRANIO dataset manager": "Linda Gaillard & Victor van Roey",
  },
  {
    workstream: "Orodental anomalies",
    "ERN CRANIO dataset manager": "Philip van der Goes",
  },
];
</script>
