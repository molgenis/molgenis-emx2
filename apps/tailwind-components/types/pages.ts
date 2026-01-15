import type { IFile } from "./types";

export type HtmlHeadingLevels = "H1" | "H2" | "H3" | "H4" | "H5" | "H6";

export interface Container {
  name: string;
  description?: string;
}

export interface Components {
  id?: string;
  block?: string[];
}

export interface Blocks {
  id: string;
  inContainer?: Container[];
  components?: Components[];
  enableFullScreenWidth?: boolean;
}

export interface Header extends Blocks {
  title: string;
  subtitle?: string;
  backgroundImage?: string;
}
export interface Section extends Blocks {}

export interface TextElements extends Components {
  text?: string;
  isCentered?: boolean;
}

export interface Headings extends TextElements {
  headingLevel: HtmlHeadingLevels;
}

export interface Img extends Components {
  src?: string;
  displayName?: string;
  image?: IFile;
  alt?: string;
  width?: string;
  height?: string;
  isCentered?: boolean;
}
