<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <div class="p-0 m-0">
      <InputOntologySubtree
        class="pl-0"
        :terms="terms"
        :selection="selection"
        :expanded="expanded"
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
import FormGroup from "./_formGroup";
import InputOntologySubtree from "./InputOntologySubtree";

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
      return { order: "ASC" };
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
      //add items not yet selected
      items.forEach((i) => {
        if (this.selection.indexOf(i) === -1) {
          this.selection.push(i);
        }
      });
      //if all children are selected for a node then also select the parent (query expansion)

      //alert("selectArray: " + JSON.stringify(this.selection));
      this.emitList();
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
      //add parents
      items.push(...this.getParents(items));
      //should also deselect any (indirect) parents
      this.selection = this.selection.filter((name) => !items.includes(name));
      this.emitList();
    },
    emitList() {
      if (this.list) {
        this.$emit(
          "input",
          this.selection.map((s) => {
            return { name: s };
          })
        );
      }
    },
    toggleSelect(name) {
      if (this.list) {
        if (this.selection.indexOf(name) === -1) {
          this.selection.push(name);
        } else {
          this.selection = this.selection.filter((s) => s !== name);
        }
        this.emitList();
      } else {
        this.selection = [name];
        this.$emit("input", name);
      }
    },
  },
  watch: {
    value() {
      this.selection = this.value ? this.value.map((term) => term.name) : [];
    },
    data() {
      this.terms = this.data
        .filter((o) => !o.parent)
        .map((o) => {
          o.children = this.getChildren(o.name);
          return o;
        });
    },
  },
  created() {
    //override default
    this.limit = 10000;
  },
};
</script>

<docs>
Example with multiple select
```
<template>
  <div>
    <InputOntology v-model="myvalue" graphqlURL="/CohortNetwork/graphql" table="AreasOfInformation" :list="true"/>
    myvalue = {{ myvalue }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        myvalue: null
      };
    }
  }
</script>
```

Example with single select
```
<template>
  <div>
    <InputOntology v-model="myvalue" graphqlURL="/CohortNetwork/graphql" table="AreasOfInformation"/>
    myvalue = {{ myvalue }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        myvalue: null
      };
    }
  }
</script>
```
</docs>
