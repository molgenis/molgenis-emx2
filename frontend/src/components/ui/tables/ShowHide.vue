<template>
  <ButtonDropdown :icon="icon" :label="label" v-slot="scope">
    <IconAction
      class="float-right"
      icon="times"
      style=" margin-right: -10px;margin-top: -10px;"
      @click="scope.close"
    />
    <div>
      <h6>
        {{ label }}
      </h6>
      <ButtonAlt @click="showAll">
        show all
      </ButtonAlt>
      <ButtonAlt @click="hideAll">
        hide all
      </ButtonAlt>

      <div>
        <div v-for="(col, key) in columns" :key="key" class="form-check">
          <input
            :id="col.name"
            :checked="
              col[checkAttribute] == undefined
                ? defaultValue
                : col[checkAttribute]
            "
            class="form-check-input"
            type="checkbox"
            @input.prevent="change(key, !col[checkAttribute])"
          >
          <label class="form-check-label" :for="col.name">
            {{ col.name }}
          </label>
        </div>
      </div>
    </div>
  </ButtonDropdown>
</template>

<script>
import ButtonAlt from '../forms/ButtonAlt.vue'
import ButtonDropdown from '../forms/ButtonDropdown.vue'
import IconAction from '../forms/IconAction.vue'

export default {
  components: {ButtonAlt, ButtonDropdown, IconAction},
  props: {
    checkAttribute: String,
    columns: Array,
    defaultValue: {type: Boolean, default: false},
    icon: String,
    label: String,
  },
  emits: ['update:columns'],
  methods: {
    change(key, value) {
      let update = JSON.parse(JSON.stringify(this.columns))
      update[key][this.checkAttribute] = value
      this.$emit('update:columns', update)
    },
    hideAll() {
      let update = JSON.parse(JSON.stringify(this.columns))
      for (var key in update) {
        update[key][this.checkAttribute] = false
      }
      this.$emit('update:columns', update)
    },
    showAll() {
      let update = JSON.parse(JSON.stringify(this.columns))
      for (var key in update) {
        update[key][this.checkAttribute] = true
      }
      this.$emit('update:columns', update)
    },
    value(col) {
      return col[this.checkAttribute] == undefined
        ? this.defaultValue
        : col[this.checkAttribute]
    },
  },
}
</script>
