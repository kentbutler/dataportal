package ncc.owfdemo

import net.sf.json.JSONArray
import net.sf.json.JSONObject

class DataportController {
    
    // Dependencies
    def dataportService
    
    
    // Methods
    
    def index() { }
    
    /**
     * Query an existing Dataport. Will load the data if necessary. 
     * @param params
     * @return
     */
    def proxy(def params) {
        log.debug "================== proxy() ========================"
        log.debug "Processing request for data context [${params.contextName}]"
        
        def dataport
        try {
            dataport = Dataport.findByContextName params.contextName
        }
        catch (Exception e) {
            log.error "Error retrieving dataportal for context [${params.contextName}]: $e"
            if (e instanceof RuntimeException) {
                e.printStackTrace()
            }
            render(text: "{\"message\":\"Error retrieving dataport [${params.contextName}]: ${e?.toString()?.encodeAsHTML()} \"}", contentType: "text/json", encoding: "UTF-8", status: 500)
            return
        }
        
        if (!dataport) {
            log.error "Dataport [${params.contextName}] not found"
            render(text: "{\"message\":\"Could not locate dataport [${params.contextName}]\"}", contentType: "text/json", encoding: "UTF-8", status: 404)
            return
        }
        
        try {
            log.trace "================== checking data ========================"
            dataportService.load dataport, params
        } 
        catch (Exception e) {
            log.error "Error loading data from ${dataport.endpoint}: $e"
            //if (e instanceof RuntimeException) {
                e.printStackTrace()
            //}
            render(text: "{\"message\":\"Error loading dataport [${params.contextName}]\"}", contentType: "text/json", encoding: "UTF-8", status: 500)
            return
        }
        
        def results
        try {
            results = dataportService.retrieveItems(dataport, params)
            
        } catch (Exception e) {
            log.error "Error retrieving data from warehouse [${dataport.contextName}]: $e"
            if (e instanceof RuntimeException) {
                e.printStackTrace()
            }
            render(text: "{\"message\":\"Could not load dataset [${params.contextName}]\"}", contentType: "text/json", encoding: "UTF-8", status: 500)
            return
        }
        
        log.trace "================== rendering results [${results?.size()}] ========================"

        // Assemble results in to a JSON list
        JSONArray alist = new JSONArray()
        results.each {
            log.trace "[${it.class.name}] ==> $it"
            alist.add it.toJson(dataport)
        }
        def JSONObject json = new JSONObject()
        json.put("items", alist)
        json.put("numItems", alist.size())
        
        render(text: json.toString(), contentType: "text/json", encoding: "UTF-8", status: 200)
    }
    
    /**
     * Generate data given: contextName from URL, and fields described as params. 
     * @param params
     * @return
     */
    def generate(def params) {
        log.debug "================== generate() ========================"
        log.debug "Processing request for data generation of context [${params.contextName}]"
        
        def dataport
        try {
            dataport = Dataport.findByContextName params.contextName
        }
        catch (Exception e) {
            log.error "Error retrieving dataportal for context [${params.contextName}]: $e"
            if (e instanceof RuntimeException) {
                e.printStackTrace()
            }
            render(text: "{\"message\":\"Error retrieving dataport [${params.contextName}]: ${e?.toString()?.encodeAsHTML()} \"}", contentType: "text/json", encoding: "UTF-8", status: 500)
            return
        }
        
        if (dataport) {
			if (params.force) {
				log.debug "Removing existing dataportal and re-creating"
				dataport.delete(flush:true)
			}
			else {
				def msg = "Dataport [${params.contextName}] already exists - please try another"
				log.debug msg
				render(text: "{\"message\":\"$msg\"}", contentType: "text/json", encoding: "UTF-8", status: 500)
				return
			}
        }
        

		// Output filename, should be 
		//       <webapp deploy location>/<context>.json
        def outFileName = "${RequestUtils.getFileOutputLocation(servletContext)}${File.separator}${params.contextName}.json"
        
        log.debug "Generating dataset [${params.contextName}] into file $outFileName"
		
		File outFile = new File(outFileName)
		if (outFile.exists()) {
			log.warn "Deleting existing outfile: $outFileName"
			outFile.delete()
		}

		try {
			dataport = dataportService.generate(params, outFile)
		}
		catch (Exception e) {
			def msg = "Error generating - see logs for details"
			if (e instanceof RuntimeException) {
				e.printStackTrace()
			}
			render(text: "{\"message\":\"$msg\"}", contentType: "text/json", encoding: "UTF-8", status: 500)
			return
		}

		// RETURN SUCCESS
        render(text: "{\"message\":\"successfully generated endpoint ${dataport.contextName}\"}", contentType: "text/json", encoding: "UTF-8", status: 200)
    }  
	
}
