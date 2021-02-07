<template>
  <div class="col-sm-3 py-2">
    <div class="card">
      <div class="card-body">
        <h4 class="card-title">
          <RouterLink :to="'/institutions/' + institution.acronym">
            <small>
              {{ institution.acronym }}
            </small>
            <br />
            {{ institution.name }}
          </RouterLink>
        </h4>
        <span v-if="institution.website">
          <label>website: </label>
          <a :href="institution.website">{{ institution.website }}</a>
        </span>
        <ReadMore
          :text="institution.description"
          length="200"
          v-if="institution.description"
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
    institution: Object,
  },
};
</script>
