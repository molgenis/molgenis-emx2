<template>
  <Page id="page-documents">
    <CustomPageHeader
      class="erras-header"
      title="ERN-SKIN Registry"
      subtitle="Studies"
      height="xlarge"
      title-position-x="center"
      title-position-y="center"
    />
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="1"
    >
      <h3 id="section-documents-title"></h3>
      <p>
      <span style="font-size: 1em;">The transparency portal, as defined by the CNIL, allows you to be informed of research projects that utilize information gathered within the framework of ERRAS: the European Registry for RAre Skin diseases. <br>
                                    These projects are essential to foster research on rare skin diseases, to improve the lives of patients, the knowledge and the treatment of their diseases. <br>
                                    Each project is summarized in accordance with the General Data Protection Regulation (GDPR) as defined in Articles 12 and following: </span><br>
        <span style="font-size: 0.8em; margin-left: 40px; display: inline-block;">“The controller shall take appropriate measures to provide any information referred to in Articles 13 and 14 and any communication under Articles 15 to 22 and 34 relating to processing to the data subject in a concise, transparent, intelligible and easily accessible form, using clear and plain language, in particular for any information addressed specifically to a child. The information shall be provided in writing, or by other means, including, where appropriate, by electronic means. When requested by the data subject, the information may be provided orally, provided that the identity of the data subject is proven by other means.”</span><br>
        <span style="font-size: 1em;">Each summary allows you to understand the objectives pursued by the research, the population involved, and the data used.</span>
      </p>
      <Accordion id="studies-nav" title="Studies" :isOpenByDefault="true">
        <Studies table="Studies" labelsColumn="title" />
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
