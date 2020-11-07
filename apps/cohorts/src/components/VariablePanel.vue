<template>
  <LayoutCard :title="variable.name">
    <dl class="row">
      <dt class="col-3">Name</dt>
      <dd class="col-9">{{ variable.name }}</dd>
      <dt class="col-3">Table</dt>
      <dd class="col-9">
        <span v-if="variable.table">{{ variable.table.name }}</span>
      </dd>
      <dt class="col-3">Format</dt>
      <dd class="col-9">
        <span v-if="variable.format">{{ variable.format.name }}</span>
      </dd>
      <dt class="col-3">Unit</dt>
      <dd class="col-9">
        <span v-if="variable.unit">{{ variable.unit.name }}</span>
      </dd>
      <dt class="col-3">Codelist</dt>
      <dd class="col-9">
        <span v-if="variable.codeList"
          >{{ variable.codeList.name }}(
          {{
            variable.codeList.codes
              .map(c => c.codeLabel + "=" + c.codeValue)
              .join(", ")
          }})</span
        >
      </dd>
      <dt class="col-3">Description</dt>
      <dd class="col-9">
        {{ variable.description }}
      </dd>
      <dt class="col-3">Harmonisations</dt>
      <dd class="col-9" v-if="variable.harmonisations">
        {{
          variable.harmonisations.map(h => h.sourceCollection.name).join(", ")
        }}
      </dd>
    </dl>
  </LayoutCard>
</template>

<script>
import {LayoutCard} from "@mswertz/emx2-styleguide";

export default {
  props: {
    variable: Object
  },
  components: { LayoutCard }
};
</script>
