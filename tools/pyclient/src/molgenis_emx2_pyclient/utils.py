import pandas as pd
import csv
import io

def parseUrl(url: str):
    """Clean URL
    Standardise the host by removing trailing slash and make sure url
    starts with 'https://'.
    
    @param url string containing an URL
    
    @return string
    """
    host = url[0:len(url)-1] if url[-1:]=='/' else url
    if not host.startswith('https://'):
      return 'https://' + url
    
    return host


def toCsv(data: list):
   """To CSV
   Prepare dataset (i.e., recordset) into csv format for import into EMX2
   
   @param data data to import into EMX2 (list of dictionaries)
   
   @return comma-separated string
   """
   return pd.DataFrame(data) \
    .to_csv(index=False, quoting=csv.QUOTE_ALL, encoding='UTF-8')
   
def readFile(file: str):
    """Read File
    Read and import data from a file
    
    @param file location to a file
    """
    with open(file, 'rb') as stream:
        data = stream.read()
        stream.close()
    return data
   

def parseCsvExport(content: str):
    """Parse Csv Export
    Parse response from csv-file endpoint
    
    @param content csv-string returned from the csv-file endpoint
    
    @return list of dictionaries 
    """
    return pd.read_csv(io.StringIO(content), sep=',')