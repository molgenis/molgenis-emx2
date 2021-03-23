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
import ButtonAlt from '../forms/ButtonAlt.vue'
import IconAction from '../forms/IconAction.vue'
import LayoutModal from '../layout/LayoutModal.vue'

export default {
  components: {
    ButtonAlt,
    IconAction,
    LayoutModal,
  },
  props: {
    selection: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['update:selection'],
  data() {
    return {
      expand: false,
    }
  },
  methods: {
    clear() {
      let update = this.selection
      update.splice(0, update.length)
      this.$emit('update:selection', update)
    },
    deselect(idx) {
      let update = this.selection
      update.splice(idx, 1)
      this.$emit('update:selection', update)
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
    toggle() {
      this.expand = !this.expand
    },
  },
}
</script>
