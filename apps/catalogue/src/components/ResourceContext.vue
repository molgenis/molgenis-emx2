<template>
  <div>
    <h6>Lead institution</h6>
    <InstitutionList :institutions="resource.institution" />
    <h6>Lead contact</h6>
    <ContactList :contacts="resource.contact" />
    <h6>Additional institutions</h6>
    <PartnersList :partners="resource.partners" />
    <h6>Protocols</h6>
    <DocumentationList :documentation="resource.documentation" />
    <h6 v-if="resource.releases">Mappings to data models</h6>
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
import InstitutionList from "./InstitutionList";
import ContactList from "./ContactList";
import ReleasesList from "./ReleasesList";
import DocumentationList from "./DocumentationList";
import Conditions from "./Conditions";
import PublicationList from "./PublicationList";
import ContributorList from "./ContributorList";
import NetworkList from "./NetworkList";
import PartnersList from "./PartnersList";

export default {
  components: {
    PartnersList,
    NetworkList,
    InstitutionList,
    ContactList,
    ReleasesList,
    DocumentationList,
    Conditions,
    PublicationList,
    ContributorList,
  },
  props: {
    resource: Object,
  },
};
</script>
