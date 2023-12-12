"""
Utility functions for the Molgenis EMX2 Pyclient package
"""


def parse_url(url: str) -> str:
    """Standardises the host by removing trailing slash
    and ensuring url starts with 'https://'.
    
    :param url: string containing a URL
    :type url: str
    :returns: string containing the standardised URL
    :rtype: str
    """
    host = url[:-1] if url.endswith('/') else url
    if not host.startswith('https://'):
        return 'https://' + host
    
    return host

   
def read_file(file_path: str) -> str:
    """Reads and imports data from a file.
    
    :param file_path: path to a data file
    :type file_path: str
    :returns: data in string format
    :rtype: str
    """
    with open(file_path, 'r') as stream:
        data = stream.read()
        stream.close()
    return data
