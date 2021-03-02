<template>
  <div>
    <h6>Provider</h6>
    <InstitutionList :institutions="resource.provider" />
    <h6>Contact</h6>
    <ContactList :contacts="resource.contact" />
    <h6>Data releases</h6>
    <ReleasesList :releases="resource.releases" />
    <h6>Documentation</h6>
    <DocumentationList :documentation="resource.documentation" />
    <h6>Networks</h6>
    <NetworkList :networks="resource.networks" />
    <h6>Partners</h6>
    <PartnersList :partners="resource.partners" />
    <h6>Contributors</h6>
    <ContributorList :contributors="resource.contributors" color="danger" />
    <h6>Publications</h6>
    <PublicationList :publications="resource.publications" />
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
