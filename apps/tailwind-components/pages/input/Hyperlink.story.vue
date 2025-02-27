<template>
  <p>
    The <code>Hyperlink Component</code> enables you to use the String component
    with the added validation of the input being a hyperlink.
  </p>
  <FormFields
    id="form-hyperlink"
    schemaId="schema-hyperlink"
    :metadata="{
      label: 'Hyperlink',
      id: 'hyperlink',
      tableType: 'my kind of table',
      columns: [
        {
          id: 'input-hyperlink',
          columnType: 'HYPERLINK',
          label: 'Input a hyperlink',
        },
      ],
    }"
    v-model="model"
    :chapters="chapters"
    :visibleMap="visibleMap"
    :errorMap="errorMap"
    :activeChapterId="activeChapterId"
  />
</template>

<script setup lang="ts">
const model = ref({ "input-hyperlink": null });

const activeChapterId = ref(null);
const errorMap = ref({});

const chapters = ref([{
  label: "_top",
  id: "_scroll_to_top",
  columns: [
    {
      id: "input-hyperlink",
      columnType: "EMAIL",
      label: "Input an email address",
    },
  ],
  isActive: false,
  errorCount: 0,
}]);
const visibleMap = ref({
  "input-hyperlink": true,
});

</script>
