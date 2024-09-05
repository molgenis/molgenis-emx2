# Ontology creation logbook

## MediaType
1. Download all `.csv` files from https://www.iana.org/assignments/media-types/media-types.xhtml & store in **single empty** folder.
2. `cd` to folder with the `.csv` files.
3. Run `mkdir output` in this folder.
4. Run the following command: 
    ```bash
   echo "name,definition,codesystem,code,ontologyTermURI" > output/MediaType.csv && \
   awk -F',' 'NR>1 {print $1","$1",IANA,,https://www.iana.org/assignments/media-types/"$2}' *.csv >> output/MediaType.csv
   ```