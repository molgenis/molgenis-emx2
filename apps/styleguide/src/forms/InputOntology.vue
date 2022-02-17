<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <Spinner v-if="loading" />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div
      class="p-0 m-0"
      :class="{dropdown: !showExpanded, 'border rounded': !showExpanded}"
      v-else
    >
      <div
        class="border-0 text-left form-control"
        style="height: auto"
        @click="toggleFocus"
      >
        <span
          class="btn btn-sm btn-primary mb-2 text-white mr-1"
          v-for="v in selectionWithoutChildren"
          :key="v"
          @click.stop="deselectWithChildren(v)"
        >
          {{ v }}
          <span class="fa fa-times"></span>
        </span>
        <i
          class="p-2 fa fa-times"
          style="vertical-align: middle"
          @click.stop="deselect(selection)"
          v-if="showExpanded && selectionWithoutParents.length > 0"
        />
        <span :class="{'input-group': showExpanded}">
          <div v-if="showExpanded" class="input-group-prepend">
            <button
              class="btn border-right-0 border btn-outline-primary"
              type="button"
            >
              <i class="fa fa-search"></i>
            </button>
          </div>
          <input
            type="text"
            ref="search"
            :placeholder="focus || showExpanded ? 'Type to search' : ''"
            :class="{
              'form-control': showExpanded,
              'border-0': !showExpanded
            }"
            v-model="search"
            @click.stop
            @focus="focus = true"
          />
        </span>
        <span class="d-inline-block float-right">
          <i
            class="p-2 fa fa-times"
            style="vertical-align: middle"
            @click.stop="deselect(selection)"
            v-if="!showExpanded && selectionWithoutParents.length > 0"
          />
          <i
            class="p-2 fa fa-caret-down"
            style="vertical-align: middle"
            v-if="!showExpanded"
          />
        </span>
      </div>
      <div
        class="w-100 show p-0 overflow-auto"
        :class="{'dropdown-menu': !showExpanded}"
        v-if="focus || showExpanded"
        v-click-outside="loseFocusWhenClickedOutside"
      >
        <InputOntologySubtree
          v-if="hasSearchResults"
          style="max-height: 50vh"
          class="pt-2 pl-0 dropdown-item"
          :terms="terms"
          :selection="selection"
          :expanded="expanded"
          :isList="isList"
          :search="search"
          @toggleExpand="toggleExpand"
          @select="select"
          @deselect="deselect"
        />
        <div v-else>No results found</div>
      </div>
    </div>
  </FormGroup>
</template>

<style>
input:focus {
  outline: none;
}
</style>

<script>
import _baseInput from './_baseInput';
import TableMixin from '../mixins/TableMixin';
import TableMetadataMixin from '../mixins/TableMetadataMixin';
import FormGroup from './_formGroup';
import InputOntologySubtree from './InputOntologySubtree';
import MessageError from './MessageError';
import Spinner from '../layout/Spinner';
import vClickOutside from 'v-click-outside';

/**
 * Expects a table that has as structure {name, parent{name} and optionally code, definition, ontologyURI}
 *
 * Known limitations: this version retrieves complete ontology and renders in place. Purpose is to enable sensible data entry and limited use in filter user interface.
 *
 * For future versions we have many ideas for improvements, for example:
 * - want to make it more lazy, only retrieving the 'root' elements and loading the children when needed.
 * - adding search to pre-filter elements / paths
 */
