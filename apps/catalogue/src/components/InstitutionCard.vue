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
        <p v-if="institution.website">
          <a :href="institution.website">{{ institution.website }}</a>
        </p>
        <p>{{ institution.type ? institution.type.name : "" }}</p>
        <p>{{ institution.country ? institution.country.name : "" }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";
import Property from "./Property";

export default {
  components: { ReadMore, Property },
  props: {
    institution: Object,
  },
};
</script>
