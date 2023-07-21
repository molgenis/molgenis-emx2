import csv
import io

import pandas as pd


def parse_url(url: str) -> str:
    """Standardises the host by removing trailing slash and ensuring url starts with 'https://'.
    
    :param url: string containing a URL
    :type url: str
    :returns: string containing the standardised URL
    :rtype: str
    """
    host = url[:-1] if url.endswith('/') else url
    if not host.startswith('https://'):
        return 'https://' + host
    
    return host


def to_csv(data: list) -> None:
    """Prepares dataset (i.e., recordset) into csv format for import into EMX2.

    :param data: data to import into EMX2 (list of dictionaries)
    :type data: list
    :returns: nothing
    :rtype: NoneType
    """
    pd.DataFrame(data).to_csv(index=False, quoting=csv.QUOTE_ALL, encoding='UTF-8')

   
def read_file(file: str):
    """Reads and imports data from a file.
    
    @param file: path to a data file
    """
    with open(file, 'rb') as stream:
        data = stream.read()
        stream.close()
    return data
   

def parse_csv_export(content: str) -> pd.DataFrame:
    """Parses response from a csv file endpoint.
    
    :param content: csv-string returned from the csv-file endpoint
    :type content: string
    :returns: list of dictionaries in dataframe format
    :rtype: pd.DataFrame
    """
    return pd.read_csv(io.StringIO(content), sep=',')
