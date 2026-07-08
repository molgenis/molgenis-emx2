export interface TypeDetailUnion {
  kind: "union";
  options: string[];
}

export interface TypeDetailObject {
  kind: "object";
  members: Record<string, string>;
}

export interface TypeDetailArray {
  kind: "array";
  elementType: string;
}

export type TypeDetail = TypeDetailUnion | TypeDetailObject | TypeDetailArray;

export interface ComponentPropMeta {
  name: string;
  type: string;
  default: string | undefined;
  required: boolean;
  description: string;
  typeDetail?: TypeDetail | null;
}

export interface ComponentEventMeta {
  name: string;
  type: string;
  description: string;
}

export interface ComponentSlotMeta {
  name: string;
  type: string;
  description: string;
}

export interface ComponentMeta {
  componentName: string;
  filePath: string;
  props: ComponentPropMeta[];
  events: ComponentEventMeta[];
  slots: ComponentSlotMeta[];
}

export interface ComponentMetaMap {
  [componentName: string]: ComponentMeta;
}
