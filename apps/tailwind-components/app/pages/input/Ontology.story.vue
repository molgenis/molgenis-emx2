<script setup lang="ts">
import { ref } from "vue";

const ontologySchemaId = ref<string>("pet store");
const ontologyTableId = ref<string>("Tag");
const modelValue = ref({ name: "green" });
const modelValueArray = ref([{ name: "colors" }, { name: "mammals" }]);

const ontologySchemaId1 = ref<string>("CatalogueOntologies");
const ontologyTableId1 = ref<string>("ResourceTypes");
const modelValue1 = ref({ name: "Biobank" });
const modelValueArray1 = ref([{ name: "Biobank" }, { name: "Cohort study" }]);

const ontologySchemaId2 = ref<string>("CatalogueOntologies");
const ontologyTableId2 = ref<string>("Countries");
const modelValue2 = ref({ name: "Andorra" });
const modelValueArray2 = ref([{ name: "Andorra" }, { name: "Spain" }]);

const ontologySchemaId3 = ref<string>("CatalogueOntologies");
const ontologyTableId3 = ref<string>("Keywords");
const modelValue3 = ref({ name: "Maternal height" });
const modelValueArray3 = ref([
  { name: "Maternal height" },
  { name: "Country of cohort" },
]);

const ontologySchemaId4 = ref<string>("CatalogueOntologies");
const ontologyTableId4 = ref<string>("Keywords");
const modelValue4 = ref({ name: "Maternal height" });
const modelValueArray4 = ref([
  { name: "Maternal height" },
  { name: "Country of cohort" },
]);
</script>

<template>
  <InputTestContainer
    show-state
    showBlurCount
    showFocusCount
    v-slot="{ invalid, valid, disabled, onBlur, onFocus }"
  >
    <!--<div class="m-4">
      <FormField
        type="ONTOLOGY"
        id="test-ontology-input-id"
        label="Small tree ontology"
        description="Small ontologies are shown inline"
        v-model="modelValue"
        :ref-schema-id="ontologySchemaId"
        :ref-table-id="ontologyTableId"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onBlur"
      />
      <p class="mt-4 text-input-description">modelValue = {{ modelValue }}</p>
    </div>
    <div class="m-4">
      <FormField
        type="ONTOLOGY_ARRAY"
        label="Small tree ontology array"
        description="Small tree ontologies are shown inline"
        id="test-ontology-array-input-id"
        v-model="modelValueArray"
        :ref-schema-id="ontologySchemaId"
        :ref-table-id="ontologyTableId"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onFocus"
      />
      <p class="mt-4 text-input-description">
        modelValueArray = {{ modelValueArray }}
      </p>
    </div>
    <div class="m-4">
      <FormField
        type="ONTOLOGY"
        id="test-ontology-input-id1"
        label="Small flat ontology"
        description="Small ontologies are shown inline"
        v-model="modelValue1"
        :ref-schema-id="ontologySchemaId1"
        :ref-table-id="ontologyTableId1"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onBlur"
      />
      <p class="mt-4 text-input-description">modelValue = {{ modelValue1 }}</p>
    </div>
    <div class="m-4">
      <FormField
        type="ONTOLOGY_ARRAY"
        id="test-ontology-array-input-id1"
        label="Small flat ontology array"
        v-model="modelValueArray1"
        :ref-schema-id="ontologySchemaId1"
        :ref-table-id="ontologyTableId1"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onFocus"
      />
      <p class="mt-4 text-input-description">
        modelValueArray = {{ modelValueArray1 }}
      </p>
    </div>
    <div class="m-4">
      <FormField
        type="ONTOLOGY"
        id="test-ontology-input-id2"
        label="Large flat ontology"
        description="Large ontologies are shown as select"
        v-model="modelValue2"
        :ref-schema-id="ontologySchemaId2"
        :ref-table-id="ontologyTableId2"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onBlur"
      />
      <p class="mt-4 text-input-description">modelValue = {{ modelValue2 }}</p>
    </div>
    <div class="m-4">
      <FormField
        label="Large flat ontology array"
        description="Large ontologies are shown as select"
        type="ONTOLOGY_ARRAY"
        id="test-ontology-array-input-id2"
        :isArray="true"
        v-model="modelValueArray2"
        :ref-schema-id="ontologySchemaId2"
        :ref-table-id="ontologyTableId2"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onFocus"
      />
      <p class="mt-4 text-input-description">
        modelValueArray = {{ modelValueArray2 }}
      </p>
    </div>
    <div class="m-4">
      <FormField
        type="ONTOLOGY"
        label="Large tree ontology"
        description="Large ontologies are shown as select"
        id="test-ontology-input-id3"
        v-model="modelValue3"
        :ref-schema-id="ontologySchemaId3"
        :ref-table-id="ontologyTableId3"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onBlur"
      />
      <p class="mt-4 text-input-description">modelValue = {{ modelValue3 }}</p>
    </div>
    <div class="m-4">
      <FormField
        type="ONTOLOGY_ARRAY"
        label="Large tree ontology array"
        description="Large ontologies are shown as select"
        id="test-ontology-array-input-id3"
        :isArray="true"
        v-model="modelValueArray3"
        :ref-schema-id="ontologySchemaId3"
        :ref-table-id="ontologyTableId3"
        :invalid="invalid"
        :valid="valid"
        :disabled="disabled"
        @blur="onBlur"
        @focus="onFocus"
      />
      <p class="mt-4 text-input-description">
        modelValueArray = {{ modelValueArray3 }}
      </p>
    </div>-->
    <p>Large ontologies forced to show without select (outside of the form field)</p>
      <div class="m-4">
        <InputOntology
            id="test-ontology-input-id3"
            v-model="modelValue4"
            :ontology-schema-id="ontologySchemaId4"
            :ontology-table-id="ontologyTableId4"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            @blur="onBlur"
            @focus="onBlur"
            :force-list="true"
            :limit="5"
        />
        <p class="mt-4 text-input-description">modelValue = {{ modelValue4 }}</p>
      </div>
      <!---div class="m-4">
        <InputOntology
            id="test-ontology-array-input-id3"
            :isArray="true"
            v-model="modelValueArray4"
            :ontology-schema-id="ontologySchemaId4"
            :ontology-table-id="ontologyTableId4"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            @blur="onBlur"
            @focus="onFocus"
            :force-list="true"
            :limit="5"
        />
        <p class="mt-4 text-input-description">
          modelValueArray = {{ modelValueArray4 }}
        </p>
    </div-->
  </InputTestContainer>
</template>
