<template>
  <div class="card">
    <div class="card-body">
      <h4 class="card-title">
        <RouterLink
          :to="{
            name: databankview,
            params: {
              databankAcronym: databank.acronym,
              institutionAcronym: institutionAcronym,
            },
          }"
        >
          <small class="float-right">
            <small>
              {{ databank.acronym }}
            </small>
          </small>
          <span
            v-if="databank.type"
            v-for="type in databank.type"
            class="badge badge-primary"
          >
            {{ type.name }}
          </span>
          <br />
          {{ databank.name }}
        </RouterLink>
      </h4>
      <span v-if="databank.institution">
        <label>institution:</label> {{ databank.institution.name }}<br />
      </span>
      <span v-if="databank.website">
        <label>website: </label>
        <a :href="databank.website">{{ databank.website }}</a>
      </span>
      <ReadMore
        :text="databank.description"
        :length="200"
        v-if="databank.description"
      />
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    databank: Object,
    institutionAcronym: String,
  },
  computed: {
    databankview() {
      if (this.institutionAcronym) {
        return "institution-databank";
      }
      return "databank";
    },
  },
};
</script>
