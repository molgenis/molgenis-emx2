<script setup lang="ts">
// @ts-ignore
import { Accordion, Page, PageHeader, PageSection } from "molgenis-viz";
import Studies from "../components/Studies.vue";
</script>

<template>
  <Page id="page-studies">
    <PageHeader
      id="ernSkinStudies"
      title="ERN-SKIN Registry"
      subtitle="STUDIES"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="1"
    >
      <p>
        The transparency portal, as defined by the CNIL, allows you to be
        informed of research projects that utilize information gathered within
        the framework of ERRAS: the European Registry for RAre Skin diseases.
        These projects are essential to foster research on rare skin diseases,
        to improve the lives of patients, the knowledge and the treatment of
        their diseases. Each project is summarized in accordance with the
        General Data Protection Regulation (GDPR) as defined in Articles 12 and
        following:
      </p>
      <blockquote>
        “The controller shall take appropriate measures to provide any
        information referred to in Articles 13 and 14 and any communication
        under Articles 15 to 22 and 34 relating to processing to the data
        subject in a concise, transparent, intelligible and easily accessible
        form, using clear and plain language, in particular for any information
        addressed specifically to a child. The information shall be provided in
        writing, or by other means, including, where appropriate, by electronic
        means. When requested by the data subject, the information may be
        provided orally, provided that the identity of the data subject is
        proven by other means.”
      </blockquote>
      <p>
        Each summary allows you to understand the objectives pursued by the
        research, the population involved, and the data used.
      </p>
      <Accordion id="studies-nav" title="Studies" :isOpenByDefault="true">
        <Studies table="Studies" labelsColumn="title" />
      </Accordion>
    </PageSection>
  </Page>
</template>
