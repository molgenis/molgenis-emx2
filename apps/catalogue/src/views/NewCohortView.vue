<template>
  <div v-if="cohort" class="container">
    <page-header-vue
      class="block"
      :title="cohort.name"
      :logoUrl="cohort.logo.url"
    ></page-header-vue>

    <links-block
    class="block"
      :items="[
        {
          label: 'Website',
          href: 'www.google.com',
        },
        {
          label: 'Contact',
          href: 'www.molgenis.org',
        },
      ]"
    ></links-block>
  </div>
</template>

<script>
import PageHeaderVue from "../components/blocks/PageHeader.vue";
import LinksBlock from "../components/blocks/LinksBlock.vue";
import { fetchById } from "../store/repository/cohortRepository";
export default {
  name: "CohortView",
  components: { PageHeaderVue, LinksBlock },
  data() {
    return {
      cohort: null,
    };
  },
  methods: {
    async fetchData() {
      this.cohort = await fetchById(this.$route.params.pid);
    },
  },
  mounted: async function () {
    this.fetchData();
  },
};
</script>

<style scoped>
.block {
  background-color: #e9ecef;
  margin-bottom: 1rem;
  padding: 1rem;
}
</style>
