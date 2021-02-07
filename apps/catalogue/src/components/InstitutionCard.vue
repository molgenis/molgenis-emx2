<template>
  <div class="col-sm-3 py-2">
    <div class="card">
      <div class="card-body">
        <h4 class="card-title">
          <RouterLink :to="'/Institutes/' + Institute.acronym">
            <small>
              {{ Institute.acronym }}
            </small>
            <br />
            {{ Institute.name }}
          </RouterLink>
        </h4>
        <span v-if="Institute.website">
          <label>website: </label>
          <a :href="Institute.website">{{ Institute.website }}</a>
        </span>
        <ReadMore
          :text="Institute.description"
          length="200"
          v-if="Institute.description"
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
    Institute: Object,
  },
};
</script>
