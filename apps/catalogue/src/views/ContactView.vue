<template>
  <div class="container bg-white">
    <ResourceHeader
      header-css="bg-info text-white"
      table-name="Contacts"
      :resource="contact"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Contributions:</h6>
        <div v-for="c in contact.contributedTo" :key="c.acronym">
          <RouterLink
            :to="{
              name: routename(c.resource.mg_tableclass),
              params: { acronym: c.resource.acronym },
            }"
          >
            {{ c.resource.acronym }}
            <OntologyTerms :terms="c.contributionType" />
          </RouterLink>
          <p>{{ c.contributionDescription }}</p>
        </div>
      </div>
      <div class="col">
        <h6>Institution</h6>
        <InstitutionList :institutions="contact.institution" />
        <h6>More information</h6>
        <ul>
          <li v-if="contact.email">
            <a :href="'mailto:' + contact.email"> email </a>
          </li>
          <li v-if="contact.homepage">
            <a :href="contact.homepage"> homepage </a>
          </li>
          <li v-if="contact.orcid">
            <a :href="'https://orcid.org/' + contact.orcid"> orcid </a>
          </li>
          <li v-if="contact.researchgate">
            <a
              :href="
                'https://www.researchgate.net/profile/' + contact.researchgate
              "
            >
              researchgate
            </a>
          </li>
          <li v-if="contact.linkedin">
            <a :href="'http://linkedin.com/in/' + contact.linkedin">
              linkedin
            </a>
          </li>
          <li v-if="contact.twitter">
            <a :href="'http://twitter.com/' + contact.twitter"> twitter </a>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError } from "@mswertz/emx2-styleguide";
import ResourceHeader from "../components/ResourceHeader";
import InstitutionList from "../components/InstitutionList";
import PartnerInList from "../components/PartnerInList";
import Property from "../components/Property";
import OntologyTerms from "../components/OntologyTerms";

export default {
  components: {
    OntologyTerms,
    Property,
    PartnerInList,
    InstitutionList,
    ResourceHeader,
    MessageError,
  },
  props: {
    //abusing this parameter, using 'name'
    name: String,
  },
  data() {
    return {
      graphqlError: null,
      contact: {},
    };
  },
  methods: {
    routename(tableName) {
      return tableName.split(".")[1].toLowerCase().slice(0, -1);
    },
    reload() {
      request(
        "graphql",
        `query Contacts($name:String){Contacts(filter:{name:{equals:[$name]}}){name,homepage,contributedTo{resource{acronym,name,mg_tableclass},contributionType{name},contributionDescription},institution{name,acronym},orcid,email,linkedin,researchgate,twitter}}`,
        {
          name: this.name,
        }
      )
        .then((data) => {
          this.contact = data.Contacts[0];
        })
        .catch((error) => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    acronym() {
      this.reload();
    },
  },
};
</script>
