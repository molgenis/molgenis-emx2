<template>
  <div class="card">
    <div class="card-body">
      <h4 class="card-title">
        <RouterLink
          :to="{
            name: datasourceview,
            params: {
              datasourceAcronym: datasource.acronym,
              institutionAcronym: institutionAcronym,
            },
          }"
        >
          <small class="float-right">
            <small>
              {{ datasource.acronym }}
            </small>
          </small>
          <span
            v-if="datasource.type"
            v-for="type in datasource.type"
            class="badge badge-primary"
          >
            {{ type.name }}
          </span>
          <br />
          {{ datasource.name }}
        </RouterLink>
      </h4>
      <span v-if="datasource.provider">
        <label>provider:</label> {{ datasource.provider.name }}<br />
      </span>
      <span v-if="datasource.website">
        <label>website: </label>
        <a :href="datasource.website">{{ datasource.website }}</a>
      </span>
      <ReadMore
        :text="datasource.description"
        :length="200"
        v-if="datasource.description"
      />
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    datasource: Object,
    institutionAcronym: String,
  },
  computed: {
    datasourceview() {
      if (this.institutionAcronym) {
        return "institution-datasource";
      }
      return "datasource";
    },
  },
};
</script>
