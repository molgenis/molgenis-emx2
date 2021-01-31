<template>
  <div class="col-sm-3 py-2">
    <div class="card">
      <div class="card-body">
        <h4 class="card-title">
          <RouterLink
            :to="{
              name: databankview,
              params: {
                databankAcronym: databank.acronym,
                providerAcronym: providerAcronym,
              },
            }"
          >
            <small class="float-right">
              <span
                v-if="databank.type"
                v-for="type in databank.type"
                class="badge badge-primary"
              >
                {{ type.name }}
              </span>
            </small>
            <small>
              {{ databank.acronym }}
            </small>
            <br />
            {{ databank.name }}
          </RouterLink>
        </h4>
        <span v-if="databank.provider">
          <label>Provider:</label> {{ databank.provider.name }}<br />
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
        <div v-if="tab === 'Variables'">
          <ul>
            <div v-for="table in databank.tables">
              {{ table.name }}
              <ul>
                <li v-for="variable in table.variables">
                  {{ variable.name }}
                </li>
              </ul>
            </div>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    databank: Object,
    providerAcronym: String,
  },
  computed: {
    databankview() {
      if (this.providerAcronym) {
        return "provider-databank";
      }
      return "databank";
    },
  },
};
</script>
