<template>
  <div @keyup.ctrl.f="format">
    <div ref="filter-editor" class="filter-editor" @keyup="dirty = true"></div>

    <button class="btn btn-info mt-3" :disabled="!dirty" @click="apply">
      Apply changes to {{ value.label }}
    </button>
    <button
      class="btn btn-danger ml-3 mt-3"
      @click="deleteFilter">
      Delete {{ value.label }}
    </button>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: Object,
      required: true
    }
  },
  methods: {
    format () {
      if (this.filterEditor && this.filterEditor.getAction) {
        this.filterEditor.getAction('editor.action.formatDocument').run()
      }
    },
    apply () {
      this.$emit('input', JSON.parse(this.filterEditor.getValue()))
    },
    deleteFilter () {
      this.$emit('delete')
    }
  },
  data () {
    return {
      dirty: false,
      filterEditor: {}
    }
  },
  destroyed () {
    this.dirty = false
    this.filterEditor.dispose()
  },
  watch: {
    value (newValue) {
      this.dirty = false
      if (this.filterEditor.dispose) {
        this.filterEditor.getModel().setValue(JSON.stringify(newValue))
        this.format()
      }
    }
  },
  async mounted () {
    const monaco = await import('monaco-editor/esm/vs/editor/editor.api')

    this.filterEditor = monaco.editor.create(this.$refs['filter-editor'], {
      automaticLayout: true,
      value: JSON.stringify(this.value),
      language: 'json'
    })

    const formatTimer = setTimeout(() => {
      this.format()
      clearTimeout(formatTimer)
    }, 500)
  }
}
</script>

<style scoped>
.filter-editor {
  height: 100%;
  width: 100%;
}
</style>
