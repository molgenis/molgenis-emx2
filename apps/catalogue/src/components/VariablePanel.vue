<template>
  <div class="card">
    <div class="card-body">
      <div class="card-title">
        <p
          class="card-title text-primary"
          style="text-align: left"
          @click="collapsed = !collapsed"
        >
          {{ variable.collection.name }}
          /
          {{ variable.table.name }}
          / {{ variable.name }}
        </p>
        <span
          v-if="variable.topics"
          v-for="topic in variable.topics"
          class="badge badge-primary"
        >
          topic: {{ topic.name }}
        </span>
        <span v-if="variable.mandatory" class="badge badge-danger ml-2">
          mandatory
        </span>
        <span v-if="variable.format" class="badge badge-warning ml-2">
          format: {{ variable.format.name }}
        </span>
        <span v-if="variable.unit" class="badge badge-info ml-2">
          unit: {{ variable.unit.name }}
        </span>
      </div>
      <small>
        <p class="card-text">
          <i> {{ variable.description }} </i>
        </p>
        <dl class="row" v-if="!collapsed">
          <dt class="col-3">Variable Name</dt>
          <dd class="col-9">{{ variable.name }}</dd>
          <dt class="col-3">Variable Table</dt>
          <dd class="col-9">
            <span v-if="variable.dataset">{{ variable.dataset.name }}</span>
          </dd>
          <dt class="col-3">Variable Collection</dt>
          <dd class="col-9">
            <span v-if="variable.collection">{{
              variable.collection.name
            }}</span>
          </dd>
          <dt class="col-3" v-if="variable.format">Format</dt>
          <dd class="col-9" v-if="variable.format">
            <span>{{ variable.format.name }}</span>
          </dd>
          <dt class="col-3" v-if="variable.unit">Unit</dt>
          <dd class="col-9" v-if="variable.unit">
            <span>{{ variable.unit.name }}</span>
          </dd>
          <dt class="col-3" v-if="variable.valueLabels">Value Labels</dt>
          <dd class="col-9" v-if="variable.valueLabels">
            <div v-for="val in variable.valueLabels">{{ val }}</div>
          </dd>
          <dt class="col-3" v-if="variable.missingValues">Missing Values</dt>
          <dd class="col-9" v-if="variable.missingValues">
            <div v-for="val in variable.missingValues">{{ val }}</div>
          </dd>
          <dt class="col-3" v-if="variable.codeList">Codelist</dt>
          <dd class="col-9" v-if="variable.codeList">
            <span>
              {{ variable.codeList.name }}(
              {{
                variable.codeList.codes
                  .map((c) => c.label + "=" + c.value)
                  .join(", ")
              }})</span
            >
          </dd>
          <dt class="col-3" v-if="variable.description">Description</dt>
          <dd class="col-9" v-if="variable.description">
            {{ variable.description }}
          </dd>
          <dt class="col-3" v-if="variable.harmonisations">Harmonisations</dt>
          <dd class="col-9" v-if="variable.harmonisations">
            {{
              variable.harmonisations
                .map((h) => h.sourceDataset.collection.name)
                .join(", ")
            }}
          </dd>
          <dt class="col-3" v-if="variable.topics">Topics</dt>
          <dd class="col-9" v-if="variable.topics">
            <span> {{ variable.topics.map((c) => c.name).join(", ") }}</span>
          </dd>
        </dl>
      </small>
    </div>
  </div>
</template>

<style>
.text-centered h4 {
  text-align: left;
}
</style>

<script>
import { ButtonAlt } from "@mswertz/emx2-styleguide";

export default {
  props: {
    variable: Object,
  },
  data() {
    return {
      collapsed: true,
    };
  },
  components: { ButtonAlt },
};
</script>
