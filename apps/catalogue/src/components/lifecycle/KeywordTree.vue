<template>
  <keyword-level 
    :keywords="keywordTree"
    :selectedKeywordNames="value"
    @change-keyword-check="handleKeywordSelectionChange"
    :handleChange="handleKeywordSelectionChange"
  >
</keyword-level>
</template>

<script>
import KeywordLevel from './KeywordLevel.vue'

export default {
  name: 'KeywordTree',
  components: { KeywordLevel },
  props: {
    keywords: Array,
    value: Array
  },
  computed: {
    keywordTree () {
      // normalize array, fill out empty parents
      const normalized = this.keywords.map(keyword => !keyword.parent ? {...keyword, parent: { name: null }} : keyword)

      // recursive list to tree function
      const nest = (items, name = null) => {
        return items
          .filter(item => item.parent.name === name)
          .map(item => ({ ...item, children: nest(items, item.name) }))
      }
       
      // create tree from list
      return nest(normalized)
    }
  },
  methods: {
    handleKeywordSelectionChange (name) {
        if(this.value.includes(name)) {
          this.value.splice(this.value.indexOf(name), 1)
        } else {
          this.value.push(name)
        }
    }
  }
}
</script>