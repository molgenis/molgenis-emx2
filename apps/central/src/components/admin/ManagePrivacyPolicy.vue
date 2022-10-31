<template>
  <div>
    <h3>Privacy policy</h3>
    <Spinner v-if="loading" />
    <div v-else>
      <InputRadio
        id="input-radio-2"
        label="Select policy level"
        :value="policyLevel"
        :options="['Level 1', 'Level 2', 'Level 3', 'Level 4', 'Custom']"
        :isClearable="false"
        @input="updatePolicyLevel"
      />
    </div>
    <InputText
      id="privacy-policy-text"
      v-model="policyText"
      placeholder="Enter the privacy policy"
      :readonly="policyLevel !== 'Custom'"
    />
    <button type="button" class="btn btn-primary" @click="save">Save</button>
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
  requestWithBody,
} from "molgenis-components";

const {
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
  },
  data() {
    return {
      policyText: null,
      policyLevel: null,
      loading: true,
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
      const query = gql`
        mutation createSetting(
          $privacyPolicyLevel: String
          $privacyPolicyText: String
        ) {
          updatePolicyLevel: createSetting(
            key: "${POLICY_LEVEL_KEY}"
            value: $privacyPolicyLevel
          ) {
            message
          }
          updatePolicyText: createSetting(
            key: "${POLICY_TEXT_KEY}"
            value: $privacyPolicyText
          ) {
            message
          }
        }
      `;

      const variables = {
        privacyPolicyLevel: this.policyLevel,
        privacyPolicyText: this.policyText,
      };

      const body = {
        operationName: "createSetting",
        query,
        variables,
      };

      await requestWithBody("graphql", body).catch((error) => {
        console.error(error);
      });
    },
  },
  mounted() {
    this.fetchPrivacyPolicy();
  },
};
</script>
