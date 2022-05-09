<template>
  <FormGroup :id="id" :label="label" :description="description">
    <Spinner v-if="loading" />
    <MessageError v-else-if="error">{{ error }}</MessageError>
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
          class="btn btn-sm btn-primary text-white mr-1"
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
          @click.stop="clearSelection"
          v-if="showExpanded && selectionWithoutChildren.length > 0"
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
            @click.stop="clearSelection"
            v-if="!showExpanded && selectionWithoutChildren.length > 0"
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
        <span
          class="pl-4"
          v-if="
            search && Object.keys(terms).length > 50 && searchResultCount >= 0
          "
        >
          found {{ searchResultCount }} terms.
        </span>
        <InputOntologySubtree
          :key="key"
          v-if="rootTerms.length > 0"
          style="max-height: 100vh"
          class="pt-2 pl-0 dropdown-item"
          :terms="rootTerms"
          :isMultiSelect="isMultiSelect"
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
import Client from "../../client/client.js";
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import Spinner from "../layout/Spinner.vue";
import InputOntologySubtree from "./InputOntologySubtree.vue";
import MessageError from "./MessageError.vue";
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
  extends: BaseInput,
  components: {
    FormGroup,
    InputOntologySubtree,
    Spinner,
    MessageError,
  },
  directives: {
    clickOutside: vClickOutside.directive,
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
    isMultiSelect: {
      type: Boolean,
      default: false,
    },
    ontologyTableName: {
      type: String,
      required: false,
    },
    graphqlURL: {
      type: String,
      default: "graphql",
    },
  },
  data() {
    return {
      error: null,
      // used for drop down focus state
      focus: false,
      //huge object with all the terms, flattened
      //includes also its children
      terms: {},
      search: null,
      //we use key to force updates
      key: 1,
      //use to block to many search results
      searchResultCount: 0,
      data: {},
    };
  },
  computed: {
    rootTerms() {
      if (this.terms) {
        let result = Object.values(this.terms).filter(
          (t) => !t.parent && t.visible
        );
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
      //include key so it triggers on it
      if (this.key) {
        //navigate the tree, recurse into children if parent is not selected
        let result = [];
        Object.values(this.rootTerms).forEach((term) => {
          result.push(...this.getSelectedChildNodes(term));
        });
        return result;
      }
      return [];
    },
  },
  methods: {
    toggleExpand(term) {
      this.terms[term].expanded = !this.terms[term].expanded;
      this.key++;
    },
    getSelectedChildNodes(term) {
      let result = [];
      if (term.selected === "complete") {
        result.push(term.name);
      } else if (term.children) {
        term.children.forEach((childTerm) =>
          result.push(...this.getSelectedChildNodes(childTerm))
        );
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
    getParents(term) {
      let result = [];
      let parent = term.parent;
      while (parent) {
        result.push(this.terms[parent.name]);
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
      if (!this.isMultiSelect) {
        //deselect other items
        Object.keys(this.terms).forEach(
          (key) => (this.terms[key].selected = false)
        );
      }
      let term = this.terms[item];
      term.selected = "complete";
      if (this.isMultiSelect) {
        //if list also select also its children
        this.getAllChildren(term).forEach(
          (childTerm) => (childTerm.selected = "complete")
        );
        //select parent(s) if all siblings are selected
        this.getParents(term).forEach((parent) => {
          if (parent.children.every((childTerm) => childTerm.selected)) {
            parent.selected = "complete";
          } else {
            parent.selected = "partial";
          }
        });
      }
      this.emitValue();
      this.$refs.search.focus();
      this.key++;
    },
    deselect(item) {
      if (this.isMultiSelect) {
        let term = this.terms[item];
        term.selected = false;
        //also deselect all its children
        this.getAllChildren(this.terms[item]).forEach(
          (childTerm) => (childTerm.selected = false)
        );
        //also its deselect its parents, might be partial
        this.getParents(term).forEach((parent) => {
          if (parent.children.some((child) => child.selected)) {
            parent.selected = "partial";
          } else {
            parent.selected = false;
          }
        });
      } else {
        //non-list, deselect all
        Object.keys(this.terms).forEach(
          (key) => (this.terms[key].selected = false)
        );
      }
      this.emitValue();
      this.$refs.search.focus();
      this.key++;
    },
    clearSelection() {
      if (this.terms) {
        Object.values(this.terms).forEach((term) => (term.selected = false));
      }
      this.emitValue();
      this.$refs.search.focus();
      this.key++;
    },
    emitValue() {
      let selectedTerms = Object.values(this.terms)
        .filter((term) => term.selected === "complete")
        .map((term) => {
          return { name: term.name };
        });
      if (this.isMultiSelect) {
        this.$emit("input", selectedTerms);
      } else {
        this.$emit("input", selectedTerms[0]);
      }
    },
    reloadMetadata() {
      //we only load if not options provided
      if (!this.options) {
        // TableMetadataMixin.methods.reloadMetadata.call(this);
      }
    },
    reload() {
      //we only load if not options provided
      if (!this.options) {
        // TableMixin.methods.reload.call(this);
      }
    },
    applySelection(value) {
      //deselect all
      Object.keys(this.terms).forEach(
        (key) => (this.terms[key].selected = false)
      );
      //apply selection to the tree
      if (value && this.isMultiSelect) {
        //clear existing selection
        value.forEach((v) => {
          let term = this.terms[v.name];
          if (term) {
            term.selected = "complete";
            if (this.isMultiSelect) {
              //if list also select its children
              this.getAllChildren(term).forEach(
                (childTerm) => (childTerm.selected = "complete")
              );
              //select parent(s) if all siblings are selected
              this.getParents(term).forEach((parent) => {
                if (parent.children.every((childTerm) => childTerm.selected)) {
                  parent.selected = "complete";
                } else {
                  parent.selected = "partial";
                }
              });
            }
          }
        });
      }
      //not a list so singular value
      else if (value) {
        let term = this.terms[value.name];
        if (term) {
          term.selected = "complete";
          this.getParents(term).forEach((parent) => {
            parent.selected = "partial";
          });
        }
      }
      this.key++;
    },
  },
  watch: {
    search() {
      this.searchResultCount = 0;
      if (this.search) {
        //first hide all
        Object.values(this.terms).forEach((t) => (t.visible = false));
        //split and sanitize search terms
        let searchTerms = this.search
          .trim()
          .split(" ")
          .filter((s) => s.trim().length > 0)
          .map((s) => s.toLowerCase());
        //check every term if it matches all search terms
        Object.values(this.terms).forEach((term) => {
          if (searchTerms.every((s) => term.name.toLowerCase().includes(s))) {
            term.visible = true;
            this.searchResultCount++;

            //also make parents visible
            if (term.parent) {
              let parent = this.terms[term.parent.name];
              while (parent && !parent.visible) {
                parent.visible = true;
                if (parent.parent) {
                  parent = this.terms[parent.parent.name];
                }
              }
            }
          }
        });
      } else {
        //no search  = all visible
        Object.values(this.terms).forEach((t) => {
          t.visible = true;
          this.searchResultCount++;
        });
      }
      //auto expand visible automatically if total visible <50
      if (Object.values(this.terms).filter((t) => t.visible).length < 50) {
        //then expand visible
        Object.values(this.terms)
          .filter((t) => t.visible && t.children)
          .forEach((t) => (t.expanded = true));
      }
      this.key++;
    },
    value() {
      if (this.terms.size > 0) {
        this.applySelection(this.value);
      }
    },
    data() {
      if (this.data) {
        this.searchResultCount = 0;

        //convert to tree of terms
        //list all terms, incl subtrees
        let terms = {};
        this.data.forEach((e) => {
          // did we see it maybe as parent before?
          if (terms[e.name]) {
            //then copy properties, currently only definition
            terms[e.name].definition = e.definition;
          } else {
            //else simply add the record
            terms[e.name] = {
              name: e.name,
              visible: true,
              selected: false,
              definition: e.definition,
            };
          }
          if (e.parent) {
            terms[e.name].parent = e.parent;
            //did we see this parent before?
            if (!terms[e.parent.name]) {
              //otherwise add it
              terms[e.parent.name] = {
                name: e.parent.name,
                visible: true,
                selected: false,
              };
            }
            // if first child then add children array
            if (!terms[e.parent.name].children) {
              terms[e.parent.name].children = [];
            }
            // add the child
            terms[e.parent.name].children.push(terms[e.name]);
          }
          this.searchResultCount++;
        });
        this.terms = terms;
        this.applySelection(this.value);
      }
    },
  },
  async mounted() {
    if (this.ontologyTableName) {
      const client = Client.newClient(this.graphqlURL);
      this.data = (await client.fetchTableData(this.ontologyTableName))[
        this.ontologyTableName
      ];
    }
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
<template>
  <div>
    <label>ontology array</label>
    <demo-item>
      <InputOntology
          id="input-ontology-1"
          v-model="value"
          label="My ontology select"
          description="please choose your options in tree below"
          :options="[
          { name: 'pet' },
          { name: 'cat', parent: { name: 'pet' } },
          { name: 'dog', parent: { name: 'pet' } },
          { name: 'cattle' },
          { name: 'cow', parent: { name: 'cattle' } },
        ]"
          :isMultiSelect="true"
      />
      <div>You selected: {{ value }}</div>
    </demo-item>

    <label>ontology array expanded</label>
    <demo-item>
      <InputOntology
          id="input-ontology-2"
          v-model="value"
          label="My ontology select expanded"
          :showExpanded="true"
          description="please choose your options in tree below"
          :options="[
          { name: 'pet' },
          { name: 'cat', parent: { name: 'pet' } },
          { name: 'dog', parent: { name: 'pet' } },
          { name: 'cattle' },
          { name: 'cow', parent: { name: 'cattle' } },
        ]"
          :isMultiSelect="true"
      />
      <div>You selected: {{ value }}</div>
    </demo-item>

    <label>ontology (single) with backend data</label>
    <demo-item>
      <InputOntology
          id="input-ontology-3"
          label="Ontology select with backend data"
          description="please choose your options in tree below"
          v-model="value"
          :isMultiSelect="false"
          ontologyTableName="Tag"
          graphqlURL="/pet store/graphql"
      />
    </demo-item>

    <label>ontology array with backend data</label>
    <demo-item>
      <InputOntology
          id="input-ontology-4"
          label="Ontology select with backend data"
          description="please choose your options in tree below"
          v-model="value"
          :isMultiSelect="true"
          ontologyTableName="Tag"
          graphqlURL="/pet store/graphql"
      />
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null,
      };
    },
  };
</script>
</docs>
