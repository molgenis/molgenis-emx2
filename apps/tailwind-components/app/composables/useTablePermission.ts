import { computed, type Ref } from "vue";
import type { TableType } from "../../../metadata-utils/src/types";
import type { ISession, ITablePermission } from "../../types/types";

/**
 * Derives the table level and row level permissions for a single table from
 * the session. Ontology tables are always viewable.
 */
export function useTablePermission(
  session: Ref<ISession | null>,
  schemaId: string,
  tableId: string,
  tableType?: TableType
) {
  const permission = computed<ITablePermission | undefined>(() =>
    session.value?.tablePermissions?.[schemaId]?.find(
      (permission) => permission.id === tableId || permission.name === tableId
    )
  );

  const canView = computed(
    () => permission.value?.canView || tableType === "ONTOLOGIES" || false
  );
  const canInsert = computed(() => permission.value?.canInsert || false);
  const canUpdate = computed(() => permission.value?.canUpdate || false);
  const canDelete = computed(() => permission.value?.canDelete || false);
  const canEdit = computed(
    () => canInsert.value || canUpdate.value || canDelete.value
  );
  const isRowLevel = computed(() => permission.value?.isRowLevel || false);
  const userRoles = computed(() => session.value?.roles?.[schemaId] ?? []);

  return {
    permission,
    canView,
    canInsert,
    canUpdate,
    canDelete,
    canEdit,
    isRowLevel,
    userRoles,
  };
}
