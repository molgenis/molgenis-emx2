<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <MessageError v-if="error">{{ error }}</MessageError>
    <div
      class="p-0 m-0"
      :class="{ dropdown: !showExpanded, 'border rounded': !showExpanded }"
      v-else
    >
      <div
        class="border-0 text-left form-control"
        style="height: auto; cursor: pointer"
        @click="toggleFocus"
      >
        <span
          class="btn btn-sm btn-primary text-white mr-1"
          v-for="selectedTerm in selectionWithoutChildren.sort(
            (a, b) => a.order - b.order
          )"
          :key="selectedTerm"
          @click.stop="deselect(selectedTerm.name)"
        >
          {{ selectedTerm.label ? selectedTerm.label : selectedTerm.name }}
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
          <span @click.prevent.stop="clearSelection" style="cursor: pointer">
            <i
              class="p-2 fa fa-times"
              style="vertical-align: middle"
              v-if="!showExpanded && selectionWithoutChildren.length > 0"
            ></i>
          </span>
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
          v-if="rootTerms.length > 0"
          :key="key"
          :terms="rootTerms"
          :isMultiSelect="isMultiSelect"
          @select="select"
          @deselect="deselect"
          @toggleExpand="toggleExpand"
          style="max-height: 100vh"
          class="pt-2 pl-0"
        />
        <Spinner v-else-if="loading" />
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

<script lang="ts">
import Client from "../../client/client";
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import InputOntologySubtree from "./InputOntologySubtree.vue";
import MessageError from "./MessageError.vue";
//@ts-ignore
import vClickOutside from "click-outside-vue3";
import Spinner from "../layout/Spinner.vue";

