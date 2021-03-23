<template>
  <div v-if="schema.tables">
    <ButtonAlt
      @click="imgFullscreen = !imgFullscreen"
    >
      {{ imgFullscreen ? "show small size" : "show full size" }}
    </ButtonAlt>
    <InputCheckbox
      v-model="showAttributes"
      :default-value="showAttributes"
      :options="['attributes', 'external']"
    />
    <div
      v-scroll-lock="imgFullscreen"
      style=" overflow: scroll;text-align: center;"
      :style="{
        height: imgFullscreen ? 'auto' : '300px',
      }"
    >
      <Spinner v-if="loadingYuml" />
      <img
        v-else
        :key="JSON.stringify(showAttributes)"
        alt="Small"
        :src="yuml"
        style="max-height: 100%;"
        @load="loadingYuml = false"
      >
    </div>
  </div>
</template>

<script>
import {ButtonAlt, InputCheckbox, Spinner} from '@components/ui/index.js'

export default {
  components: {
    ButtonAlt,
    InputCheckbox,
    Spinner,
  },
  props: {
    schema: Object,
  },
  data() {
    return {
      imgFullscreen: false,
      loadingYuml: false,
      showAttributes: [],
    }
  },
  computed: {
    tables() {
      return this.schema.tables
    },
    yuml() {
      // eslint-disable-next-line vue/no-side-effects-in-computed-properties
      this.loadingYuml = true
      if (!this.tables) return ''
      let res = 'https://yuml.me/diagram/plain;dir:bt/class/'
      // classes
      this.tables
        .filter(
          (t) => !t.externalSchema || this.showAttributes.includes('external'),
        )
        .forEach((table) => {
          res += `[${table.name}`

          if (
            Array.isArray(table.columns) &&
            this.showAttributes.includes('attributes')
          ) {
            res += '|'
            table.columns
              .filter((column) => !column.inherited)
              .forEach((column) => {
                if (column.columnType.includes('REF')) {
                  res += `${column.name}:${column.refTable}`
                } else {
                  res += `${column.name}:${column.columnType}`
                }
                res += `［${column.nullable ? '0' : '1'}..${
                  column.columnType.includes('ARRAY') ? '*' : '1'
                }］;` // notice I use not standard [] to not break yuml
              })
          }
          if (table.externalSchema) {
            res += '],'
          } else {
            res += '{bg:dodgerblue}],'
          }
        })

      // relations
      this.tables
        .filter(
          (t) =>
            t.externalSchema == undefined ||
            this.showAttributes.includes('external'),
        )
        .forEach((table) => {
          if (table.inherit) {
            res += `[${table.inherit}]^-[${table.name}],`
          }
          if (Array.isArray(table.columns)) {
            table.columns
              .filter(
                (c) =>
                  !c.inherited &&
                  (c.refSchema == undefined ||
                    this.showAttributes.includes('external')),
              )
              .forEach((column) => {
                if (column.columnType === 'REF') {
                  res += `[${table.name}]${column.name}->[${column.refTable}],`
                } else if (column.columnType === 'REF_ARRAY') {
                  res += `[${table.name}]${column.name}-*>[${column.refTable}],`
                }
              })
          }
        })

      return res
    },
  },
}
</script>
