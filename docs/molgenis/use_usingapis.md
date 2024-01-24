# Using APIs

The EMX2 APIs can be used to various environments to retrieve, add, manipulate or replace data.



## Python

Data management of a Molgenis EMX2 server can be performed through the use of the Python client.
This client can be installed from the [PyPI repository](https://pypi.org/project/molgenis-emx2-pyclient/) and can be applied in a number of operations, such as 
- retrieving data from a table
- uploading data to a table


## R

R is a free software environment for statistical computing and graphics.
Download the latest version from [CRAN](https://cran.r-project.org/).
A more powerful graphical environment such as [RStudio](https://www.rstudio.com/) can be helpful to work with R.

### Prerequisites

The `httr` package is used to connect to EMX2.
Install and load this package as follows:

```
install.packages("httr")
library(httr)
```

### Download data

Table data can be retrieved using GET and should be converted to a data frame before it can be used. Here we assume that the data has 'View' permissions for anonymous users.

```
pets <- GET(url = "https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet")
petsdf <- read.csv(textConnection(content(pets, 'text', encoding = "UTF-8")))
```

Make a plot of pet weight and print the first row to check if everything looks right.

```
plot(petsdf$weight)
petsdf[1,]
```

Result:

```
   name category photoUrls    status tags weight
1 pooky      cat        NA available         9.4
```

### Alter data

Let's change pet weight from (supposedly) kilogram to grams by multiplying by one thousand. The new data should be converted back to CSV to prepare for upload.

```
petsdf$weight = petsdf$weight * 1000
newPetWeight <- capture.output(write.csv(petsdf))
```

### Upload data
The data can be uploaded to replace the original data using POST. Here we assume that anonymous users do not have 'Edit' permissions, so we must authenticate using a security token.
```
token <- "<your token here>"
header <- add_headers("x-molgenis-token" = token)
POST(url = "https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet", body = newPetWeight, config = header)
```

If everything went well, a HTTP status code 200 (meaning [OK](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes)) is returned:

```
Response [https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet]
  Date: 2023-04-05 09:38
  Status: 200
  Content-Type: text/csv
  Size: 1 B
```

Re-download the data to see if the weight value has been updated in the database.

```
newPets <- GET(url = "https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet")
newPetsdf <- read.csv(textConnection(content(pets, 'text', encoding = "UTF-8")))
newPetsdf[1,]
```

Result:

```
   name category photoUrls    status tags weight
1 pooky      cat        NA available        9400
```

### Define new tables
todo

## Command line

todo
