<template>
  <MessageBox type="error" v-if="error" class="study-list-error">
    <p><strong>Unable to retrieve studies</strong></p>
    <p>{{ error }}</p>
  </MessageBox>
  <MessageBox
    type="error"
    v-else-if="!data && !error"
    class="study-list-error"
  >
    <div class="p-2">
      <p>
        No studies are available. To add studies, follow the steps outlines
        below.
      </p>
      <ol>
        <li>Navigate to the <a href="../tables/#/Studies">Studies table</a></li>
        <li>
          Create a new record by clicking the add new record button (<PlusIcon
            class="heroicons"
          />) located in the top left corner of the table.
        </li>
        <li>
          In the form that appears, enter as much information about the study as
          necessary. Make sure all required fields are completed.
        </li>
        <li>
          When you have finished, click "save". Your study will be added as a
          new record in the table.
        </li>
      </ol>
      <p>Repeat the process for each study.</p>
    </div>
  </MessageBox>
  <ul class="study" v-for="study in data" :key="study.id">
    <li>
      <a
        class="study-element study-url"
        :href="
          '../tables/#/Studies?_filter=title&title=' +
          study[labelsColumn] +
          '&_view=record&_limit=1'
        "
        >{{ study[labelsColumn] }}</a
      >
    </li>
  </ul>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { MessageBox } from "molgenis-viz";
import { ArrowDownTrayIcon, PlusIcon } from "@heroicons/vue/24/outline";

const props = defineProps<{
  table: string;
  labelsColumn?: string;
  fileColumn: string;
}>();

interface StudyProperties {
  id: string;
  title: string;
}

const error = ref<Error | null>(null);
const data: Record<string, StudyProperties>[] = ref([]);

async function getStudies() {
  const query = gql`query {
    ${props.table} {
      ${props.labelsColumn || ""}
    }
  }`;
  const response = await request("../api/graphql", query);
  data.value = response[props.table];
}

getStudies().catch((err) => {
if (!err.response.errors.length) {
  error.value = err;
} else {
  error.value = err.response.errors[0].message;
}
});
</script>

<style lang="scss">
.study-list-error {
  .heroicons {
    @include setIconSize(24px);
  }
}

.study-list {
  list-style: none;
  padding: 0;
  margin: 0 auto;
  $border-radius: 8pt;

  .heroicons {
    @include setIconSize(24px);
    path {
      stroke-width: 2;
    }
  }

  .study {
    display: grid;
    grid-template-columns: 1fr repeat(2, 0.5fr) 0.2fr;
    justify-content: center;
    align-items: stretch;
    box-shadow: $box-shadow;
    box-sizing: border-box;
    margin-bottom: 2em;
    padding: 0;
    margin: 0;
    margin-bottom: 2em;
    border-radius: $border-radius;

    .study-element {
      padding: 0.75em 1.2em;
      margin: 0;
      background-color: $red-050;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .study-name {
      justify-content: flex-start;
      border-radius: $border-radius 0 0 $border-radius;
    }

    .study-url {
      justify-content: flex-start;
      background-color: $blue-800;
      color: $gray-050;
      border-radius: 0 $border-radius $border-radius 0;

      &:hover,
      &:focus {
        background-color: $yellow-400;
        color: $blue-800;
      }
    }
  }
}
</style>
