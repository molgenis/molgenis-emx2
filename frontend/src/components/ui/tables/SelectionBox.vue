<template>
  <div>
    <ButtonAlt @click="toggle">
      Selection
      <i class="fa fa-lg fa-check-square-o" />
      <span
        v-if="selection && selection.filter((s) => s != null).length > 0"
        class="badge badge-pill badge-danger"
        style=" left: -7px;top: -7px;"
      >
        {{
          Array.isArray(selection)
            ? selection.filter((s) => s != null).length
            : 0
        }}
      </span>
    </ButtonAlt>
    <LayoutModal v-if="expand" title="Show selection" @close="expand = false">
      <template #body>
        <span
          v-if="
            !Array.isArray(selection) ||
              selection.filter((s) => s != null).length == 0
          "
        >
          No items selected
        </span>
        <span
          v-for="(item, idx) in selection.filter((s) => s != null)"
          v-else
          :key="JSON.stringify(item)"
          class="btn-outline-primary btn-sm mr-2"
        >
          {{ flattenObject(item) }}
          <IconAction icon="times" @click="deselect(idx)" />
        </span>
      </template>
      <template #footer>
        <ButtonAlt
          v-if="Array.isArray(selection) && selection.length > 0"
          @click="clear"
        >
          clear selection
        </ButtonAlt>
      </template>
    </LayoutModal>
  </div>
</template>

<script>
import IconAction from '../forms/IconAction.vue'
import ButtonAlt from '../forms/ButtonAlt.vue'
import LayoutModal from '../layout/LayoutModal.vue'

export default {
  components: {
    IconAction,
    ButtonAlt,
    LayoutModal,
  },
  props: {
    selection: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      expand: false,
    }
  },
  methods: {
    deselect(idx) {
      let update = this.selection
      update.splice(idx, 1)
      console.log('update:selection ' + JSON.stringify(update))
      this.$emit('update:selection', update)
    },
    clear() {
      let update = this.selection
      update.splice(0, update.length)
      console.log('update:selection ' + JSON.stringify(update))
      this.$emit('update:selection', update)
    },
    toggle() {
      this.expand = !this.expand
    },
    flattenObject(object) {
      let result = ''
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          // nothing
        } else if (typeof object[key] === 'object') {
          result += this.flattenObject(object[key])
        } else {
          result += '.' + object[key]
        }
      })
      return result.replace(/^\./, '')
    },
  },
}
</script>
