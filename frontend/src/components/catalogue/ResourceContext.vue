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
    <ContributorList color="danger" :contributors="resource.contributors" />
    <h6>Publications</h6>
    <PublicationList :publications="resource.publications" />
  </div>
</template>

<script>
import ContactList from './ContactList.vue'
import ContributorList from './ContributorList.vue'
import DocumentationList from './DocumentationList.vue'
import InstitutionList from './InstitutionList.vue'
import NetworkList from './NetworkList.vue'
import PartnersList from './PartnersList.vue'
import PublicationList from './PublicationList.vue'
import ReleasesList from './ReleasesList.vue'

export default {
  components: {
    ContactList,
    ContributorList,
    DocumentationList,
    InstitutionList,
    NetworkList,
    PartnersList,
    PublicationList,
    ReleasesList,
  },
  props: {
    resource: Object,
  },
}
</script>
