<template>
  <div v-if="mapping">
  <div :title="mapping.variable.name" :sub-title="mapping.variable.description">
    <div v-if="mapping.sources">
      <h6 class="pt-2">Source variables</h6>
      <div>
        <div
          v-for="(source, index) in mapping.sources"
          :key="index"
          class="flex-column align-items-start"
        >
          <div class="d-flex w-100 justify-content-between">
            <p class="mb-1"><b>{{ source.name }}</b></p>
            <small>{{ source.name }}</small>
          </div>
          <p class="mb-1">
            {{ source.name }}
          </p>
        </div>
      </div>
    </div>
    <div v-if="mapping.syntax">
      <h6 class="pt-2">Syntax</h6>
      <p>
      <highlightjs
        autodetect
        :code="mapping.syntax"
      />
      </p>
    </div>
  </div>
  </div>
  <div v-else style="margin:10rem">
    <p>There is no mapping specified</p>
  </div>
</template>

<script>
import 'highlight.js/styles/atom-one-light.css'

export default {
  name: 'MappingDetailView',
  props: ['mapping']
}
</script>