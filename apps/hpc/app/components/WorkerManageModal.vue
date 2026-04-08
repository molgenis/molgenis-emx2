<template>
  <Modal
    v-model:visible="visibleModel"
    :title="`Manage ${workerId}`"
    max-width="max-w-2xl"
  >
    <div class="p-5 space-y-5">
      <Message v-if="error" id="worker-manage-error" invalid>
        {{ error }}
      </Message>

      <!-- Secret revealed after issue -->
      <div v-if="revealedSecret" class="space-y-3">
        <Message id="worker-credential-issued" valid>
          Credential issued. Store this secret now — it cannot be retrieved
          later.
        </Message>

        <div class="rounded-md border border-color-theme bg-content p-3">
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <p
                class="text-[11px] font-semibold text-table-column-header uppercase tracking-wider mb-1"
              >
                Secret
              </p>
              <code class="block text-xs leading-relaxed break-all">{{
                revealedSecret.secret
              }}</code>
            </div>
            <Button
              type="primary"
              size="tiny"
              class="shrink-0"
              @click="copySecretValue"
            >
              {{ copyState === "secret" ? "Copied" : "Copy" }}
            </Button>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div class="rounded-md border border-color-theme bg-content p-3">
            <div class="flex items-start justify-between gap-2 mb-2">
              <p
                class="text-[11px] font-semibold text-table-column-header uppercase tracking-wider"
              >
                Write .secret
              </p>
              <Button type="outline" size="tiny" @click="copyWriteCommand">
                {{ copyState === "write" ? "Copied" : "Copy" }}
              </Button>
            </div>
            <pre
              class="text-xs whitespace-pre-wrap break-all leading-relaxed"
              >{{ secretWriteSnippet(revealedSecret.secret) }}</pre
            >
          </div>

          <div class="rounded-md border border-color-theme bg-content p-3">
            <div class="flex items-start justify-between gap-2 mb-2">
              <p
                class="text-[11px] font-semibold text-table-column-header uppercase tracking-wider"
              >
                Daemon Config
              </p>
              <Button type="outline" size="tiny" @click="copyConfig">
                {{ copyState === "config" ? "Copied" : "Copy" }}
              </Button>
            </div>
            <pre
              class="text-xs whitespace-pre-wrap break-all leading-relaxed"
              >{{ daemonConfigSnippet(workerId) }}</pre
            >
          </div>
        </div>
      </div>

      <!-- Running Jobs -->
      <div v-if="activeJobs.length">
        <p
          class="text-xs font-semibold text-table-column-header uppercase tracking-wider mb-2"
        >
          Running Jobs
        </p>
        <div class="space-y-1.5">
          <div
            v-for="job in activeJobs"
            :key="job.id"
            class="flex items-center justify-between gap-3 rounded-md border border-color-theme bg-content px-3 py-2"
          >
            <NuxtLink
              :to="`/jobs/${job.id}`"
              class="text-xs font-mono text-button-outline hover:text-button-outline-hover underline underline-offset-2"
            >
              {{ job.id?.substring(0, 8) }}
            </NuxtLink>
            <StatusBadge :status="job.status" />
          </div>
        </div>
      </div>

      <!-- Credentials -->
      <div>
        <p
          class="text-xs font-semibold text-table-column-header uppercase tracking-wider mb-2"
        >
          Credentials
        </p>

        <p
          v-if="loadingCredentials"
          class="text-xs text-definition-list-term py-3 text-center"
        >
          Loading...
        </p>

        <p
          v-else-if="!credentials.length"
          class="text-xs text-definition-list-term py-3 text-center"
        >
          No credentials issued.
        </p>

        <div v-else class="space-y-2">
          <div
            v-for="cred in credentials"
            :key="cred.id"
            class="flex items-center justify-between gap-3 rounded-md border border-color-theme px-3 py-2"
            :class="cred.status === 'ACTIVE' ? 'bg-green-500/5' : 'bg-content'"
          >
            <div class="flex items-center gap-3 min-w-0">
              <span
                class="inline-flex items-center px-2 py-0.5 rounded-full border text-xs"
                :class="credentialStatusClass(cred.status)"
              >
                {{ cred.status }}
              </span>
              <code
                class="text-xs bg-content/50 px-1.5 py-0.5 rounded"
                :title="cred.id"
              >
                {{ shortCredentialId(cred.id) }}
              </code>
            </div>
            <div
              class="flex items-center gap-3 text-xs text-definition-list-term shrink-0"
            >
              <span>{{ formatDate(cred.created_at) }}</span>
              <Button
                v-if="cred.status === 'ACTIVE'"
                type="outline"
                size="tiny"
                :disabled="actionBusy"
                @click="onRevoke(cred.id)"
              >
                Revoke
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2 p-3">
        <Button
          v-if="!hasActive && !revealedSecret"
          type="primary"
          size="small"
          :disabled="actionBusy || loadingCredentials"
          @click="onIssue"
        >
          Issue New Credential
        </Button>
        <Button
          v-if="revealedSecret"
          type="primary"
          size="small"
          @click="visibleModel = false"
        >
          Done
        </Button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  fetchWorkerCredentials,
  issueWorkerCredential,
  revokeWorkerCredential,
  type WorkerCredential,
} from "../composables/useHpcApi";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import StatusBadge from "./StatusBadge.vue";
import { formatDate } from "../utils/jobs";

