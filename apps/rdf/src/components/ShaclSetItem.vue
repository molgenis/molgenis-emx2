<!-- based on molgenis-viz "Accordion.vue" -->
<template>
  <div
    :id="`accordion-${shaclSet.name}`"
    :class="visible ? 'accordion visible' : 'accordion'"
  >
    <h3 class="accordion-heading">
      <button
        type="button"
        :id="`accordion-toggle-${shaclSet.name}`"
        class="accordion-toggle"
        :aria-controls="`accordion-content-${shaclSet.name}`"
        :aria-expanded="visible"
        @click="toggleVisible()"
      >
        <ChevronDownIcon
          :class="visible ? 'toggle-icon rotated' : 'toggle-icon'"
        />
        <span class="toggle-label">{{ shaclSetTitle() }}</span>
      </button>
      <i class="shacl-status fa" :class="icon" />
      <button
        type="button"
        class="run-shacl btn btn-outline-primary"
        :disabled="disabled"
        @click.prevent="runShacl()"
      >
        validate
      </button>
    </h3>
    <section
      :id="`accordion-content-${shaclSet.name}`"
      class="accordion-content"
      :aria-labelledby="`accordion-toggle-${shaclSet.name}`"
      v-show="visible"
    >
      Sources:
      <ul>
        <li v-for="source in shaclSet.sources">
          <a :href="source" target="_blank">{{ source }}</a>
        </li>
      </ul>
      <LayoutCard title="SHACL output">
        <pre>{{ shaclOutput }}</pre>
      </LayoutCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import { watch, ref } from "vue";
import { ChevronDownIcon } from "@heroicons/vue/24/outline";
import { LayoutCard } from "molgenis-components";

const props = defineProps({
  shaclSet: {
    type: Array<String, String>,
    required: true,
  },
});

function shaclSetTitle() {
  return (
    props.shaclSet.description + " (version: " + props.shaclSet.version + ")"
  );
}

const visible = ref(false);
function toggleVisible() {
  visible.value = !visible.value;
}

const disabled = ref(false);
const shaclOutput = ref(null);
const shaclStatus = ref(null);
async function runShacl() {
  disabled.value = true;
  shaclOutput.value = null;
  shaclStatus.value = "RUNNING";
  const res = await fetch("../api/rdf?validate=" + props.shaclSet.name);
  shaclOutput.value = await res.text();
  if (
    shaclOutput.value
      .substring(0, 100)
      .includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.")
  ) {
    shaclStatus.value = "VALID";
  } else {
    shaclStatus.value = "INVALID";
  }
  disabled.value = false;
}

const icon = ref(null);
function updateIcon() {
  switch (shaclStatus.value) {
    case "VALID":
      icon.value = "fa-check";
      return;
    case "INVALID":
      icon.value = "fa-times";
      return;
    case "RUNNING":
      icon.value = "fa-spinner fa-spin";
      return;
    default:
      icon.value = "fa-question";
      return;
  }
}

watch(shaclStatus, updateIcon);
</script>

<style lang="scss">
$border-radius: 6px;

.accordion {
  font-family: inherit;
  box-sizing: border-box;
  margin: 12px 0;
  border: 1px solid rgba(0, 0, 0, 0.125);
  border-radius: $border-radius;

  .accordion-heading {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    margin: 0;
    padding: 8px 8px;
    font-size: 14pt;
    color: #495057;
    background-color: rgba(0, 0, 0, 0.03);
    border-radius: $border-radius;

    .accordion-toggle {
      border: none;
      position: relative;
      background: none;
      background-color: none;
      margin: 0;
      padding: 0;
      cursor: pointer;
      font-size: inherit;
      text-align: left;
      color: currentColor;
      display: flex;
      justify-content: flex-start;
      align-items: center;
      width: 100%;

      $icon-size: 24px;
      .toggle-label {
        display: inline-block;
        width: calc(100% - $icon-size);
        word-break: break-word;
      }

      .toggle-icon {
        width: $icon-size;
        height: $icon-size;
        transform: rotate(0);
        transform-origin: center;
        transition: transform 0.4s ease-in-out;

        &.rotated {
          transform: rotate(180deg);
        }
      }
    }

    i.shacl-status {
      position: relative;
      margin: 0 10px;
    }
  }

  .accordion-content {
    margin: 0 0 12px 0;
    box-sizing: content-box;
  }

  &.visible {
    .accordion-heading {
      border-radius: $border-radius $border-radius 0 0;
    }
    .accordion-content {
      padding: 0 12px;
    }
  }
}
</style>
