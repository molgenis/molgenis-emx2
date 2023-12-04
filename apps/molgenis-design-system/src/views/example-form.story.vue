<template>
  <Story title="Examples/Patient search form" :layout="{ type: 'grid', width: '75%' }">
    <Variant title="Full example">
      <Form id="patientSearch" title="Search for patients">
        <template v-slot:context>
          <p class="leading-relaxed text-gray-600">
            There are over 50k+ patients in the registry
            <!-- <TextTooltip
              label="registry"
              description="The XYZ Registry for A,B, and C"
            />. -->
            The data comes from organisations (e.g., universities, hospitals,
            medical practices, governments, etc.) in many countries. Apply
            one or more of the following filters to create a subset of the
            registry.
          </p>
        </template>
        <template v-slot:inputs>
          <InputText
            id="user-account"
            label="Username"
            :disabled="true"
            value="Test"
          >
            <template v-slot:description>
              <p>
                In order to run the search, we need to verify your account.
              </p>
            </template>
          </InputText>
          <FormInput>
            <InputSelect
              id="mirrorSelection"
              label="Select a mirror"
              :options="[
                {name: 'NL.001'},
                {name: 'NL.002'},
                {name: 'UK.001'},
                {name: 'UK.002'},
              ]"
              option_label="name"
            >
              <template v-slot:description>
                <p class="leading-relaxed text-gray-600">
                  Choose a mirror to run the search. It may be useful to switch
                  mirrors during busy periods.
                </p>
              </template>
            </InputSelect>
          </FormInput>
          <FormInput>
            <InputText
              id="patient-search-all"
              type="search"
              label="Search database"
              :required="true"
            >
              <template v-slot:description>
                <p class="leading-relaxed text-gray-600">
                  Use an identifier ('A-1244') or search pattern ('^(A-[0-9]{4,})')
                </p>
              </template>
            </InputText>
          </FormInput>
          <FormInput>
            <InputOptionGroup
              id="countryInput"
              type="checkbox"
              name="countries"
              title="Select Countries"
              :required="true"
              :data="countries"
              row_id="rowId"
              row_label="country"
              row_value="code"
              row_checked="default"
            >
              <template v-slot:description>
                <p class="leading-relaxed text-gray-600">
                  Data comes from many countries. If you would like to limit the
                  results to one or more countries, then select them below.
                </p>
              </template>
            </InputOptionGroup>
          </FormInput>
          <FormInput>
            <InputOptionGroup
              id="searchStrictness"
              :required="true"
              type="radio"
              name="search-strictness"
              title="Set strictness level"
              :data="[
                {
                  id: 'level-1',
                  name: 'Basic',
                  definition: 'Fuzzy matching methods will be used'
                },
                {
                  id: 'level-2',
                  name: 'Standard',
                  definition: 'Our standard filters will be applied',
                  checked: true
                },
                {
                  id: 'level-3',
                  name: 'Strict',
                  definition: 'Strict pattern matching rules will be followed'
                },
              ]"
              row_id="id"
              row_label="name"
              row_value="name"
              row_description="definition"
              row_checked="checked"
            >
              <template v-slot:description>
                <p class="leading-relaxed text-gray-600">
                  Adjust the level of specificity of the search parameters.
                </p>
              </template>
            </InputOptionGroup>
          </FormInput>
        </template>
        <template v-slot:actions>
          <div class="flex flex-col gap-3">
            <Button type="submit" context="secondary" label="Search" />
            <Button type="reset" context="outline" label="Reset" />
          </div>
        </template>
      </Form>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import Form from "../components/layouts/forms.vue";
import FormInput from "../components/layouts/FormInput.vue";
import InputText from "../components/inputs/InputText.vue";
import InputOptionGroup from "../components/inputs/InputOptionGroup.vue";
import InputSelect from "../components/inputs/InputSelect.vue";
import Button from "../components/inputs/Buttons.vue";

// import TextTooltip from "../components/text/TextTooltip.vue";

const countries = [
  { rowId: "AU", country: "Australia", code: "AU", default: true },
  { rowId: "BE", country: "Belgium", code: "BE" },
  { rowId: "CA", country: "Canada", code: "CA" },
  { rowId: "MX", country: "Mexico", code: "MX" },
  { rowId: "NL", country: "Netherlands", code: "NL", default: true },
  { rowId: "UK", country: "United Kingdom", code: "UK" },
  { rowId: "US", country: "United States", code: "US" },
];
</script>