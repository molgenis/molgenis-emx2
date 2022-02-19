<template>
  <FormGroup v-bind="$props" v-on="$listeners">
    <Spinner v-if="loading" />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div
      class="p-0 m-0"
      :class="{ dropdown: !showExpanded, 'border rounded': !showExpanded }"
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
          @click.stop="deselect(v)"
        >
          {{ v }}
          <span class="fa fa-times"></span>
        </span>
        <i
          class="p-2 fa fa-times"
          style="vertical-align: middle"
          @click.stop="deselect(selection)"
          v-if="showExpanded && this.selection.length > 0"
        />
        <span :class="{ 'input-group': showExpanded }">
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
              'border-0': !showExpanded,
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
            v-if="!showExpanded && this.selection.length > 0"
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
        :class="{ 'dropdown-menu': !showExpanded }"
        v-if="focus || showExpanded"
        v-click-outside="loseFocusWhenClickedOutside"
      >
        <InputOntologySubtree
          :key="key"
          v-if="rootTerms.length > 0"
          style="max-height: 100vh"
          class="pt-2 pl-0 dropdown-item"
          :terms="rootTerms"
          :selection="selection"
          :list="list"
          @select="select"
          @deselect="deselect"
          @toggleExpand="toggleExpand"
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
import _baseInput from "./_baseInput";
import TableMixin from "../mixins/TableMixin";
import TableMetadataMixin from "../mixins/TableMetadataMixin";
import FormGroup from "./_formGroup";
import InputOntologySubtree from "./InputOntologySubtree";
import MessageError from "./MessageError";
import Spinner from "../layout/Spinner";
import vClickOutside from "v-click-outside";

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
    clickOutside: vClickOutside.directive,
  },
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
    /** show as pulldown. When false, shows always expanded*/
    showExpanded: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      //huge object with all the terms, flattened
      //includes also its children
      terms: {},
      search: null,
      selection: [],
      key: 1,
    };
  },
  computed: {
    rootTerms() {
      if (this.terms) {
        console.log("root terms");
        let timer = Date.now();
        let result = Object.values(this.terms).filter(
          (t) => !t.parent && t.visible
        );
        console.log("root terms complete " + (Date.now() - timer));
        return result;
      } else {
        return [];
      }
    },
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
    selectionWithoutChildren() {
      if (this.list) {
        console.log("start selectionWithoutChildren");
        let timer = Date.now();
        //navigate the tree, recurse into children if parent is not selected
        let result = [];
        this.rootTerms.forEach((t) =>
          result.push(...this.getSelectedChildNodes(t))
        );
        console.log(
          "complete selectionWithoutChildren " + (Date.now() - timer)
        );
        return result;
      } else if (this.selection[0] != null) {
        return this.selection;
      } else {
        return [];
      }
    },
  },
  methods: {
    toggleExpand(term) {
      this.terms[term].expanded = !this.terms[term].expanded;
      this.key++;
    },
    getSelectedChildNodes(term) {
      let result = [];
      if (this.selection.includes(term.name)) {
        result.push(term.name);
      } else {
        if (term.children) {
          term.children.forEach((t) =>
            result.push(...this.getSelectedChildNodes(t))
          );
        }
      }
      return result;
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
    getParentNames(term) {
      let result = [];
      let parent = term.parent;
      while (parent) {
        result.push(parent.name);
        if (
          this.terms[parent.name].parent &&
          //check for parent that are indirect parent of themselves
          !result.includes(this.terms[parent.name].parent.name)
        ) {
          parent = this.terms[parent.name].parent;
        } else {
          parent = null;
        }
      }
      return result;
    },
    getChildren(name) {
      return this.data.filter((o) => o.parent && o.parent.name === name);
    },
    getAllChildren(term) {
      let result = [];
      if (term.children) {
        result = term.children;
        term.children.forEach(
          (childTerm) =>
            (result = result.concat(this.getAllChildren(childTerm)))
        );
      }
      return result;
    },
    select(item) {
      console.log("start select");
      let timer = Date.now();
      if (!this.list) {
        this.selection = [];
      }
      this.selection.push(item);
      let term = this.terms[item];
      if (this.list) {
        //if list also select also its children
        this.getAllChildren(term).forEach((childTerm) =>
          this.selection.push(childTerm.name)
        );
        //select parent(s) if all siblings are selected
        this.getParentNames(term).forEach((parentName) => {
          let parent = this.terms[parentName];
          if (
            parent.children.every((childTerm) =>
              this.selection.includes(childTerm.name)
            )
          ) {
            this.selection.push(parentName);
          }
        });
      }
      this.emitValue();
      this.$refs.search.focus();
      console.log("complete select " + (Date.now() - timer));
    },
    deselect(item) {
      console.log("start deselect");
      let timer = Date.now();
      if (this.list) {
        this.selection = this.selection.filter((s) => s != item);
        //also deselect all its children
        this.getAllChildren(this.terms[item]).forEach(
          (childTerm) =>
            (this.selection = this.selection.filter((s) => s != childTerm.name))
        );
        //also its deselect its parents
        this.getParentNames(this.terms[item]).forEach(
          (parentName) =>
            (this.selection = this.selection.filter((s) => s != parentName))
        );
      } else {
        this.selection = [];
      }
      this.emitValue();
      this.$refs.search.focus();
      console.log("complete deselect " + (Date.now() - timer));
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
    search() {
      console.log("apply search");
      let timer = Date.now();
      //first show/hide depending on filter
      Object.values(this.terms).forEach(
        (t) => (t.visible = this.search == "" || !this.search)
      );
      if (this.search && this.search.length > 0) {
        let searchTerms = this.search.split(" ").map((s) => s.toLowerCase());
        console.log("searching " + searchTerms);
        Object.values(this.terms).forEach((term) => {
          if (searchTerms.every((s) => term.name.toLowerCase().includes(s))) {
            //items are visible when matching search, or when a child matches search
            term.visible = true;
            this.getParentNames(term).forEach((parentName) => {
              this.terms[parentName].visible = true;
            });
          }
        });
      }
      //collapse all first
      Object.values(this.terms).forEach((t) => (t.expanded = false));
      //auto expand visible automatically if total visible <50
      if (Object.values(this.terms).filter((t) => t.visible).length < 50) {
        //then expand visible
        Object.values(this.terms)
          .filter((t) => t.visible && t.children)
          .forEach((t) => (t.expanded = true));
      }
      this.key++;
      console.log("apply search complete " + (Date.now() - timer));
    },
    value() {
      if (this.list) {
        this.selection = this.value ? this.value.map((term) => term.name) : [];
      } else {
        this.selection = this.value ? [this.value.name] : [];
      }
    },
    data() {
      if (this.data) {
        //convert to tree of terms
        console.log("create term tree");
        let timer = Date.now();
        //list all terms, incl subtrees
        let terms = {};
        this.data.forEach((e) => {
          // did we see it maybe as parent before?
          if (terms[e.name]) {
            //then copy properties, currently only definition
            terms[e.name].definition = e.definition;
          } else {
            //else simply add the record
            terms[e.name] = e;
            e.visible = true;
          }
          if (e.parent) {
            //did we see this parent before?
            if (!terms[e.parent.name]) {
              //otherwise add it
              terms[e.parent.name] = { name: e.parent.name, visible: true };
            }
            // if first child then add children array
            if (!terms[e.parent.name].children) {
              terms[e.parent.name].children = [];
            }
            // add the child
            terms[e.parent.name].children.push(e);
          }
        });
        this.terms = terms;
        console.log("complete create term tree " + (Date.now() - timer));
      }
    },
  },
  created() {
    if (this.options) {
      this.data = this.options;
    } else {
      //override default
      this.limit = 100000;
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
Example 'expanded' with hardcoded options, can select multiple
```
<template>
  <div>
    <InputOntology label="My ontology select" description="please choose your options in tree below" v-model="myvalue"
                   :showExpanded="true"
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
                   table="Tag" :list="true" graphqlURL="/pet store/graphql"/>
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
                   table="Tag" graphqlURL="/pet store/graphql"/>
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
