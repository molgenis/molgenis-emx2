<template>
  <span v-if="harmonization">
  <b-card :title="harmonization.variable.name" :sub-title="harmonization.variable.description">
    <div v-if="harmonization.sources">
      <h6 class="pt-2">Source variables</h6>
      <b-list-group>
        <b-list-group-item
          v-for="(source, index) in harmonization.sources"
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
    <div v-if="harmonization.syntax">
      <h6 class="pt-2">Syntax</h6>
      <p>
      <highlightjs
        autodetect
        :code="harmonization.syntax"
      />
      </p>
    </div>
  </b-card>
  </span>
  <span v-else style="margin:10rem">
    <p>There is no harmonization specified</p>
  </span>
</template>

<script>

import { BListGroupItem, BCard } from 'bootstrap-vue'
import 'highlight.js/styles/atom-one-light.css'
import Spinner from '../../../../styleguide/src/layout/Spinner.vue'

export default {
  name: 'HarmonizationDetailsView',
  props: ['harmonization'],
  components: {
    BListGroupItem,
    BCard,
    Spinner
  }
}
</script>