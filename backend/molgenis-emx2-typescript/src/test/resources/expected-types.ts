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

export interface ICategory_agg {
    count: number
}

export interface IOrder {
    orderId: string;
    pet?: IPet;
    quantity?: string;
    price?: number;
    complete?: boolean;
    status?: string;
}

export interface IOrder_agg {
    count: number
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

export interface IPet_agg {
    count: number
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

export interface ITag_agg {
    count: number
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

export interface IUser_agg {
    count: number
}


