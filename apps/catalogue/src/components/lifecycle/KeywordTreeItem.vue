<template>
    <div class="list-group-item list-group-item-action text-lowercase" @click.stop="handleClick">
      <div class="form-check text-truncate">
        <input
          v-if="!hasChildren" 
          :id="'check-input-' + keyword.name"
          class="form-check-input" 
          type="checkbox"
          :checked="isSelected" 
          @change="handleChange(keyword.name)"
        >
        <label class="form-check-label" :for="'check-input-' + keyword.name">
          {{ keyword.definition }} 
        </label>
        <i 
          v-if="hasChildren && isCollapsed" 
          class="float-right fa fa-caret-up"
        ></i>
        <i 
          v-if="hasChildren && !isCollapsed" 
          class="float-right fa fa-caret-down"
        ></i>
        <keyword-level 
          v-if="hasChildren && !isCollapsed" 
          :keywords="keyword.children"
          :handleChange="handleChange"
          :selectedKeywordNames="selectedKeywordNames"
        ></keyword-level>
      </div>
    </div>
</template>

<script>
export default {
    name: 'KeywordTreeItem',
    components: {
      KeywordLevel: () => import('./KeywordLevel.vue')
    },
    props: {
      keyword: Object,
      handleChange: Function,
      selectedKeywordNames: Array
    },
    data () {
      return {
        isCollapsed: true
      }
    },
    computed: {
      hasChildren () {
        return this.keyword.children.length
      },
      isSelected () {
        return this.selectedKeywordNames.includes(this.keyword.name)
      }
    },
    methods: {
      handleClick() {
        if (this.hasChildren) {
          this.isCollapsed = !this.isCollapsed
        } else {
          this.handleChange(this.keyword.name)
        }
      }
    }
}
</script>

<style scoped>

.list-group-item:hover, .form-check-label:hover, form-check-input:hover {
  cursor: pointer;
}

</style>