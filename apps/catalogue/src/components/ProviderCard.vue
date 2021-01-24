<template>
  <div class="col-sm-3 py-2">
    <div class="card">
      <div class="card-body">
        <h4 class="card-title">
          <RouterLink :to="'/providers/' + organisation.acronym">
            <small>
              {{ organisation.acronym }}
            </small>
            <br />
            {{ organisation.name }}
          </RouterLink>
        </h4>
        <span v-if="organisation.website">
          <label>website: </label>
          <a href="organisation.website">{{ organisation.website }}</a>
        </span>
        <ReadMore
          :text="organisation.description"
          length="200"
          v-if="organisation.description"
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
    organisation: Object,
  },
};
</script>
