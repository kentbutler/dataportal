# Dataportal

## Overview

Dataportal is a Grails webapp which generically proxies either live or static datasets. It exposes your datasets via a URL endpoint and normalized JSON interface.
  
* static datasets - Dataportal loads JSON static files into MongoDB and makes them dynamically searchable.
  
* remote datasets - Dataportal offers a local proxy which will not be blocked by browser XSS security features. *[Field normalization TBD - currently it functions as a straight pass-through]*
    
------------------------------------------------------------------------------------------------
  
## <a id="example"></a>Examples



### Make a static JSON File web-searchable
     
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
     
     
### Proxy a remote endpoint
     
Pavel found a data service located on the web rooted at
    
          http:/website.com/infodata
  
He needs to use some of its content on his UI, but because it is from a different domain he has a cross-domain issue using Ajax to retrieve it. Also it is straight HTTP and his site is secured by HTTPS which puts the browser into mixed HTTP/HTTPS mode. It would be easier to front this endpoint with a local HTTPS call.
          
He [creates a Dataport](#create) to represent it.  He doesn't care about the standard field names, he wants a straight pass-through:
  
            new Dataport(contextName:'infodata', 
                        endpoint:'http:/website.com/infodata', 
                        type: 'json', 
                        description:'Information meta-data for buzzword scheme traversal'
                ).save(flush:true)
   
Now he can direct his Ajax calls to his local server

        https:/localserver:8443/dataportal/data/infodata
  
and it will pass requests through and back - no Fuss no Muss.
     
     
------------------------------------------------------------------------------------------------

## How it works
  
Dataportal defines a [Dataport](#defs) as a source of data - it tells Dataportal how to talk to the datasource   
  
It maps typical fields such as `uuid, name, description, location, eventDate, createDate`   
  
Local datasources are parsed and uploaded into a Mongo datastore to enable search functionality

------------------------------------------------------------------------------------------------
  
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

2. generating the data - Dataportal can fabricate a dataset for you, and that will create a Dataport in the process - see [this section](#gen) for details

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


------------------------------------------------------------------------------------------------

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


------------------------------------------------------------------------------------------------

## Building

### Requirements

Requires:
* Grails 2.2.3 - Ensure your `GRAILS_HOME` environment variable is set
* Java6/7 - Ensure your `JAVA_HOME` environment variable is set
  
### Get Latest

`git pull https://github.com/kentbutler/dataportal`
  
or
  
`git pull git@gitolite:/dataportal`

### Build for deployment

Open a command shell in the project directory. Execute:

        grails war 
  
Find `.war` output app in `target/` directory.


------------------------------------------------------------------------------------------------

## Running

### Requirements

Running the app additionally requires: 
  
* Mongo database installed and running
  
### Run locally 

Open a command shell in the project directory. Execute:
  
        grails -Dserver.port=8888 run-app --stacktrace
  
### Run from a server
  
Build the `war` file (see Building). Move `.war` file to a Java servlet container like Tomcat or Jetty.

### Verifying the install

As a quick test of installation, run the following command after starting the server.  This should return a short list of results:
  
        http://localhost:8888/dataportal/data/ufomini?locationLike=WA
   
If this does not return any results then the app probably cannot connect to Mongo.  Check your server logs.

### Verifying data generation

Try a simple case like

        http://localhost:8888/dataportal/gen/test?genSize=10&force=true&name=RANDTEXTmax8&status=RANDPICKactive|inactive|new&mothership=RANDTEXTmax15 

and verify results with

        http://localhost:8888/dataportal/data/test


------------------------------------------------------------------------------------------------

## Testing

### Run Functional Tests

Functional testing is dependent upon another plugin which must be installed. To install, download the following project adjacent to this application:
   
        git clone https://github.com/kentbutler/grails-json-rest-api.git

Modify your Dataportal app to use the plugin - open `grails-app/conf/BuildConfig.groovy` and uncomment the line

        //grails.plugin.location.jsonrest = "/opt/projects/grails-json-rest-api"

and correct the path for your local env.   
   
Now execute the tests:

        grails -Dserver.port=8888 test-app --stacktrace -functional

### Run Unit Tests

        grails test-app -unit

------------------------------------------------------------------------------------------------

## Todo

On the wish list:   

* UI for creating dataports
* generate data from a JSON file (describing the data layout)
* support for CSV

