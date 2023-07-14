<template>
  <div class="container-fluid p-0">
    <div
      class="row px-5 pb-3">

      <div class="row w-100 mt-1">
        <div class="col-6 pr-0"><h3>Current config</h3></div>
        <div class="col-6 pl-0"><h3>New config</h3></div>
      </div>

      <div ref="diff-editor" class="editor"></div>
    </div>

    <div class="row px-5 pb-5">
      <button class="btn btn-primary mr-3 save-button" @click="save">
        Save changes
      </button>
      <button class="btn btn-dark mr-3" @click="cancel">
        Cancel
      </button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    currentConfig: {
      type: String,
      required: true

    },
    newConfig: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      diffEditor: {}
    }
  },
  methods: {
    save () {
      const changesToSave = this.diffEditor.getModifiedEditor().getValue()
      this.$emit('save', changesToSave)
    },
    cancel () {
      this.$emit('cancel')
    }
  },
  destroyed () {
    this.diffEditor.dispose()
  },
  async mounted () {
    const monaco = await import('monaco-editor/esm/vs/editor/editor.api')

    const originalModel = monaco.editor.createModel(
      this.currentConfig,
      'application/json'
    )
    const modifiedModel = monaco.editor.createModel(
      this.newConfig,
      'application/json'
    )

    this.diffEditor = monaco.editor.createDiffEditor(
      this.$refs['diff-editor']
    )

    this.diffEditor.setModel({
      original: originalModel,
      modified: modifiedModel
    })
  }
}
</script>

<style scoped >
.editor {
  margin: 0 auto;
  border: 1px solid black;
  height: 65vh;
  width: 100%;
}

.save-button {
  width: 14rem;
}
</style>