/**
 * Expects a table that has as structure {name, parent{name} and optionally code, definition, ontologyURI}
 *
 * Known limitations: this version retrieves complete ontology and renders in place.
 * Purpose is to enable sensible data entry and limited use in filter user interface.
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
    MessageError,
    Spinner,
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
    tableId: {
      type: String,
      required: false,
    },
    schemaId: {
      type: String,
      required: false,
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
      data: [],
      loading: true,
    };
  },
  computed: {
    rootTerms() {
      if (this.terms) {
        let result = Object.values(this.terms).filter(
          (term: any) => !term.parent && term.visible
        );
        return result;
      } else {
        return [];
      }
    },
    selectionWithoutChildren() {
      //include key so it triggers on it
      let result: any[] = [];
      if (this.key) {
        //navigate the tree, recurse into children if parent is not selected
        this.rootTerms.forEach((term: any) => {
          result.push(...getSelectedChildNodes(term));
        });
      }
      return result;
    },
  },
  methods: {
    toggleExpand(term: string) {
      this.terms[term].expanded = !this.terms[term].expanded;
      this.key++;
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
    getParents(term: Record<string, any>) {
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
    getChildren(name: string) {
      return this.data.filter(
        (o: Record<string, any>) => o.parent?.name === name
      );
    },
    getAllChildren(term: Record<string, any>) {
      let result: Record<string, any>[] = [];
      if (term.children) {
        result = term.children;
        term.children.forEach(
          (childTerm: Record<string, any>) =>
            (result = result.concat(this.getAllChildren(childTerm)))
        );
      }
      return result;
    },
    select(item: string) {
      if (!this.isMultiSelect) {
        //deselect other items
        Object.keys(this.terms).forEach(
          (key) => (this.terms[key].selected = "unselected")
        );
      }
      let term = this.terms[item];
      term.selected = "complete";
      if (this.isMultiSelect) {
        //if list also select also its children
        this.getAllChildren(term).forEach(
          (childTerm: Record<string, any>) => (childTerm.selected = "complete")
        );
        //select parent(s) if all siblings are selected
        this.getParents(term).forEach((parent: Record<string, any>) => {
          if (
            parent.children.every(
              (childTerm: Record<string, any>) =>
                childTerm.selected === "complete"
            )
          ) {
            parent.selected = "complete";
          } else {
            parent.selected = "partial";
          }
        });
      }
      this.emitValue();
      this.$refs.search.focus();
      if (!this.isMultiSelect) {
        //close on select
        this.focus = false;
      }
      this.key++;
    },
    deselect(item: string) {
      if (this.isMultiSelect) {
        let term = this.terms[item];
        term.selected = "unselected";
        //also deselect all its children
        this.getAllChildren(this.terms[item]).forEach(
          (childTerm: Record<string, any>) =>
            (childTerm.selected = "unselected")
        );
        //also its deselect its parents, might be partial
        this.getParents(term).forEach((parent: Record<string, any>) => {
          if (
            parent.children.some(
              (child: Record<string, any>) => child.selected === "complete"
            )
          ) {
            parent.selected = "partial";
          } else {
            parent.selected = "unselected";
          }
        });
      } else {
        //non-list, deselect all
        Object.keys(this.terms).forEach(
          (key) => (this.terms[key].selected = "unselected")
        );
      }
      this.emitValue();
      this.$refs.search.focus();
      this.key++;
    },
    clearSelection() {
      if (this.terms) {
        Object.values(this.terms).forEach(
          (term: any) => (term.selected = "unselected")
        );
      }
      this.emitValue();
      this.$refs.search.focus();
      this.key++;
    },
    emitValue() {
      let selectedTerms = Object.values(this.terms)
        .filter((term: any) => term.selected === "complete")
        .map((term: any) => {
          return { name: term.name };
        });
      if (this.isMultiSelect) {
        this.$emit("update:modelValue", selectedTerms);
      } else {
        //need explicit 'null' to ensure value is emitted in form
        this.$emit("update:modelValue", selectedTerms[0] || null);
      }
    },
    applySelection(value: Record<string, any>) {
      //deselect all
      Object.keys(this.terms).forEach(
        (key) => (this.terms[key].selected = "unselected")
      );
      //apply selection to the tree
      if (value && this.isMultiSelect) {
        //clear existing selection
        value.forEach((v: Record<string, any>) => {
          let term = this.terms[v.name];
          if (term) {
            //select if doesn't have children
            if (this.getAllChildren(term).length == 0) {
              term.selected = "complete";
            }
            if (this.isMultiSelect) {
              //if list also select its children
              this.getAllChildren(term).forEach(
                (childTerm: Record<string, any>) =>
                  (childTerm.selected = "complete")
              );
              //select parent(s) if all siblings are selected
              this.getParents(term).forEach((parent: Record<string, any>) => {
                if (
                  parent.children.every(
                    (childTerm: Record<string, any>) =>
                      childTerm.selected === "complete"
                  )
                ) {
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
          this.getParents(term).forEach((parent: Record<string, any>) => {
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
        Object.values(this.terms).forEach((t: any) => {
          t.visible = false;
          t.selectable = false;
        });
        //split and sanitize search terms
        let searchTerms = this.search
          .trim()
          .split(/[\s,:]+/)
          .filter((s: string) => s.trim().length > 0)
          .map((s: string) => s.toLowerCase());
        //check every term if it matches all search terms
        Object.values(this.terms).forEach((term: any) => {
          if (
            searchTerms.every(
              (s: string) =>
                term.name.toLowerCase().includes(s) ||
                term.label?.toLowerCase().includes(s) ||
                term.definition?.toLowerCase().includes(s) ||
                term.code?.toLowerCase().includes(s) ||
                term.codesystem?.toLowerCase().includes(s)
            )
          ) {
            term.visible = true;
            term.selectable = true;
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

            //also make children selectable and visible
            this.getAllChildren(term).forEach((t) => {
              t.visible = true;
              t.selectable = true;
            });
          }
        });
      } else {
        //no search  = all visible and selectable
        Object.values(this.terms).forEach((t: any) => {
          t.visible = true;
          t.selectable = true;
          this.searchResultCount++;
        });
      }
      //auto expand visible automatically if total visible <50
      if (Object.values(this.terms).filter((t: any) => t.visible).length < 50) {
        //then expand visible
        Object.values(this.terms)
          .filter((t: any) => t.visible && t.children)
          .forEach((t: any) => (t.expanded = true));
      }
      this.key++;
    },
    modelValue: {
      deep: true,
      handler() {
        this.applySelection(this.modelValue);
        //vue has problem to react on changes deep changes in selection tree
        //therefore we use this key to force updates in this component
        this.key++;
      },
    },
    data() {
      if (this.data) {
        this.searchResultCount = 0;

        //convert to tree of terms
        //list all terms, incl subtrees
        let terms: Record<string, any> = {};
        this.data.forEach((term: Record<string, any>) => {
          // did we see it maybe as parent before?
          if (terms[term.name]) {
            //then copy properties, currently only definition and label
            terms[term.name].definition = term.definition;
            terms[term.name].label = term.label;
            terms[term.name].code = term.code;
            terms[term.name].codesystem = term.codesystem;
            terms[term.name].order = term.order;
          } else {
            //else simply add the record
            terms[term.name] = {
              name: term.name,
              visible: true,
              selectable: true,
              selected: "unselected",
              definition: term.definition,
              code: term.code,
              codesystem: term.codesystem,
              label: term.label,
              order: term.order,
            };
          }
          if (term.parent) {
            terms[term.name].parent = term.parent;
            //did we see this parent before?
            if (!terms[term.parent.name]) {
              //otherwise add it
              terms[term.parent.name] = {
                name: term.parent.name,
                visible: true,
                selectable: true,
                selected: "unselected",
              };
            }
            // if first child then add children array
            if (!terms[term.parent.name].children) {
              terms[term.parent.name].children = [];
            }
            // add the child
            terms[term.parent.name].children.push(terms[term.name]);
          }
          this.searchResultCount++;
        });
        this.terms = terms;
        this.applySelection(this.modelValue);
      }
    },
  },
  async mounted() {
    if (this.tableId) {
      const client = Client.newClient(this.schemaId);
      this.data = await client.fetchOntologyOptions(this.tableId);
    }
  },
  created() {
    if (this.options) {
      this.data = this.options;
      this.loading = false;
    } else {
      //override default
      this.limit = 100000;
    }
  },
};

function getSelectedChildNodes(term: Record<string, any>) {
  let result = [];
  if (term.selected === "complete") {
    result.push(term);
  } else if (term.children) {
    term.children.forEach((childTerm: Record<string, any>) =>
      result.push(...getSelectedChildNodes(childTerm))
    );
  }
  return result;
}
</script>

<docs>
<template>
  <div>
    <label>ontology array</label>
    <demo-item>
      <InputOntology
          id="input-ontology-1"
          v-model="value1"
          label="My ontology select"
          description="please choose your options in tree below"
          :options="options"
          :isMultiSelect="true"
      />
      <div>You selected: {{ value1 }}</div>
    </demo-item>

    <label>ontology array expanded</label>
    <demo-item>
      <InputOntology
          id="input-ontology-2"
          v-model="value2"
          label="My ontology select expanded"
          :showExpanded="true"
          description="please choose your options in tree below"
          :options="options"
          :isMultiSelect="true"
      />
      <div>You selected: {{ value2 }}</div>
    </demo-item>

    <label>ontology (single) with backend data</label>
    <demo-item>
      <InputOntology
          id="input-ontology-3"
          label="Ontology select with backend data"
          description="please choose your options in tree below"
          v-model="value3"
          tableId="Tag"
          schemaId="pet store"
      />
      <div>You selected: {{ value3 }}</div>
    </demo-item>

    <label>ontology array with backend data</label>
    <demo-item>
      <InputOntology
          id="input-ontology-4"
          label="Ontology select with backend data"
          description="please choose your options in tree below"
          v-model="value4"
          :isMultiSelect="true"
          tableId="Tag"
          schemaId="pet store"
      />
      <div>You selected: {{ value4 }}</div>
    </demo-item>

    <label>ontology  expanded</label>
    <demo-item>
      <InputOntology
          id="input-ontology-5"
          v-model="value5"
          label="My ontology select expanded"
          :showExpanded="true"
          description="please choose your options in tree below"
          :options="options"
          :isMultiSelect="true"
      />
      <div>You selected: {{ value5 }}</div>
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value1: null,
        value2: null,
        value3: null,
        value4: null,
        value5: null,
        options: [
          { name: 'pet' },
          { name: 'cat', parent: { name: 'pet' }, label: 'kitty' },
          { name: 'dog', parent: { name: 'pet' }, label: 'doggo' },
          { name: 'cattle' },
          { name: 'cow', parent: { name: 'cattle' } },
        ]
      };
    },
  };
</script>
</docs>
