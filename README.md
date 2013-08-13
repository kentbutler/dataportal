## Dataportal
__Synopsis:__  Dataportal is a Grails webapp which generically proxies either live or static datasets
           Provides access to registered datasets using a normalized JSON interface

### How it works

* A Dataport defines a source of data, and how its standard fields are represented 
  - "standard fields" are things like Uuid,Name,Description,Location,EventDate,CreateDate,
* Local Dataport sources are parsed and uploaded into a Mongo datastore
* Queries can be made against the Mongo datastore via the Dataport
* Example:
  - Pavel has a data file in JSON format
  
          file:///opt/data/ufo.json
          
    
    he creates a Dataport to represent it, which includes a map of the standard field names (currently this is done programmatically in BootStrap.groovy)
  
            new Dataport(contextName:'ufo', 
                        endpoint:'file:///opt/data/ufo.json', 
                        type: 'json', 
                        description:'UFO sighting data from a JSON file',
                        mapId: 'id',
                        mapName: 'location',
                        mapDescr: 'description',
                        mapLocation: 'location',
                        mapCreateDate: 'reported_at',
                        mapEventDate: 'sighted_at',
                        mapCreateDateFormat: 'YYYYMMdd',
                        mapEventDateFormat: 'YYYYMMdd'
                ).save(flush:true)


  - Pavel queries the dataset via the /dataportal/data/ JSON endpoint
  
          http://localhost:8080/dataportal/data/ufo
          
    which returns every single record in JSON

  - He applies search fields matching any field existing in the record set
  
          http://localhost:8080/dataportal/data/ufo?location="Yuma, AZ"
  
    and even wildcarded
    
          http://localhost:8080/dataportal/data/ufo?locationLike="AZ"
  
    and even fields that are not in the standard set
    
          http://localhost:8080/dataportal/data/ufo?locationLike="AZ"&shape="saucer"



### General Definitions

* Dataportal - this project, if anyone asks
* Dataport - an endpoint data source, i.e. a point to draw data out of - this could be online or a local CSV(TBD) or JSON file
* Dataset - the actual data retrieved from the Dataportal; responsible for rendering the data as JSON

### Data Definition

* Dataport consists of:
  - ContextName - the proxy name, i.e what you want to be able to query the endpoint on
  - Endpoint - the URL to be proxied  
   - endpoint if local should be a URL:  file:///opt/data/mydataset.json
   - endpoint if remote should be publicly accessible - if not, download to a local file or set up a tunnel
   - e.g. for local static data
   
           file:///path/to/file.json

  - Type - tell Dataportal what format to expect - currently supports 'json' (FUTURE - 'csv')
  - Description - 2048 chars of free text, describe endpoint
  - OutputDateFormat - format of date fields returned from queries, when possible to reformat
  
  - map* - provide the name of these fields in the corresponding endpoint data
    - when queried the dataset data will be represented with standard names
    - given the example above, the data originally defined as
    
                        mapId: 'id',
                        mapName: 'location',
                        mapDescr: 'description',
                        mapLocation: 'location',
                        mapCreateDate: 'reported_at',
                        mapEventDate: 'sighted_at',

      will be rendered from Dataportal as JSON
      
              {"uuid":"d8c48972-cd64-467a-bb6b-0746de9f58df",
              "name":" Columbia, MO",
              "description":"Man repts. son&apos;s bizarre sighting of small humanoid creature in back yard.  Reptd. in Acteon Journal, St. Louis UFO newsletter.",
              "location":" Columbia, MO",
              "createDate":"05/10/1995",
              "eventDate":"05/10/1995",
              "shape":"",
              "duration":"2 min."}
      

### Defining a Dataport
* programmatically - create a definition in BootStrap.groovy

* (TBD) dynamically - load the admin interface
          http://localhost:8080/dataportal/admin/index.html


### Querying Datasets from a Dataport
* send a request to /data/<contextName>  of a known Dataport
  - include any filtering params along with that
  
  - examples:
  
    Get all records with descriptions like %commercial%
    
           GET http://localhost:8080/dataportal/data/ufo?descriptionLike='commercial'
           
    Get all records with location like %AZ%
    
           GET http://localhost:8080/dataportal/data/ufo?locationLike='AZ'
           


