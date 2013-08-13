package ncc.owfdemo

import ncc.owfdemo.data.JsonDataLoader
import ncc.owfdemo.data.LocalFileLoader
import ncc.owfdemo.data.RandomGenerator

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
            dataport.loaded = Boolean.TRUE
            dataport.save(flush:true)
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
            new JsonDataLoader(log).parseAndStore(dataport, data)
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
     * I think this will go away when we actually implement this function.
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
    def retrieveItems (Dataport dataport, def params) {
        if (!dataport) {
            throw new IllegalArgumentException("Unsupported: search on undefined Dataport")
        }
        log.debug "================== searching warehouse [${dataport.contextName}] ========================"
        // Now Apply search parameters, dynamically create method string based on input params
        //   First we need to remove some common params
        
        def searchKeys = RequestUtils.extractSearchKeys(params)
        def finderName = RequestUtils.createFinderName(searchKeys)
        def searchParams = RequestUtils.extractSearchValues(params)
        
        // Convert search params for arg input        
        // Note we want to give full param set as last arg so sort/max flags are in there for GORM
        Object[] args = new Object[searchParams.size()+1]
        def cnt = 0
		
		log.debug "### Input Search params: $params"
        log.debug "### Finder name: $finderName"
		log.debug "### Search Keys: $searchKeys"
		log.debug "### Search Vals: $searchParams"
		
        searchParams.each {
            args[cnt++] = it
        }
        args[searchParams.size()] = params
		
		log.debug "### Object[] args (last arg always params map): $args"
		
        // SEARCH     
		if (finderName == "list") finderName = "findAllBy"
		   
        return (finderName == "list" ? Dataset.list([:]) :  // Mongo no like params!! Make Mongo do bad things!!
                                       Dataset.metaClass.invokeStaticMethod(Dataset, finderName, args) )
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
        log.debug "Dataport is a local datasource: ${dataport.isLocalDatasource()}"
        
        // Look for fields - expecting format:
        //   fieldName=value|RANDTEXTmaxNN|RANDNUMmaxNN|RANDDATE|[val1|val2..|valn]
        // where [val1|val2..|valn] are a list to choose randomly from
        //  and maxNN in RANDTEXT and RANDNUM tokens means restrict number of output chars to the given length
            
        def fields=[:], gen    = new RandomGenerator()
        def fieldNames = RequestUtils.extractSearchKeys(params, CONTROL_FLAGS)
		
		
        fieldNames.each { field ->
            def type = params."$field"
			log.debug "Registering field [$field] as type '$type'"
			fields[field] = gen.getGenerator(type)
        }
        
        // And ensure the standards are all represented
        log.debug "Checking standard fields"
        for (j in 0..Dataport.STD_FIELDS.size()-1) {
            if (!fields[Dataport.STD_FIELDS[j]] && Dataport.STD_FIELDS[j] != Dataport.STD_UUID) {
                // Field not set - default, except for UUID which will get generated
                log.debug "Adding generator for std field [${Dataport.STD_FIELDS[j]}]"
                fields [Dataport.STD_FIELDS[j]] =
                     Dataport.STD_FIELDS[j].endsWith("Date") ?
                        gen.getGenerator(RandomGenerator.TYPE_DATE) :  // the only non-textfields among the standards
                        gen.getGenerator()    // everything else is ok using a text generator
            }
        }

        // Now simply loop through number of fields....
        log.debug "Creating output writer"
        def dataset, writer = outFile.newWriter()
        
        def numToGen
        try {
           numToGen = params[GEN_SIZE_TOKEN] ? Integer.parseInt(params[GEN_SIZE_TOKEN])    : MAX_GENERATED
        } catch (Exception e) {
            log.error "Generate request asked for an unparsable [${params[GEN_SIZE_TOKEN]}] number of records]"
            numToGen = MAX_GENERATED
        }
        log.debug "Generating [$numToGen] data records"
        for (i in 1..numToGen) {
            
            // CREATE DATASET
            def fname, newval
            Map<String,Object> props = new HashMap<String,Map>()
            props['fields'] = new HashMap<String,Map>()
            
            dataset = new Dataset()
            log.trace "----- Creating new Dataset -----"
            
            // Populate standard fields...
            fields.each { key, fieldGen ->
                fname = key.toString()
                if (Dataport.STD_FIELDS.contains(fname)) {
                    log.trace "  [$fname]: $newval"
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
                if (!Dataport.STD_FIELDS.contains(fname) && fname != GEN_SIZE_TOKEN && fname != FORCE_FLAG) {
                    // generate value
                    dataset[fname] = fields[fname].getVal()
                    // record name of extra field so we can retrieve it later from mongostore
                    dataset.fieldNames << fname
                }
            }
            log.trace "Rendering as JSON: $dataset"
            def jsonStr = dataset.toJson(dataport).toString()
            log.debug "------------- Writing JSON: -----------------\n $jsonStr"
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


