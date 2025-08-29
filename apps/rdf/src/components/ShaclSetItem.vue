<!-- based on molgenis-viz "Accordion.vue" -->
<template>
  <div
    :id="`accordion-${shaclSet.name}`"
    :class="visible ? 'accordion visible' : 'accordion'"
  >
    <div class="accordion-heading">
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
        <span class="toggle-label">{{ shaclSetTitle }}</span>
      </button>
      <div>
        <div
          class="shacl-status fa"
          :aria-describedby="`${shaclSet.name}-status`"
          :class="{
            'fa-check': shaclStatus === 'VALID',
            'fa-times': shaclStatus === 'INVALID',
            'fa-spinner fa-spin': shaclStatus === 'RUNNING',
            'fa-exclamation-circle': shaclStatus === 'ERROR',
          }"
        />
        <span :id="`${shaclSet.name}-status`" class="visually-hidden">
          status: {{ shaclStatus }}
        </span>
      </div>
      <button
        type="button"
        class="run-shacl btn btn-outline-primary"
        :disabled="disabled"
        @click.prevent="runShacl()"
      >
        validate
      </button>
    </div>
    <div
      :id="`accordion-content-${shaclSet.name}`"
      class="accordion-content"
      :aria-labelledby="`accordion-toggle-${shaclSet.name}`"
      v-show="visible"
    >
      <p>Sources:</p>
      <ul>
        <li v-for="source in shaclSet.sources">
          <a :href="source" target="_blank">{{ source }}</a>
        </li>
      </ul>
      <MessageError v-if="error">{{ error }}</MessageError>
      <LayoutCard title="output">
        <div class="shacl-output-card">
          <pre>{{ shaclOutput }}</pre>
        </div>
      </LayoutCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { ChevronDownIcon } from "@heroicons/vue/24/outline";
// @ts-expect-error
import { LayoutCard, MessageError } from "molgenis-components";

const props = defineProps<{
  shaclSet: {
    name: string;
    description: string;
    version: string;
    sources: string[];
  };
}>();

const shaclSetTitle = computed<string>(() => {
  return (
    props.shaclSet.description + " (version: " + props.shaclSet.version + ")"
  );
});

const visible = ref<boolean>(false);

function toggleVisible() {
  visible.value = !visible.value;
}

const disabled = ref(false);
const error = ref<string>("");
const shaclOutput = ref("");
const shaclStatus = ref<string>("UNKNOWN");

async function runShacl() {
  disabled.value = true;
  error.value = "";
  shaclOutput.value = "";
  shaclStatus.value = "RUNNING";
  const res = await fetch("../api/rdf?validate=" + props.shaclSet.name);
  shaclOutput.value = await res.text();
  if (res.status !== 200) {
    shaclStatus.value = "ERROR";
    error.value = "Error (status code: " + res.status + ")";
  } else if (
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
</script>

<style lang="scss">
$border-radius: 6px;

.accordion {
  font-family: inherit;
  box-sizing: border-box;
  margin: 24px 0;
  border: 1px solid rgba(0, 0, 0, 0.125);
  border-radius: $border-radius;

  .accordion-heading {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    margin: 0;
    padding: 16px 12px;
    font-size: 14pt;
    color: #495057;
    background-color: rgba(0, 0, 0, 0.03);
    border-radius: $border-radius;

    .accordion-toggle {
      border: none;
      position: relative;
      background: none;
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

    .shacl-status {
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

.card {
  .shacl-output-card {
    max-height: 30em;
    overflow-y: scroll;
  }
  &.card-fullscreen {
    .shacl-output-card {
      max-height: 100vh;
    }
  }
}
</style>
