
interface IPrepListboxData {
  data: object[];
  valueKey: string;
  labelKey?: string;
}

export function prepareListboxData({data, valueKey, labelKey}: IPrepListboxData) {
  const testValue = data[0];
  
  if (typeof testValue === "object") {
    
  } else {
    
  } 
}