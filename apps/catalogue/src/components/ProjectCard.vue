<template>
  <div class="col-sm-4 py-2">
    <div class="card h-100 overflow-auto">
      <span>
        <h4 class="card-title">
          <RouterLink :to="'/projects/' + project.acronym">
            <small>
              {{ project.acronym }}
            </small>
            <br />
            {{ project.name }}
          </RouterLink>
        </h4>
        <span v-if="project.institution">
          <label>by:</label> {{ project.institution.name }}<br />
        </span>
        <span v-if="project.website">
          <label>website:</label>&nbsp;
          <a :href="project.website">{{ project.website }}</a>
        </span>
      </span>
      <ReadMore
        :text="project.description"
        :length="200"
        v-if="project.description"
      />
    </div>
  </div>
</template>

<script>
import { ReadMore } from "@mswertz/emx2-styleguide";

export default {
  components: { ReadMore },
  props: {
    project: Object,
  },
};
</script>
