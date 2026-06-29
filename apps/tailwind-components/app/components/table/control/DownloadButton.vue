<template>
  <Dropdown label="Download" icon="download" icon-position="left">
    <section class="p-4 w-52">
      <h3>Download as:</h3>

      <ul>
        <li>
          <Button
            type="inline"
            size="tiny"
            :href="`/${schemaId}/api/csv/${tableId}`"
          >
            CSV
          </Button>
        </li>
        <li>
          <Button
            type="inline"
            size="tiny"
            :href="`/${schemaId}/api/zip/${tableId}`"
          >
            Zip
          </Button>
        </li>
        <li>
          <Button
            type="inline"
            size="tiny"
            :href="`/${schemaId}/api/excel/${tableId}`"
          >
            Excel
          </Button>
        </li>
        <li>
          <Button
            type="inline"
            size="tiny"
            :href="`/${schemaId}/api/jsonld/${tableId}`"
            target="_blank"
          >
            JSONLD
          </Button>
        </li>
        <li>
          <Button
            type="inline"
            size="tiny"
            :href="`/${schemaId}/api/ttl/${tableId}`"
            target="_blank"
          >
            TTL
          </Button>
        </li>
      </ul>
    </section>
  </Dropdown>
</template>

<script lang="ts" setup>
import Dropdown from "../../button/Dropdown.vue";

defineProps<{ schemaId: string; tableId: string }>();
</script>
