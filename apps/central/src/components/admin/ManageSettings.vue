<template>
  <div>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <h3>Manage system settings</h3>

    <table class="table table-hover table-bordered bg-white">
      <tr>
        <th style="width: 1px">
          <IconAction
            icon="plus"
            @click="handleCreateRequest"
            aria-label="Add"
          />
        </th>
        <th>key</th>
        <th>value</th>
      </tr>
      <tbody v-if="settings">
        <tr v-for="setting in settings" :key="setting.key">
          <td>
            <div style="display: flex">
              <IconAction
                icon="edit"
                @click="handleRowEditRequest(setting)"
                :aria-label="`Edit-${setting.key}`"
              />
              <IconDanger
                icon="trash"
                @click="handleRowDeleteRequest(setting)"
                :aria-label="`Remove-${setting.key}`"
              />
            </div>
          </td>
          <td>
            {{ setting.key }}
          </td>
          <td>
            {{ setting.value }}
          </td>
        </tr>
      </tbody>
    </table>
    <LayoutModal
      v-if="showModal"
      :title="modalTitle"
      :show="true"
      @close="showModal = false"
    >
      <template v-slot:body>
        <div>
          <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
          <LayoutForm>
            <InputString
              v-model="settingKey"
              label="key"
              id="settings-key"
              name="key"
              :readonly="isKeyReadOnly"
            />
            <InputText
              v-model="settingValue"
              label="setting value"
              id="settings-value"
              name="value"
              :readonly="isValueReadOnly"
            />
          </LayoutForm>
        </div>
      </template>
      <template v-slot:footer>
        <ButtonAlt @click="showModal = false">Cancel</ButtonAlt>
        <ButtonAction @click="actionFunction"
          >{{ settingActionLabel }}
        </ButtonAction>
      </template>
    </LayoutModal>
  </div>
</template>

<script>
import { request, gql } from "graphql-request";
import {
  IconAction,
  IconDanger,
  LayoutModal,
  LayoutForm,
  InputString,
  InputText,
  ButtonAlt,
  ButtonAction,
  MessageError,
} from "molgenis-components";

export default {
  name: "ManageSettings",
  components: {
    IconAction,
    IconDanger,
    LayoutModal,
    LayoutForm,
    InputString,
    InputText,
    ButtonAlt,
    ButtonAction,
    MessageError,
  },
  data() {
    return {
      settings: null,
      showModal: false,
      modalTitle: "",
      settingKey: "",
      settingValue: "",
      settingActionLabel: "",
      isKeyReadOnly: true,
      isValueReadOnly: true,
      graphqlError: null,
      actionFunction: null,
    };
  },
  methods: {
    async fetchSettings() {
      const resp = await request("graphql", `{_settings{key, value}}`);
      this.settings = resp._settings;
    },
    handleRowEditRequest(setting) {
      this.modalTitle = `Edit ${setting.key} setting`;
      this.settingActionLabel = "Edit Setting";
      this.settingKey = setting.key;
      this.settingValue = setting.value;
      this.isKeyReadOnly = true;
      this.isValueReadOnly = false;
      this.actionFunction = this.createSetting;
      this.showModal = true;
    },
    handleRowDeleteRequest(setting) {
      this.modalTitle = `Delete ${setting.key} setting`;
      this.settingActionLabel = "Delete Setting";
      this.settingKey = setting.key;
      this.settingValue = setting.value;
      this.isKeyReadOnly = true;
      this.isValueReadOnly = true;
      this.actionFunction = this.deleteSetting;
      this.showModal = true;
    },
    handleCreateRequest() {
      this.modalTitle = "Create new setting";
      this.settingActionLabel = "Create Setting";
      this.settingKey = "";
      this.settingValue = "";
      this.isKeyReadOnly = false;
      this.isValueReadOnly = false;
      this.actionFunction = this.createSetting;
      this.showModal = true;
    },
    async createSetting() {
      this.graphqlError = null;
      const createMutation = gql`
        mutation change($settings: [MolgenisSettingsInput]) {
          change(settings: $settings) {
            message
          }
        }
      `;

      const variables = {
        settings: {
          key: this.settingKey,
          value: this.settingValue,
        },
      };

      try {
        await request("graphql", createMutation, variables);
        this.fetchSettings();
        this.showModal = false;
      } catch (e) {
        this.graphqlError =
          e.response?.errors?.[0]?.message || "An error occurred.";
      }
    },
    async deleteSetting() {
      const deleteMutation = gql`
        mutation drop($settings: [DropSettingsInput]) {
          drop(settings: $settings) {
            message
          }
        }
      `;

      const variables = {
        settings: {
          key: this.settingKey,
        },
      };

      await request("graphql", deleteMutation, variables).catch((e) => {
        console.error(e);
      });
      this.fetchSettings();
      this.showModal = false;
    },
  },
  mounted() {
    this.fetchSettings();
  },
};
</script>
