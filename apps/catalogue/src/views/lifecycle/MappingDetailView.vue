<template>
  <div v-if="mapping">
  <b-card :title="mapping.variable.name" :sub-title="mapping.variable.description">
    <div v-if="mapping.sources">
      <h6 class="pt-2">Source variables</h6>
      <b-list-group>
        <b-list-group-item
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
        </b-list-group-item>
      </b-list-group>
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
  </b-card>
  </div>
  <div v-else style="margin:10rem">
    <p>There is no mapping specified</p>
  </div>
</template>

<script>

import { BListGroupItem, BCard, BButton } from 'bootstrap-vue'
import 'highlight.js/styles/atom-one-light.css'

export default {
  name: 'MappingDetailView',
  props: ['mapping'],
  components: {
    BListGroupItem,
    BButton,
    BCard
  }
}
</script>