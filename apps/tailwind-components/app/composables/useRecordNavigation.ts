import { inject, provide, type InjectionKey } from "vue";
import { navigateTo } from "#imports";
import { getPrimaryKey } from "../utils/getPrimaryKey";
import { buildRefHref } from "../utils/displayUtils";
import { buildNestedRecordPath } from "../utils/recordPath";
import fetchMetadata from "./fetchMetadata";

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
      let nestedPath: string | null = null;
      try {
        nestedPath = buildNestedRecordPath(
          await fetchMetadata(schema),
          tableId,
          row
        );
      } catch (error) {
        console.error(
          `Could not build a nested record path for ${schema}/${tableId}, trying the flat path, `,
          error
        );
      }
      if (nestedPath) {
        await navigateTo(nestedPath);
        return;
      }
      try {
        const key = await getPrimaryKey(row, tableId, schema);
        await navigateTo(buildRefHref(schemaId, tableId, refSchemaId, key));
      } catch (error) {
        console.error(
          `Could not resolve the primary key of ${schema}/${tableId}, cannot navigate to the record, `,
          error
        );
      }
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
