package ncc.owfdemo

import ncc.owfdemo.data.JsonDataLoader
import ncc.owfdemo.data.LocalFileLoader
import ncc.owfdemo.data.RandomGenerator
import wslite.http.HTTPMethod

class DataportService {

    /**
     * Upload data for the given Dataport if it is required.  Doing this allows new local Datasets to be queried.
     * Without doing this, there will be no data to query. 
     * @param dataport
     * @param params
     * @return
     */
    def load(Dataport dataport, def params) {
        // See if data is already loaded -- else load to our intermediate datastore
        // RULE: Any LOCAL dataset i.e. file-based should load only once, and search vs. Mongo
        if (dataport.isLocalDatasource() && !dataport.loaded) {
            log.debug "================== loading data ========================"
            doLoad dataport, params
        }
    }
        
    //** worker bee **
    def doLoad(Dataport dataport, def params) {
        log.debug "reading data from dataport [$dataport.contextName] endpoint [$dataport.endpoint]"
        // GUARDS
        if (!dataport) {
            log.error "null datapoint given to service; exiting"
            throw IllegalArgumentException ("no dataport provided to service")
        }
        if (!dataport.endpoint) {
            log.error "invalid dataport [${dataport.contextName}]- no endpoint defined"
            throw IllegalArgumentException ("invalid dataport - no endpoint defined")
        }
        
        // check out endpoint
        //   if URL: 
        //      if local, read
        //      else load remote content
        //   else FAIL
        def data
        
        if (dataport.isLocalDatasource()) {
            data = new LocalFileLoader(log).load(dataport)
        }
        else {
            log.error "We should currently not be loading a remote data source - FUTURE may have different requirements"
            throw new RuntimeException("Currently we should not be loading a remote data source")
        }
        
        log.debug "================== warehousing dataport data ========================\n$dataport"
        
        if (dataport.type == 'json') {
            try {
                new JsonDataLoader(log).parseAndStore(dataport, data)
                dataport.loaded = Boolean.TRUE
                dataport.save(flush:true)
    
            } catch (Exception e) {
                log.error "Error parsing and storing data: $e"
            }
        }
        else if (dataport.type == 'csv') {
            //TODO Parse CSV
            log.error "Dataport type [${dataport.type}] is currently Invalid"
        }
        else {
            //return data
            log.error "Dataport type [${dataport.type}] is currently Invalid"
        }
    }
    
    
    /**
     * I think this will go away when we actually implement this capability.
     * @param data
     * @return
     */
    def loadRemoteData (Dataport data) {
        //def file = new FileOutputStream(url.tokenize("/")[-1])
        //def out = new BufferedOutputStream(file)
        
        //TODO All we are doing here is redirecting the search to our offsite source
        //   -- but we are providing the normalizing of field names to give client
        //   -- a consistent view 
        
        //TODO Not done!!!
        def out = new StringWriter()
        log.debug "------ loading remote data from: ${data.endpoint} ------"
        out << new URL(data.endpoint).openStream()
        out.close()
        
        return out.toString()
    }
    
    
    /**
     * Return a list of items for the given Dataport and params.
     * Searches the Dataset for the Dataport and returns a list of Dataset results.
     * 
     * @param dataport
     * @param params
     * @return
     */
    def retrieveItems (Dataport dataport, def params, def request) throws Exception {
        if (!dataport || !dataport.contextName) {
            throw new IllegalArgumentException("Unsupported: search on undefined Dataport")
        }
        
        if (!dataport.isLocalDatasource()) {
            return remoteRetrieve (dataport, params, request)
        }
        
        log.debug "================== searching warehouse [${dataport.contextName}] ========================"
        // Now Apply search parameters, dynamically create method string based on input params
        //   First we need to remove some common params
        
        def searchKeys = RequestUtils.extractSearchKeys(params)
        def finderName = RequestUtils.createFinderName(searchKeys)
        def searchVals  = RequestUtils.extractSearchValues(params)
        
        // Convert search params for arg input        
        // Note we want to give full param set as last arg so sort/max flags are in there for GORM
        Object[] args = new Object[searchVals .size()+1]
        def cnt = 0

		log.debug "### Input Search params: $params"
        log.debug "### Finder name: $finderName"
		log.debug "### Search Keys: $searchKeys"
		log.debug "### Search Vals: $searchVals "
		
        searchVals.each { key, val ->
            args[cnt++] = val
        }
        args[searchVals .size()] = params
		
		log.debug "### Object[] args (last arg always params map): $args"
		
        // SEARCH 
		// this cannot support the persistence strategy using optional fields in a Map since we need . notation to 
		//    search them
        //return (finderName == "list" ? Dataset.list([:]) :  // Mongo no like params!! Make Mongo do bad things!!
        //                              Dataset.metaClass.invokeStaticMethod(Dataset, finderName, args) )
		if (finderName == "list") {
            def results 
			Dataset.withCollection(dataport.contextName) {
                results = Dataset.list(params)
            }
            return results
		}
		
		// use criteria in order to compare nested values
		def keystr, isLike
		
		log.debug "Searching collection [${dataport.contextName}]"
		
		def results
     	Dataset.withCollection(dataport.contextName) {
			def c = Dataset.createCriteria()
			results = c.list {
				searchVals.each { key, val ->
					keystr = key.toString() 
					isLike = keystr.endsWith("Like")
					
					if (isLike) {
						keystr = keystr[0..-5]
					}
					if (!Dataport.STD_FIELDS.contains(keystr)) {
						keystr = "fields.$keystr"
					}
					if (isLike) {
						like keystr, val
					}
					else {
						eq keystr, val
					}
				}
			}
		}
		 
        return results
    }
    
    
    def remoteRetrieve(Dataport dataport, def params, def request) throws Exception {
        log.debug "Remotely accessing Dataport:: ${dataport}"
        
        def searchParams = RequestUtils.extractSearchValues(params)
        log.debug "Searching with params: $searchParams"
        def output
        
        // Issue request vs. remote source
        /*
        withRest(url: dataport.endpoint) { 
            //TODO One day make this dynamic
            def response
            log.debug "Request method:: ${request.method}"
            if (request.method == 'GET') {
                response = delegate.get(path: '', query: searchParams)
            }
            else if (request.method == 'POST') {
                delegate.httpClient.sslTrustAllCerts = true
                
                response = delegate.post() {
                    charset "UTF-8"
                    searchParams
                }
            }
            else if (request.method == 'PUT') {
                response = delegate.put(path: '', query: searchParams)
            }

            log.debug response.json
            if (response?.json) {
                output = response.json
            }
        }
        */
        def rsp
        
        withRest(url: dataport.endpoint) {
            log.debug "Request method:: ${request.method}"
            log.debug "Endpoint: ${dataport.endpoint}"
            /*
            if (request.method == 'GET') {
                //delegate.httpClient.followRedirects = true
                response = delegate.get(path: '', 
                                        query: searchParams,
                                        connectTimeout: 10000,
                                        readTimeout: 30000,
                                        followRedirects: true
                                        )
                
                log.debug response.json
                if (response?.json) {
                    output = response.json
                }
            }
            */
            if (request.method == 'POST' || request.method == 'GET') {
                
                // Our little secret!  <wink/>
                delegate.httpClient.sslTrustAllCerts = true
                
                def req = new wslite.http.HTTPRequest()
                req.method = (request.method == 'POST' ? HTTPMethod.POST : HTTPMethod.GET)
                req.sslTrustAllCerts  = true
                
                def finalUrl = dataport.endpoint + "?"
                searchParams.each { k, v ->
                    finalUrl += "$k=${java.net.URLEncoder.encode(v)}&"
                }
                req.url = new java.net.URL(finalUrl)
                log.debug "${request.method}ing URL: $finalUrl"
                
                rsp = delegate.httpClient.execute(req) 
                log.debug "Response type: ${rsp.class.name}"
                log.debug "Response:: $rsp"
                log.debug "Response content:: ${rsp.contentAsString}"
                //output = rsp?.data && rsp?.data instanceof String ? new String(rsp.data) : rsp.data
                return rsp
            }
            else if (request.method == 'PUT') {
                
                //  TODO - this branch replaced by previous?? 
                
                //response = delegate.put(path: '', query: searchParams)
                rsp = delegate.put() {
                    charset "UTF-8"
                    (java.util.Map) searchParams
                }
                log.debug rsp.json
                if (rsp?.json) {
                    output = rsp.json
                }
                //!!RETURN HERE
                return output
            }

        }
    }
    
