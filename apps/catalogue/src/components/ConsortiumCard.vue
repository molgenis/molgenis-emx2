<template>
  <div class="col-sm-4 py-2">
    <div class="card h-100 overflow-auto">
      <span>
        <h4 class="card-title">
          <RouterLink :to="'/consortia/' + consortium.acronym">
            <small>
              {{ consortium.acronym }}
            </small>
            <br />
            {{ consortium.name }}
          </RouterLink>
        </h4>
        <span v-if="consortium.host_organisation">
          <label>by:</label> {{ consortium.host_organisation.name }}<br />
        </span>
        <span v-if="consortium.website">
          <label>website:</label>&nbsp;
          <a :href="consortium.website">{{ consortium.website }}</a>
        </span>
      </span>
      <ReadMore
        :text="consortium.description"
        :length="200"
        v-if="consortium.description"
      />
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    consortium: Object,
  },
};
</script>
