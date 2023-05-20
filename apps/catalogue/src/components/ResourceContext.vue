<template>
  <div>
    <h6>Lead institution</h6>
    <InstitutionList :institutions="resource.institution" />
    <h6>Lead contact</h6>
    <ContactList :contacts="resource.contact" />
    <h6 v-if="resource.partners">Additional institutions</h6>
    <PartnersList v-if="resource.partners" :partners="resource.partners" />
    <h6>Protocols</h6>
    <DocumentationList :documentation="resource.documentation" />
    <h6 v-if="resource.models">Common Data Models</h6>
    <ModelsList v-if="resource.models" :models="resource.models" />
    <h6 v-if="resource.releases">Releases</h6>
    <ReleasesList v-if="resource.releases" :releases="resource.releases" />
    <h6 v-if="resource.contributors">Contributors</h6>
    <ContributorList
      v-if="resource.contributors"
      :contributors="resource.contributors"
      color="danger"
    />
    <h6 v-if="resource.publications">Publications</h6>
    <PublicationList
      v-if="resource.publications"
      :publications="resource.publications"
    />
  </div>
</template>
<script>
import InstitutionList from "./InstitutionList.vue";
import ContactList from "./ContactList.vue";
import ReleasesList from "./ReleasesList.vue";
import DocumentationList from "./DocumentationList.vue";
import PublicationList from "./PublicationList.vue";
import ContributorList from "./ContributorList.vue";
import PartnersList from "./PartnersList.vue";
import ModelsList from "./ModelsList.vue";

export default {
  components: {
    PartnersList,
    InstitutionList,
    ContactList,
    ReleasesList,
    DocumentationList,
    PublicationList,
    ContributorList,
    ModelsList,
  },
  props: {
    resource: Object,
  },
};
</script>
