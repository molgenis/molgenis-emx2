import { inject, provide, type InjectionKey } from "vue";
import { navigateTo } from "#imports";
import { getPrimaryKey } from "../utils/getPrimaryKey";
import { buildRefHref } from "../utils/displayUtils";

export interface RecordNavigation {
  navigateToRecord: (
    schemaId: string,
    tableId: string,
    row: Record<string, any>,
    refSchemaId?: string
  ) => Promise<void>;
}

const RECORD_NAVIGATION_KEY: InjectionKey<RecordNavigation> =
  Symbol("recordNavigation");

function createDefaultNavigation(): RecordNavigation {
  return {
    async navigateToRecord(schemaId, tableId, row, refSchemaId) {
      const schema = refSchemaId || schemaId;
      const key = await getPrimaryKey(row, tableId, schema);
      const href = buildRefHref(schemaId, tableId, refSchemaId, key);
      navigateTo(href);
    },
  };
}

export function provideRecordNavigation(
  navigation?: Partial<RecordNavigation>
): RecordNavigation {
  const nav = { ...createDefaultNavigation(), ...navigation };
  provide(RECORD_NAVIGATION_KEY, nav);
  return nav;
}

export function useRecordNavigation(): RecordNavigation {
  return inject(RECORD_NAVIGATION_KEY, createDefaultNavigation());
}
