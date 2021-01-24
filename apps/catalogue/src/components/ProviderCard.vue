<template>
  <div class="col-sm-3 py-2">
    <div class="card">
      <div class="card-body">
        <h4 class="card-title">
          <RouterLink :to="'/providers/' + provider.acronym">
            <small>
              {{ provider.acronym }}
            </small>
            <br />
            {{ provider.name }}
          </RouterLink>
        </h4>
        <span v-if="provider.website">
          <label>website: </label>
          <a :href="provider.website">{{ provider.website }}</a>
        </span>
        <ReadMore
          :text="provider.description"
          length="200"
          v-if="provider.description"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    provider: Object,
  },
};
</script>
