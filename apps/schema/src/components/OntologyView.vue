<template>
  <tr
    :style="ontology.drop ? 'text-decoration: line-through' : ''"
    class="hoverContainer">
    <td>
      <div
        :id="
          ontology.name !== undefined ? ontology.name.replaceAll(' ', '_') : ''
        "
        style="display: inline-block; text-transform: none !important"
        :style="ontology.drop ? 'text-decoration: line-through' : ''">
        {{ ontology.name }}
        <span v-if="ontology.semantics" class="small">
          (<a
            :href="purl"
            target="_blank"
            v-for="purl in ontology.semantics"
            :key="purl"
            >{{ purl.substring(purl.lastIndexOf("/") + 1) }}</a
          >)
        </span>
      </div>
      <TableEditModal
        v-if="isManager"
        v-model="ontology"
        :schema="schema"
        @update:modelValue="$emit('update:modelValue', ontology)" />
      <IconDanger
        v-if="isManager"
        @click="deleteOntology(ontology)"
        icon="trash"
        class="hoverIcon" />
    </td>
    <td>
      {{
        ontology.description ? ontology.description : "No description available"
      }}
    </td>
  </tr>
</template>

<style>
.hoverIcon {
  visibility: hidden;
}

.hoverContainer:hover .hoverIcon {
  visibility: visible;
}
</style>

<script>
import { IconAction, IconDanger } from "molgenis-components";
import columnTypes from "../columnTypes.js";
import TableEditModal from "./TableEditModal.vue";

export default {
  components: {
    TableEditModal,
    IconAction,
    IconDanger,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
    },
    schema: {
      type: Object,
      required: true,
    },
    schemaNames: {
      type: Array,
      required: true,
    },
    isManager: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      ontology: {},
      columnTypes,
    };
  },
  methods: {
    validateName() {
      if (!this.name) {
        return "Ontology name is required";
      }
      if (this.schema.tables.filter(t => t.name === this.name).length > 1) {
        return "Ontology name must be unique within schema";
      }
    },
    deleteOntology(ontology) {
      if (!ontology.oldName) {
        this.$emit("delete");
        return;
      }
      if (!ontology.drop) {
        //need to do deep set otherwise vue doesn't see it
        ontology.drop = true;
      } else {
        ontology.drop = false;
      }
      this.$emit("update:modelValue", ontology);
    },
  },
  created() {
    this.ontology = this.modelValue;
  },
  emits: ["update:modelValue", "delete"],
};
</script>
