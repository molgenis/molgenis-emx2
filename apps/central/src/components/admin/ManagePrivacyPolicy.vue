<template>
  <div>
    <h3>Privacy policy</h3>
    <Spinner v-if="loading" />
    <div v-else>
      <InputRadio
        id="input-radio-2"
        label="Select policy level"
        :modelValue="policyLevel"
        :options="levelOptions"
        :isClearable="false"
        @update:modelValue="updatePolicyLevel"
      />
      <InputText
        id="privacy-policy-text"
        v-model="policyText"
        placeholder="Enter the privacy policy"
        :readonly="policyLevel !== CUSTOM"
      />
    </div>
    <button type="button" class="btn btn-primary" @click="save">Save</button>
    <MessageSuccess v-if="successMessage">
      {{ successMessage }}
    </MessageSuccess>
  </div>
</template>

<script>
import { gql } from "graphql-request";
import {
  InputText,
  InputRadio,
  Spinner,
  privacyConstants,
  request,
  MessageSuccess,
} from "molgenis-components";

const {
  LEVEL_1,
  LEVEL_2,
  LEVEL_3,
  LEVEL_4,
  CUSTOM,
  POLICY_LEVEL_KEY,
  POLICY_TEXT_KEY,
  PREFABS,
} = privacyConstants;
export default {
  name: "ManagePrivacyPolicy",
  components: {
    InputText,
    InputRadio,
    Spinner,
    MessageSuccess,
  },
  data() {
    return {
      policyText: null,
      policyLevel: null,
      loading: true,
      successMessage: "",
      levelOptions: [LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4, CUSTOM],
      CUSTOM,
    };
  },
  methods: {
    updatePolicyLevel(newValue) {
      this.policyLevel = newValue;
      this.policyText = PREFABS[newValue];
    },
    async fetchPrivacyPolicy() {
      const response = await request("graphql", `{_settings{key, value}}`);
      const policyLevel = response._settings.find(
        (item) => item.key === POLICY_LEVEL_KEY
      )?.value;
      const policyText = response._settings.find(
        (item) => item.key === POLICY_TEXT_KEY
      )?.value;
      if (!policyLevel) {
        this.policyText = PREFABS[LEVEL_4];
        this.policyLevel = LEVEL_4;
      } else if (policyLevel === CUSTOM) {
        this.policyText = policyText;
        this.policyLevel = policyLevel;
      } else {
        this.policyText = PREFABS[policyLevel];
        this.policyLevel = policyLevel;
      }
      this.loading = false;
    },
    async save() {
      this.successMessage = "";
      const query = gql`
        mutation change($settings: [MolgenisSettingsInput]) {
          change(settings: $settings) {
            message
          }
        }
      `;

      const variables = {
        settings: [
          { key: POLICY_LEVEL_KEY, value: this.policyLevel },
          { key: POLICY_TEXT_KEY, value: this.policyText },
        ],
      };

      await request("graphql", query, variables)
        .then(() => {
          this.successMessage = `Privacy policy successfully saved with privacy level: ${this.policyLevel}`;
        })
        .catch((error) => {
          console.error(error);
        });
    },
  },
  mounted() {
    this.fetchPrivacyPolicy();
  },
};
</script>
