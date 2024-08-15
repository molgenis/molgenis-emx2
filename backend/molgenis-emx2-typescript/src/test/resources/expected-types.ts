// Generated (on: 2024-08-15T16:39:47.962256) from Generator.java for schema: pet store

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

export interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: string;
}

export interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface ICategory {
  name :string;
}

export interface IPet {
  name :string;
  category :ICategory;
  photoUrls :string[];
  status :string;
  tags :IOntologyNode[];
  weight :number;
  orders :IOrder[];
}

export interface ITag {
  order :number;
  name :string;
  label :string;
  parent :ITag;
  codesystem :string;
  code :string;
  ontologyTermURI :string;
  definition :string;
  children :ITag[];
}

export interface IOrder {
  orderId :string;
  pet :IPet;
  quantity :number;
  price :number;
  complete :boolean;
  status :string;
}

export interface IUser {
  username :string;
  firstName :string;
  lastName :string;
  picture :IFile;
  email :string;
  password :string;
  phone :string;
  userStatus :number;
  pets :IPet[];
}


