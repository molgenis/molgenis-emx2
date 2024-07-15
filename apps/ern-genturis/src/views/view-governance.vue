<template>
  <Page id="page-governance">
    <PageHeader
      class="genturis-header"
      title="ERN Genturis"
      subtitle="Goverance of the GENTURIS Registry"
      imageSrc="genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection
      id="genturis-section-governance"
      aria-labelledby="genturis-section-governance-title"
      :verticalPadding="2"
      width="large"
    >
      <h2 id="genturis-section-governance-title">
        The Registry Steering Committee
      </h2>
      <p>
        The GENTURIS registry is embedded in the organisational structure of the
        European Reference Network for all patients with one of the genetic
        tumour risk syndromes (ERN GENTURIS).
      </p>
      <p>
        The GENTURIS registry steering committee is responsible for the daily
        management of the GENTURIS registry. The GENTURIS registry coordinator
        chairs the GENTURIS registry steering committee and the coordinator of
        ERN GENTURIS governs the GENTURIS registry.
      </p>
      <p>
        The GENTURIS registry steering committee consists of the following
        members.
      </p>
      <ul>
        <li>The coordinator of ERN GENTURIS</li>
        <li>The GENTURIS registry coordinator</li>
        <li>The GENTURIS registry data steward</li>
        <li>A representative of Task Force 5 Research</li>
        <li>A representative of software provider Molgenis</li>
        <li>
          An ERN GENTURIS European Patient Advocacy Group (ePAG) council
          representative
        </li>
      </ul>
    </PageSection>
    <PageSection
      id="genturis-section-support"
      aria-labelledby="genturis-section-support-title"
      :verticalPadding="1"
      width="large"
    >
      <h2 id="genturis-section-support-title">Support Team</h2>
      <p>
        The software underlying the GENTURIS registry is developed by the
        Molgenis team. The Molgenis team consist of software developers who
        implement the software underlying the GENTURIS registry and data
        managers who support the users of the GENTURIS registry.
      </p>
    </PageSection>
    <PageSection
      id="genturis-section-management"
      aria-labelledby="genturis-section-management-title"
      :verticalPadding="1"
      width="large"
    >
      <h2 id="genturis-section-management-title">General Management</h2>
      <p>
        ERN GENTURIS Task Force 5 Research is closely involved in the
        development of the GENTURIS registry. They are the general management
        and sounding board of the GENTURIS registry steering committee.
      </p>
    </PageSection>
    <PageSection
      id="genturis-section-dac"
      aria-labelledby="genturis-section-dac-title"
      :verticalPadding="1"
      width="large"
    >
      <h2 id="genturis-section-dac-title">Data Access Committee</h2>
      <p>
        The Data Access Committee (DAC) is closely involved in the procedure of
        requesting access to the GENTURIS registry data. The DAC reviews data
        requests and access and advises the GENTURIS registry steering committee
        as stipulated in the GENTURIS registry Data Access Policy (DAP). The DAC
        is composed of qualified health professionals and a patient
        representative. One or more of these committee members has also relevant
        legal and ethical expertise.
      </p>
      <p>The DAC consists of:</p>
      <ul>
        <li>The GENTURIS registry coordinator</li>
        <li>A representative of Task Force 5 research</li>
        <li>A representative of ERN GENTURIS Thematic Group 1</li>
        <li>A representative of ERN GENTURIS Thematic Group 2</li>
        <li>A representative of ERN GENTURIS Thematic Group 3</li>
        <li>A representative of ERN GENTURIS Thematic Group 4</li>
        <li>
          An ERN GENTURIS European Patient Advocacy Group (ePAG) council
          representative
        </li>
      </ul>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
</script>
