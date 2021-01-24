<template>
  <div class="col-sm-4 py-2">
    <div class="card h-100 overflow-auto">
      <span>
        <h4 class="card-title">
          <RouterLink :to="'/network-details/' + network.acronym">
            <small>
              {{ network.acronym }}
            </small>
            <br />
            {{ network.name }}
          </RouterLink>
        </h4>
        <span v-if="network.host_organisation">
          <label>by:</label> {{ network.host_organisation.name }}<br />
        </span>
        <span v-if="network.website">
          <label>website:</label>&nbsp;
          <a href="network.website">{{ network.website }}</a>
        </span>
      </span>
      <ReadMore
        :text="network.description"
        length="200"
        v-if="network.description"
      />
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    network: Object,
  },
};
</script>