const props = defineProps<{
  visible: boolean;
  workerId: string;
  activeJobs: Array<{ id: string; status: string }>;
  initialSecret?: string | null;
}>();

const emit = defineEmits<{
  (e: "update:visible", value: boolean): void;
}>();

const visibleModel = computed({
  get: () => props.visible,
  set: (val: boolean) => emit("update:visible", val),
});

const credentials = ref<WorkerCredential[]>([]);
const loadingCredentials = ref(false);
const actionBusy = ref(false);
const error = ref<string | null>(null);
const revealedSecret = ref<{ secret: string } | null>(null);
const copyState = ref<"secret" | "write" | "config" | null>(null);

const hasActive = computed(() =>
  credentials.value.some((c) => c.status === "ACTIVE")
);

async function loadCredentials() {
  loadingCredentials.value = true;
  try {
    credentials.value = await fetchWorkerCredentials(props.workerId);
  } catch {
    credentials.value = [];
  } finally {
    loadingCredentials.value = false;
  }
}

async function onIssue() {
  actionBusy.value = true;
  error.value = null;
  try {
    const result = await issueWorkerCredential(props.workerId, {});
    revealedSecret.value = { secret: result.secret };
    await loadCredentials();
  } catch (e: unknown) {
    error.value =
      e instanceof Error ? e.message : "Failed to issue credential.";
  } finally {
    actionBusy.value = false;
  }
}

async function onRevoke(credentialId: string) {
  if (!confirm(`Revoke credential ${credentialId.substring(0, 12)}...?`))
    return;
  actionBusy.value = true;
  error.value = null;
  try {
    await revokeWorkerCredential(props.workerId, credentialId);
    await loadCredentials();
  } catch (e: unknown) {
    error.value =
      e instanceof Error ? e.message : "Failed to revoke credential.";
  } finally {
    actionBusy.value = false;
  }
}

function credentialStatusClass(status: string): string {
  if (status === "ACTIVE")
    return "border-green-500/40 bg-green-500/10 text-green-700";
  if (status === "REVOKED")
    return "border-red-500/40 bg-red-500/10 text-red-700";
  if (status === "EXPIRED")
    return "border-yellow-500/40 bg-yellow-500/10 text-yellow-800";
  return "border-color-theme bg-content text-definition-list-term";
}

function shortCredentialId(idVal: string): string {
  if (!idVal) return "-";
  if (idVal.length <= 12) return idVal;
  return `${idVal.slice(0, 8)}...${idVal.slice(-4)}`;
}

function secretWriteSnippet(secret: string): string {
  return `printf '%s' '${secret}' > .secret && chmod 600 .secret`;
}

function daemonConfigSnippet(workerId: string): string {
  return [
    "emx2:",
    '  base_url: "https://emx2.example.org"',
    `  worker_id: "${workerId}"`,
    '  worker_secret_file: ".secret"',
  ].join("\n");
}

async function copyText(value: string, key: "secret" | "write" | "config") {
  try {
    await navigator.clipboard.writeText(value);
    copyState.value = key;
    setTimeout(() => {
      if (copyState.value === key) copyState.value = null;
    }, 1500);
  } catch {
    // fallback
    const area = document.createElement("textarea");
    area.value = value;
    area.style.position = "fixed";
    area.style.opacity = "0";
    document.body.appendChild(area);
    area.select();
    document.execCommand("copy");
    document.body.removeChild(area);
    copyState.value = key;
    setTimeout(() => {
      if (copyState.value === key) copyState.value = null;
    }, 1500);
  }
}

function copySecretValue() {
  if (revealedSecret.value) copyText(revealedSecret.value.secret, "secret");
}
function copyWriteCommand() {
  if (revealedSecret.value)
    copyText(secretWriteSnippet(revealedSecret.value.secret), "write");
}
function copyConfig() {
  copyText(daemonConfigSnippet(props.workerId), "config");
}

// Load credentials and reset state when modal opens
watch(
  () => props.visible,
  (open) => {
    if (open) {
      error.value = null;
      revealedSecret.value = props.initialSecret
        ? { secret: props.initialSecret }
        : null;
      copyState.value = null;
      loadCredentials();
    }
  },
  { immediate: true }
);
</script>
