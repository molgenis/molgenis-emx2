<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <Spinner v-if="loading" />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div v-else class="p-0 m-0">
      <InputOntologySubtree
        class="pl-0"
        :terms="terms"
        :selection="selection"
        :expanded="expanded"
        :list="list"
        @toggleExpand="toggleExpand"
        @select="select"
        @deselect="deselect"
      />
    </div>
  </FormGroup>
</template>

<script>
import _baseInput from "./_baseInput";
import TableMixin from "../mixins/TableMixin";
import TableMetadataMixin from "../mixins/TableMetadataMixin";
import FormGroup from "./_formGroup";
import InputOntologySubtree from "./InputOntologySubtree";
import MessageError from "./MessageError";
import Spinner from "../layout/Spinner";

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
  components: {
    FormGroup,
    InputOntologySubtree,
    MessageError,
    Spinner,
  },
  props: {
    /** if you don't want to use autoload using table you can provide options via 'items'. Should be format [{name:a, parent:b},{name:b}]
     */
    options: {
      type: Array,
      default: null,
    },
  },
  data() {
    return {
      terms: [],
      selection: [],
      expanded: [],
    };
  },
  computed: {
    //Override tableMixin
    orderByObject() {
      if (
        this.tableMetadata &&
        this.tableMetadata.columns.some((c) => c.name === "order")
      ) {
        return { order: "ASC" };
      } else {
        return {};
      }
    },
  },
  methods: {
    getChildren(name) {
      return this.data.filter((o) => o.parent && o.parent.name === name);
    },
    toggleExpand(name) {
      if (this.expanded.indexOf(name) === -1) {
        this.expanded.push(name);
      } else {
        this.expanded = this.expanded.filter((s) => s !== name);
      }
    },
    select(items) {
      //if not a list you can only select one
      if (!this.list) {
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
    deselect(items) {
      if (this.list) {
        //add parents
        items.push(...this.getParents(items));
        //should also deselect any (indirect) parents
        this.selection = this.selection.filter((name) => !items.includes(name));
      } else {
        this.selection = [];
      }
      this.emitValue();
    },
    emitValue() {
      if (this.list) {
        this.$emit(
          "input",
          this.selection.map((s) => {
            return { name: s };
          })
        );
      } else {
        this.$emit("input", { name: this.selection[0] });
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
    },
  },
  watch: {
    options() {
      this.data = this.options;
    },
    value() {
      if (this.list) {
        this.selection = this.value ? this.value.map((term) => term.name) : [];
      } else {
        this.selection = this.value ? [this.value.name] : [];
      }
    },
    data() {
      this.terms = this.data
        .filter((o) => !o.parent)
        .map((o) => {
          let children = this.getChildren(o.name);
          if (children.length > 0) {
            o.children = children;
          }
          return o;
        });
    },
  },
  created() {
    if (this.options) {
      this.data = this.options;
    } else {
      //override default
      this.limit = 10000;
    }
    this.loading = false;
  },
};
</script>

<docs>
Example with hardcoded options, can select multiple
```
<template>
  <div>
    <InputOntology label="My ontology select" description="please choose your options in tree below" v-model="myvalue"
                   :options="[{name:'pet'},{name:'cat',parent:{name:'pet'}},{name:'dog',parent:{name:'pet'}},{name:'cattle'},{name:'cow',parent:{name:'cattle'}}]"
                   :list="true"/>
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
                   table="Tag" :list="true"  graphqlURL="/pet store/graphql"/>
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
                   table="Tag"  graphqlURL="/pet store/graphql"/>
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
