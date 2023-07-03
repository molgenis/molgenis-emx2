
def cleanUrl(url: str=None):
    """Clean URL
    Standardise the host by removing trailing slash
    
    @param url string containing an URL
    @return string
    """
    return url[0:len(url)-1] if url[-1:]=='/' else url
