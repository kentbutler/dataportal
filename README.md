# Dataportal

__Synopsis:__  Dataportal is a Grails webapp which generically proxies either live or static datasets. It exposes your datasets via a URL endpoint and normalized JSON interface.

For static datasets the advantage is obvious - Dataportal brings them to life in webspace.  

For remote datasets, Dataportal offers a local proxy which will not be blocked by browser XSS security features. *[Remote proxy feature TBD]*

## How it works

Dataportal defines a [Dataport](#defs) as a source of data - it tells Dataportal and how to talk to the datasource   

It maps typical fields such as `uuid, name, description, location, eventDate, createDate`   

Local datasources are parsed and uploaded into a Mongo datastore to enable search functionality

<a id="example"></a>**Example:**  
   
Pavel has a data file in JSON format located at
  
          file:/opt/data/ufo.json
          
He [creates a Dataport](#create) to represent it, including a map of the standard field names 
  
            new Dataport(contextName:'ufo', 
                        endpoint:'file:/opt/data/ufo.json', 
                        type: 'json', 
                        description:'UFO sighting data 1994-1995',
                        mapId: 'id',
                        mapName: 'location',
                        mapDescr: 'description',
                        mapLocation: 'location',
                        mapCreateDate: 'reported_at',
                        mapEventDate: 'sighted_at',
                        mapCreateDateFormat: 'YYYYMMdd',
                        mapEventDateFormat: 'YYYYMMdd'
                ).save(flush:true)

Pavel queries the dataset via the `/data` endpoint
  
          http://localhost:8080/dataportal/data/ufo
          
which returns every single record in JSON.

He applies **search fields** from any of the standard fields
  
          http://localhost:8080/dataportal/data/ufo?location="Yuma, AZ"
  
and even wildcarded
    
          http://localhost:8080/dataportal/data/ufo?locationLike="AZ"
  
and even fields that are **not in the standard set**
    
          http://localhost:8080/dataportal/data/ufo?locationLike="AZ"&shape="saucer"

and he didn't have to write any code to get his data online!
   

## Definitions    
 
### General Definitions

* **Dataportal** - this project, if anyone asks   
* **Dataport** - an endpoint data source, i.e. a point to draw data out of - this could be online or a local CSV(TBD) or JSON file   
* **Dataset** - the actual data retrieved from the Dataportal; responsible for rendering the data as JSON   

### <a id="input"></a>Input Formats

The following are known data formats and can be parsed by Dataportal.

#### JSON
Dataports representing JSON datasets should have type
    type = 'json'

and be formatted as a flat list of JSON objects.  Example:

    {"f1":"v1", "f2":"v2"},
    {"f1":"v1", "f2":"v2"},
    {"f1":"v1", "f2":"v2"}

#### CSV
TBD.

### <a id="defs"></a>Data Definition

#### <a id="dataportDef"></a>Dataport

A `Dataport` represents any source of data accessible via URL and in a known data format.

        class Dataport {

           	String contextName
          	String endpoint
          	String type
          	String description
          	String outputDateFormat = DEFAULT_OUTPUT_DATE_FORMAT
          	Boolean loaded = Boolean.FALSE  

            String mapUuid = STD_UUID
            String mapName = STD_NAME
            String mapDescr = STD_DESCRIPTION 
          	String mapLocation = STD_LOCATION
          	String mapCreateDate = STD_CREATE_DATE
          	String mapEventDate = STD_EVENT_DATE
          	String mapCreateDateFormat = "yyyyMMdd"
          	String mapEventDateFormat = "yyyyMMdd"

           	static final String NAME_PATTERN = "[a-zA-Z]{1}[a-zA-Z0-9]*"

            static constraints = {
                contextName(blank:false, nullable:false, maxSize:32, matches:NAME_PATTERN)
                endpoint(blank:false,nullable:false,maxSize:1024)
                description(blank:false,nullable:false,maxSize:2048)
            }  
   

where

- contextName - the proxy name, i.e what you want to be able to query the endpoint on

- endpoint - the URL to be proxied 
  - example of a local dataset:  
        file:/opt/data/mydataset.json

  - remote datasets should be publicly accessible - if not, download to a local file or set up a tunnel

- type - what format the data is in - currently supports 'json' (FUTURE - 'csv')

- description - 2048 chars of free text, describe endpoint

- outputDateFormat - format of date fields returned from queries, when possible to reformat - default is 'mm/DD/yyyy'
  
- map* - used to normalize the JSON interface; maps critical fields from the dataset's weird names to Dataportal standard names

    - only used in parsing - i.e. use the standard names when querying, and expect the standard names in returned JSON

Given the [example](#example) mapping above, the data originally defined as
    
        {"sighted_at": "19950510", 
         "reported_at": "19950510", 
         "location": " Columbia, MO", 
         "shape": "", 
         "duration": "2 min.", 
         "description": "Man repts. son&apos;s bizarre sighting of small humanoid creature in back yard.  Reptd. in Acteon Journal, St. Louis UFO newsletter."}   

will be rendered from Dataportal as the JSON
   
      {"uuid":"d8c48972-cd64-467a-bb6b-0746de9f58df",
       "name":" Columbia, MO",
       "description":"Man repts. son&apos;s bizarre sighting of small humanoid creature in back yard.  Reptd. in Acteon Journal, St. Louis UFO newsletter.",
       "location":" Columbia, MO",
       "createDate":"05/10/1995",
       "eventDate":"05/10/1995",
       "shape":"",
       "duration":"2 min."}   

Note that a UUID has been generated for the record.

#### <a id="datasetDef"></a>Dataset

A `Dataset` represents the actual data behind a `Dataport`.  Datasets are mapped directly to Mongo and can be considered a warehouse.

    class Dataset {
    
        String id
        String uuid
        String name
        String description
        String location
        String createDate
        String eventDate
        Map<String,String> fields = new HashMap<String,String>()   

where

* id - internal Mongo-GORM identifier - do not tamper!

* uuid - generated by Dataportal if none given

* name, description, location, createDate, eventDate - represent standard fields pulled in from the original datasource

* fields - all the additional fields from the original datasource

Upon sending the first query to a new `Dataport`, Dataportal will populate the warehouse with the Dataset contents.  


## <a id="create"></a>Defining a Dataport

There are several ways to define a Dataport:

1. programmatically - create a definition in BootStrap.groovy. See the [example](#example) for a sample constructor

2. generating the data - Dataportal can fabricate a dataset for you, and that will create a Dataport in the process - see this section for details

3. (TBD) dynamically - load the admin interface
          http://localhost:8080/dataportal/admin/index.html


## <a id="query"></a>Querying Datasets from a Dataport

To query data from an existing Dataport, send a request to `/data/<contextName>` 

- include any filtering params on the URL
  
- **examples:**
  
Get all records with descriptions like %commercial%
    
       GET http://localhost:8080/dataportal/data/ufo?descriptionLike='commercial'
           
Get all records with location like %AZ%
    
       GET http://localhost:8080/dataportal/data/ufo?locationLike='AZ'
           
Get all record with location like %AZ% and shape like 'cigar'

       GET http://localhost:8080/dataportal/data/ufo?locationLike='AZ'&shape=cigar

Search is case-insensitive.  'Like' searches should **not** include wildcard characters in field values.


## <a id="gen"></a>Generating Datasets

A Dataport with fabricated records may be created by issuing a request to the `/gen` endpoint. 

Example:

    http://localhost:8080/dataportal/gen/het?genSize=1000&force=true \ 
    &name=RANDTEXTmax8&status=RANDPICKactive|inactive|new \        
    &mothership=RANDTEXTmax15   
    where:   
   
* `/gen/het`  specifies Dataset name as `/gen/<setName>`   
* `genSize=[num]`  number of records to generate   
* `force=[true|false]`  delete and recreate dataset   
* `<fieldname>=<type>[minN][maxM]` - generate a datapoint with type:   

    - `RANDTEXT` - random text   
    - `RANDINT` - a random whole number (long actually)
    - `RANDFLOAT` - a random float (double really)
    - `RANDPICK[val1|val2]` - pipe-separate list of tokens to randomly choose
    - `RANDDATE` - random date file, formatted as is the default outputFormat
    - `minN` - apply this to ranged types TEXT,INT,FLOAT to specify min value   
    - `maxN` - apply this to ranged types TEXT,INT,FLOAT to specify max value


## For Developers

### Get Latest

`git pull git@gitolite:/dataportal`

### Build 

`grails compile`   
*or**   

`grails war`

### Run locally 

`grails -Dserver.port=8888 run-app --stacktrace`

### Run Functional Tests

`grails -Dserver.port=8888 test-app --stacktrace -functional`

### Run Unit Tests

`grails test-app -unit`

## Todo
On the wish list:   

* admin interface
* when generating data, auto-stuff the data into Mongo vs. just creating the datafile
* support for CSV
* support for remote data endpoints  (need good examples)
