<template>
  <Page id="page-documents">
    <CustomPageHeader
      class="erras-header"
      title="ERN-Skin Registry"
      subtitle="Additional Information"
      imageSrc="img/erras-header.jpg"
      height="xlarge"
      title-position-x="center"
      title-position-y="center"
    />
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="2"
    >
      <h2 id="section-documents-title"></h2>
      <Accordion id="studies-nav" title="Studies" :isOpenByDefault="false">
        <Studies table="Studies" labelsColumn="title" />
      </Accordion>
      <Accordion
        id="accReq-nav"
        title="Access Requests"
        :isOpenByDefault="false"
      >
        <FileList
         table="Files"
         filter='(filter: { tags: { not_equals: "private" } })'
         labelsColumn="name"
         fileColumn="file" />
      </Accordion>
      <Accordion
        id="patient-nav"
        title="Are you a patient?"
        :isOpenByDefault="false"
        :headingStyle="3"
      >
        <ul>
          <li>
            Informed consent: please check with your treating doctor who will
            provide you with all explanations and documents in local language.
          </li>
          <li>
            Withdrawal of consent: please check with your treating doctor who
            will provide you with the information for the withdrawal of your
            consent.
          </li>
          <li>Contact point: skin-registry@ern-skin.eu</li>
        </ul>
      </Accordion>
      <Accordion
        id="general-nav"
        title="General Documents"
        :isOpenByDefault="false"
      >
        <PrivateFiles/>
        <strong>Publications</strong>
        <Publications
          table="Publications"
          labelsColumn="title"
          doiColumn="doi"
        />
      </Accordion>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
// @ts-ignore
import { Accordion, Page, PageSection, FileList, MessageBox } from "molgenis-viz";
import CustomPageHeader from "../components/CustomPageHeader.vue";
import PrivateFiles from "../components/PrivateFiles.vue";
import Publications from "../components/Publications.vue";
import Studies from "../components/Studies.vue";
</script>

<style lang="scss">
#accordion-studies-nav {
  .accordion-heading {
    background-color: #0084b4;
    border-color: #0084b4;
    color: #ffffff;
  }
}

#accordion-accReq-nav {
  .accordion-heading {
    background-color: #00a453;
    border-color: #00a453;
    color: #ffffff;
  }
}

#accordion-patient-nav {
  .accordion-heading {
    background-color: #f48b31;
    border-color: #f48b31;
    color: #ffffff;
  }
}

#accordion-general-nav {
  .accordion-heading {
    background-color: #eb212e;
    border-color: #eb212e;
    color: #ffffff;
  }
}
</style>
