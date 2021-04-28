<template>
    <div class="list-group-item list-group-item-action text-lowercase">
      <div class="form-check text-truncate">
        <input
          v-if="!hasChildren" 
          :id="'check-input-' + keyword.name"
          class="form-check-input" 
          type="checkbox" 
          :checked="isSelected" 
          @change="handleChange(keyword.name)"
        >
        <label class="form-check-label" :for="'check-input-' + keyword.name" @click="handleClick">
          {{ keyword.definition }} 
        </label>
        <i 
          v-if="hasChildren && isCollapsed" 
          class="float-right fa fa-caret-up"
          @click="handleClick"
        ></i>
        <i 
          v-if="hasChildren && !isCollapsed" 
          class="float-right fa fa-caret-down"
          @click="handleClick"
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
      handleClick(event) {
        if (this.hasChildren) {
          this.isCollapsed = !this.isCollapsed
        }
      }
    }
}
</script>