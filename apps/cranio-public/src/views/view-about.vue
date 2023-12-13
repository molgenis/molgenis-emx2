<template>
  <Page id="page-home">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="About the ERN CRANIO registry"
      imageSrc="banner-diagnoses.jpg"
    />
    <Breadcrumbs />
    <PageSection
      id="section-intro-title"
      aria-labelledby="section-intro-title"
      :verticalPadding="2"
      :horizontalPadding="0"
    >
      <h2 id="section-intro-title">ERN CRANIO registry</h2>
      <p>
        The ERN CRANIO Registry is a cutting-edge, secure web-based application
        with a mission to establish a patient registry tailored to evaluate
        clinical and patient-reported outcomes, ultimately advancing long-term
        treatment strategies for individuals affected by rare craniofacial and
        ENT disorders.
      </p>
      <p>
        This objective is achieved by systematically collecting standardized
        disease-specific data as an integral part of the standard treatment
        process.
      </p>
      <p>
        Identifying the most effective treatment outcomes empowers a
        comprehensive analysis of predictive factors, placing a specific
        emphasis on the type and timing of surgical interventions within each
        unique center. The result is the generation of robust recommendations
        for best clinical practices, benefiting not only ERN CRANIO centers but
        extending this knowledge to a broader healthcare community.
      </p>
      <p>
        Patient data is gathered encompassing the severity of the phenotype,
        diagnostic methods employed, treatment modalities, and a diverse range
        of clinical and patient-reported outcomes.
      </p>
      <p>
        In a collaborative partnership with MOLGENIS, an expert in registry
        development and support, the registry is built and hosted, further
        enhancing its functionality by supporting FAIR interfaces for seamless
        data exchange.
      </p>
      <p>
        To ensure utmost data security, all information incorporated into the
        ERN CRANIO Registry is pseudonymized through the ERDRI SPIDER tool,
        safeguarding it with the highest levels of patient data security.
      </p>
      <p>
        At present, datasets for Craniosynostosis and Cleft/Lip and Palate are
        ready for immediate implementation.
      </p>
      <p>
        Concurrently, the ERN CRANIO Registry team is diligently working to
        finalize datasets for rare genetic deafness and craniofacial microsomia,
        broadening the scope of this invaluable resource.
      </p>
      <p>
        For any questions regarding ERN CRANIO registry, please email:
        <a href="mailto:ern-cranioregistry@erasmusmc.nl"
          >ern-cranioregistry@erasmusmc.nl</a
        >.
      </p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
import Breadcrumbs from "../components/breadcrumbs.vue";
</script>
