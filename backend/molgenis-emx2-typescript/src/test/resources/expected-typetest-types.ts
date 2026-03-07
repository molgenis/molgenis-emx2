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

export interface IComponent {
    name: string;
    parts?: IPart[];
}

export interface IComponent_agg {
    count: number
}

export interface IPart {
    name: string;
    weight?: number;
}

export interface IPart_agg {
    count: number
}

export interface IProduct {
    name: string;
    components?: IComponent[];
}

export interface IProduct_agg {
    count: number
}

export interface ITypeTest {
    id: string;
    testUuid: string;
    testUuidNillable?: string;
    testString: string;
    testStringNillable?: string;
    testBool: boolean;
    testBoolNillable?: boolean;
    testInt: number;
    testIntNillable?: number;
    testLong: string;
    testLongNillable?: string;
    testDecimal: number;
    testDecimalNillable?: number;
    testText: string;
    testTextNillable?: string;
    testDate: string;
    testDateNillable?: string;
    testDatetime: string;
    testDatetimeNillable?: string;
    testPeriod: any;
    testPeriodNillable?: any;
}

export interface ITypeTest_agg {
    count: number
}


