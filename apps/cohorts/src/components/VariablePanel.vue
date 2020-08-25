<template>
  <LayoutCard :title="variable.name">
    <dl class="row">
      <dt class="col-3">Name</dt>
      <dd class="col-9">{{ variable.name }}</dd>
      <dt class="col-3">Table</dt>
      <dd class="col-9">
        <span v-if="variable.table">{{ variable.table.name }}</span>
      </dd>
      <dt class="col-3">Type</dt>
      <dd class="col-9">
        <span v-if="variable.type">{{ variable.type.name }}</span>
      </dd>
      <dt class="col-3">Unit</dt>
      <dd class="col-9">
        <span v-if="variable.unit">{{ variable.unit.name }}</span>
      </dd>
      <dt class="col-3">Description</dt>
      <dd class="col-9">
        {{ variable.description }}
      </dd>
      <dt class="col-3">Harmonisations</dt>
      <dd class="col-9"></dd>
    </dl>
  </LayoutCard>
</template>

<script>
import { LayoutCard } from "@mswertz/emx2-styleguide";

export default {
  props: {
    variable: Object
  },
  components: { LayoutCard }
};
</script>
