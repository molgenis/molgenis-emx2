// Generated (on: 2024-08-22T10:45:50.328661) from Generator.java for schema: pet store

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

export interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: {
    name: string;
  };
}

export interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface ICategory {
  name: string;
}

export interface IOrder {
  orderId: string;
  pet?: IPet;
  quantity?: string;
  price?: number;
  complete?: boolean;
  status?: string;
}

export interface IPet {
  name: string;
  category: ICategory;
  photoUrls?: string[];
  status?: string;
  tags?: IOntologyNode[];
  weight: number;
  orders?: IOrder[];
}

export interface ITag {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: ITag;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: ITag[];
}

export interface IUser {
  username: string;
  firstName?: string;
  lastName?: string;
  picture?: IFile;
  email?: string;
  password?: string;
  phone?: string;
  userStatus?: number;
  pets?: IPet[];
}