export default {
  extends: _baseInput,
  mixins: [TableMixin],
  directives: {
    clickOutside: vClickOutside.directive
  },
  components: {
    FormGroup,
    InputOntologySubtree,
    MessageError,
    Spinner
  },
  props: {
    /** if you don't want to use autoload using table you can provide options via 'items'. Should be format [{name:a, parent:b},{name:b}]
     */
    options: {
      type: Array,
      default: null
    },
    /** show as pulldown. When false, shows always expanded*/
    showExpanded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      terms: [],
      selection: [],
      expanded: [],
      search: null
    };
  },
  computed: {
    //Override tableMixin
    orderByObject() {
      if (
        this.tableMetadata &&
        this.tableMetadata.columns.some((c) => c.name === 'order')
      ) {
        return {order: 'ASC'};
      } else {
        return {};
      }
    },
    //below will be used in case ontology is not a hierarchy
    selectionWithoutParents() {
      if (this.isList) {
        return this.selection.filter((t) => this.getParents(t).length > 0);
      } else if (this.selection[0] != null) {
        return this.selection;
      } else {
        return [];
      }
    },
    //below will be used in case ontology is a hierarchy
    selectionWithoutChildren() {
      if (this.isList) {
        //skip children if parent selected
        return this.selection.filter((t) => {
          let parents = this.getParents([t]);
          return parents.length == 0 || !this.selection.includes(parents[0]);
        });
      } else if (this.selection[0] != null) {
        return this.selection;
      } else {
        return [];
      }
    },
    hasSearchResults() {
      if (this.search) {
        return this.hasSearchResultsRecursive(this.terms);
      }
      return true;
    }
  },
  methods: {
    hasSearchResultsRecursive(terms) {
      return this.search
        .split(' ')
        .every((t) =>
          terms.some(
            (term) =>
              term.name.toLowerCase().includes(t.toLowerCase()) ||
              (term.children && this.hasSearchResultsRecursive(term.children))
          )
        );
    },
    loseFocusWhenClickedOutside() {
      if (this.focus && !this.showExpanded) {
        this.focus = false;
      }
    },
    toggleFocus() {
      if (!this.showExpanded) {
        this.focus = !this.focus;
        if (this.focus) {
          this.$refs.search.focus();
        }
      }
    },
    getChildren(name) {
      return this.data.filter((o) => o.parent && o.parent.name === name);
    },
    getChildrenRecursive(name) {
      return this.data
        .filter((o) => o.parent && o.parent.name === name)
        .map((t) => {
          let children = this.getChildrenRecursive(t.name);
          if (children.length > 0) {
            t.children = children;
          }
          return t;
        });
    },
    toggleExpand(name) {
      if (this.expanded.indexOf(name) === -1) {
        this.expanded.push(name);
      } else {
        this.expanded = this.expanded.filter((s) => s !== name);
      }
      this.$refs.search.focus();
    },
    select(items) {
      //if not a list you can only select one
      if (!this.isList) {
        this.selection = [];
      }
      //add items not yet selected
      items.forEach((i) => {
        if (this.selection.indexOf(i) === -1) {
          this.selection.push(i);
        }
      });
      //if all children are selected for a node then also select the parent (query expansion)

      //alert("selectArray: " + JSON.stringify(this.selection));
      this.emitValue();
      this.$refs.search.focus();
    },
    getParents(items) {
      let result = this.data
        .filter((t) => t.parent && items.indexOf(t.name) != -1)
        .map((t) => t.parent.name);
      //get parents of the parents
      if (result.length > 0) {
        result.push(...this.getParents(result));
      }
      return result;
    },
    deselectWithChildren(item) {
      if (this.isList) {
        this.deselect([item]);
        this.getChildren(item).forEach((t) =>
          this.deselectWithChildren(t.name)
        );
      } else {
        this.selection = [];
      }
      this.emitValue();
      this.$refs.search.focus();
    },
    deselect(items) {
      if (this.isList) {
        //add parents
        items.push(...this.getParents(items));
        //should also deselect any (indirect) parents
        this.selection = this.selection.filter((name) => !items.includes(name));
      } else {
        this.selection = [];
      }
      this.emitValue();
      this.$refs.search.focus();
    },
    emitValue() {
      if (this.isList) {
        this.$emit(
          'input',
          this.selection.map((s) => {
            return {name: s};
          })
        );
      } else {
        this.$emit('input', {name: this.selection[0]});
      }
    },
    reloadMetadata() {
      //we only load if not options provided
      if (!this.options) {
        TableMetadataMixin.methods.reloadMetadata.call(this);
      }
    },
    reload() {
      //we only load if not options provided
      if (!this.options) {
        TableMixin.methods.reload.call(this);
      }
    }
  },
  watch: {
    options() {
      this.data = this.options;
    },
    value() {
      if (this.isList) {
        this.selection = this.value ? this.value.map((term) => term.name) : [];
      } else {
        this.selection = this.value ? [this.value.name] : [];
      }
    },
    data() {
      if (this.data) {
        this.terms = this.data
          .filter((o) => !o.parent)
          .map((o) => {
            let children = this.getChildrenRecursive(o.name);
            if (children.length > 0) {
              o.children = children;
            }
            return o;
          });
      }
    }
  },
  created() {
    if (this.options) {
      this.data = this.options;
    } else {
      //override default
      this.limit = 10000;
    }
    this.loading = false;
  }
};
</script>

<docs>
Example with hardcoded options, can select multiple
```
<template>
  <div>
    <InputOntology label="My ontology select" description="please choose your options in tree below" v-model="myvalue"
                   :options="[{name:'pet'},{name:'cat',parent:{name:'pet'}},{name:'dog',parent:{name:'pet'}},{name:'cattle'},{name:'cow',parent:{name:'cattle'}}]"
                   :isList="true"/>
    myvalue = {{ myvalue }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        myvalue: []
      };
    }
  }
</script>
```
Example 'expanded' with hardcoded options, can select multiple
```
<template>
  <div>
    <InputOntology label="My ontology select" description="please choose your options in tree below" v-model="myvalue"
                   :showExpanded="true"
                   :options="[{name:'pet'},{name:'cat',parent:{name:'pet'}},{name:'dog',parent:{name:'pet'}},{name:'cattle'},{name:'cow',parent:{name:'cattle'}}]"
                   :isList="true"/>
    myvalue = {{ myvalue }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        myvalue: []
      };
    }
  }
</script>
```

Example with hardcoded options, can select only single item
```
<template>
  <div>
    <InputOntology label="My ontology select" description="please choose your options in tree below" v-model="myvalue"
                   :options="[{name:'pet'},{name:'cat',parent:{name:'pet'}},{name:'dog',parent:{name:'pet'}},{name:'cattle'},{name:'cow',parent:{name:'cattle'}}]"
    />
    myvalue = {{ myvalue }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        myvalue: []
      };
    }
  }
</script>
```

Example with loading contents from table on backend (requires sign-in), multiple select
```
<template>
  <div>
    <InputOntology label="My ontology select" description="please choose your options in tree below" v-model="myvalue"
                   table="Category" :isList="true" graphqlURL="/pet store/graphql"/>
    myvalue = {{ myvalue }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        myvalue: []
      };
    }
  }
</script>
```

Example with loading contents from table on backend (requires sign-in)
```
<template>
  <div>
    <InputOntology label="My ontology select" description="please choose your options in tree below" v-model="myvalue"
                   table="Category" graphqlURL="/pet store/graphql"/>
    myvalue = {{ myvalue }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        myvalue: []
      };
    }
  }
</script>
```
</docs>
