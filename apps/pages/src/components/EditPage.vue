<template>
  <div v-if="canEdit && getContent">
    <router-link :to="'/' + page">view page</router-link>
    <div v-if="!content?.modules">
      select page layout
      <button
        @click="
          content = {
            version: 2,
            modules: [{ type: 'Html', html: '<h1>html</h1>' }],
          }
        "
      >
        html page
      </button>
      <button
        @click="
          content = {
            version: 2,
            modules: [
              {
                type: 'Header',
                title: 'ERN Genturis Registry',
                subtitle: 'Registry for Genetic Tumour Risk Syndromes',
              },
              {
                type: 'Section',
                title: 'Welcome to the GENTURIS registry',
                html: '<p>The <strong>GENTURIS</strong> registry is the European registry for patients with one of the genetic tumour risk syndromes (genturis). The registry is affiliated to the European Reference Network for all patients with one of the genetic tumour risk syndromes (ERN GENTURIS) </p>',
              },
              { type: 'Section' },
            ],
          }
        "
      >
        ern frontpage
      </button>
      <button
        @click="
          content = {
            version: 2,
            modules: [{ type: 'Header' }, { type: 'PieChart' }],
          }
        "
      >
        simple dashboard
      </button>
      <button>contact page</button>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";

export default {
  components: {
    ModularPage,
  },
  props: {
    page: String,
    session: Object,
  },
  data() {
    return {
      content: this.getContent,
    };
  },
  computed: {
    getContent() {
      console.log(this?.session);
      if (this?.session && this?.session?.settings) {
        if (this?.session?.settings["page." + this.page].version == 2) {
          return this?.session?.settings["page." + this.page];
        }
        return {
          version: 2,
        };
      } else {
        return {
          version: 2,
        };
      }
    },
    canEdit() {
      /*
      return (
        this.session &&
        (this.session.email == "admin" ||
          (this.session.roles && this.session.roles.includes("Manager")))
      );
      */
      return true;
    },
  },
  methods: {
    savePage(value) {
      console.log("savePage", value);
      this.loading = true;
      this.graphqlError = null;
      this.success = null;
      request(
        "graphql",
        `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}`,
        {
          settings: {
            key: "page." + this.page,
            value: JSON.stringify(value),
          },
        }
      )
        .then((data) => {
          this.success = data.change.message;
          this.session.settings["page." + this.page] = value;
          //          this.content = value;
        })
        .catch((graphqlError) => {
          console.log(graphqlError);
          //          this.graphqlError = graphqlError.response.errors[0].message;
        })
        .finally((this.loading = false));
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        this.content = this.getContent;
        console.log("handler", this.content);
      },
    },
  },
};
</script>