    /**
     * Generate JSON for the given input params to the given output file
     * @param params
     * @param outFile
     * @return
     */
    def generate (def params, File outFile) {
        
        // CREATE DATAPORT
        def dataport = new Dataport(contextName: params.contextName,
                                endpoint: outFile.toURI().toString(),
                                type:'json',
                                // Must use the output format because we are parsing and rendering again as part of
                                //   generation; we need to be able to parse the Dataport default output format
                                mapCreateDateFormat: Dataport.DEFAULT_OUTPUT_DATE_FORMAT,
                                mapEventDateFormat: Dataport.DEFAULT_OUTPUT_DATE_FORMAT
                                )
        def createDate = dataport.getOutputDateFormatter().format(new Date())
        dataport.description = "Data generated at your request on $createDate"
        dataport.save(flush:true)

        log.debug "Created Dataport: $dataport"
        
        // Look for fields - expecting format:
        //   fieldName=value|RANDTEXTmaxNN|RANDNUMmaxNN|RANDDATE|[val1|val2..|valn]
        // where [val1|val2..|valn] are a list to choose randomly from
        //  and maxNN in RANDTEXT and RANDNUM tokens means restrict number of output chars to the given length
            
        def fields=[:], gen = new RandomGenerator()
        def fieldNames = RequestUtils.extractSearchKeys(params, CONTROL_FLAGS) // exclude control flags
		
        fieldNames.each { field ->
            def type = params."$field"
			if (type?.size() > 0) {
				log.debug "Registering field [$field] as type '$type'"
				fields[field] = gen.getGenerator(type)
			}
			else {
				log.warn "Not generating data for column [$field] - no type specified"
			}
        }
        
        // And ensure the standards are all represented
        log.trace "Checking standard fields"
        for (j in 0..Dataport.STD_FIELDS.size()-1) {
            if (!fields[Dataport.STD_FIELDS[j]] && Dataport.STD_FIELDS[j] != Dataport.STD_UUID) {
                // Field not set - default, except for UUID which will get generated
                log.trace "Adding generator for std field [${Dataport.STD_FIELDS[j]}]"
                fields [Dataport.STD_FIELDS[j]] =
                     Dataport.STD_FIELDS[j].endsWith("Date") ?
                        gen.getGenerator(RandomGenerator.TYPE_DATE) :  // the only non-textfields among the standards
                        gen.getGenerator()    // everything else is ok using a text generator
            }
        }

        // Now simply loop through number of fields....
        log.trace "Creating output writer"
        def dataset, writer = outFile.newWriter()
        
        def numToGen
        try {
           numToGen = params[GEN_SIZE_TOKEN] ? Integer.parseInt(params[GEN_SIZE_TOKEN]) : MAX_GENERATED
        } catch (Exception e) {
            log.error "Generate request asked for an unparsable [${params[GEN_SIZE_TOKEN]}] number of records]"
            numToGen = MAX_GENERATED
        }
        log.debug "Generating [$numToGen] data records"
        for (i in 1..numToGen) {
            
            // CREATE DATASET
            def fname, newval
            Map<String,Object> props = new HashMap<String,Map>()
            
            dataset = new Dataset()
            log.trace "----- Creating new Dataset -----"
            
            // Populate standard fields...
            fields.each { key, fieldGen ->
                fname = key.toString()
                if (Dataport.STD_FIELDS.contains(fname)) {
                    log.trace "  Generating value for [$fname]"
                    // Recall we are essentially writing into JSON, so we need our dates output as
                    //   if they already lay in a file somewhere
                    if (fname == Dataport.STD_EVENT_DATE) {
                        props[fname] = dataport.getEventDateFormatter().format(fieldGen.getVal())
                    }
                    else if (fname == Dataport.STD_CREATE_DATE) {
                        props[fname] = dataport.getCreateDateFormatter().format(fieldGen.getVal())
                    }
                    else {
                        props[fname] = fieldGen.getVal()
                    }
                }
            }
            props[Dataport.STD_UUID] = UUID.randomUUID().toString()
            
            // Assign fields this way b/c using
            //      dataset['f'] = val
            //   causes the Mongo plugin to invoke the 'putAt' method which creates a DB session
            //   and tries to persist the object in an incomplete state, which bombs.
            dataset.properties = props
            
            // Now add extra fields
            fields.each { key, fieldGen ->
                fname = key.toString()
                if (!Dataport.STD_FIELDS.contains(fname) && !CONTROL_FLAGS.contains(fname)) {
                    // generate value
                    dataset.fields[fname] = fieldGen.getVal()
                }
            }
            log.trace "Rendering dataset:\n $dataset"
            def jsonStr = dataset.toJson(dataport).toString()
            log.trace "------------- Writing JSON: -----------------\n $jsonStr"
            writer.write(jsonStr)
            
            if (i != numToGen) {
                writer.write(',\n')
            }
        }
        
        // CLOSE OUT FILE
        try {
            writer.close()
        }
        catch (Exception e) {
            def msg = "Error finalizing output JSON file: $e"
            log.error msg
            throw e
        }

        return dataport
    }
    
    static final int MAX_GENERATED = 10
    static final String GEN_SIZE_TOKEN = 'genSize'
    static final String FORCE_FLAG = 'force'
    
	static final CONTROL_FLAGS = [GEN_SIZE_TOKEN, FORCE_FLAG]
	
}


