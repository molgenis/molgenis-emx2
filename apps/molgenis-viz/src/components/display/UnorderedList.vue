<template>
  <ul :class="classNames">
    <!-- list content -->
    <slot></slot>
  </ul>
</template>

<script>

// The `<UnorderedList />` component is primarily used in the `<PageFooter />` component to display links to other pages (both internally and externally). The list may be rendered horizontally (ideal for the links to legal pages) or vertically (site maps).
export default {
  props: {
    // determine if the list should be rendered vertically (default) or horizontally
    listLayout: {
      // `'vertical' / 'horizontal'`
      type: String,
      default: 'vertical',
      validator: (value) => {
        const layouts = ['horizontal', 'vertical']
        return layouts.includes(value)
      }
    },
    // Choose the icon that separates each link
    listType: {
      // `'none' / 'circle' / 'square' `
      type: String,
      // `circle`
      default: 'circle',
      validator: (value) => {
        const separators = ['none', 'circle', 'square']
        return separators.includes(value)
      }
    }
  },
  computed: {
    classNames () {
      const base = 'unordered-list'
      const layout = `list-layout-${this.listLayout}`
      const type = `list-separator-${this.listType}`
      return [base, layout, type].join(' ')
    }
  }
}
</script>

<style lang="scss">

@mixin separator {
  content: '';
  display: inline-block;
  background-color: currentColor;
  margin-right: 0.3em;
  margin-bottom: 1px;
  $size: 8px;
  width: $size;
  height: $size;
}

.unordered-list {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 0;
  margin: 0;
  padding: 0;
  color: currentColor;
  list-style: none;
  
  li {
    position: relative;
    a {
      color: currentColor;
    }
  }
  
  &.list-style-none {
    padding: 0;
  }
  
  &.list-layout-horizontal {
    flex-direction: row;
    gap: 1em;
    padding: 0;
  }
  
  &.list-layout-vertical {
    flex-direction: column;
    gap: 0.4em;
  }
  
  &.list-separator-circle {
    li {
      &::before {
        @include separator;
        border-radius: 50%;
      }
    }
  }

  &.list-separator-square {
    li {
      &::before {
        @include separator;
      }
    }
  } 
    
  &.list-layout-horizontal {
    li {
      &::before {
       margin-right: 1em;
      }
      &:first-child {
        &::before {
          display: none;
        }
      }
    }
  }
}
</style>