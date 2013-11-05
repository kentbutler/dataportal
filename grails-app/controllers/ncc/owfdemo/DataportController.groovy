package ncc.owfdemo

import javax.servlet.ServletOutputStream

import net.sf.json.JSONArray
import net.sf.json.JSONObject

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.sitemesh.GrailsPageFilter

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
            render(text: "{\"message\":\"Error retrieving dataport [${params.contextName}]: ${e?.toString()?.encodeAsHTML()} \"}", contentType: "application/json", encoding: "UTF-8", status: 500)
            return
        }
        
        if (!dataport) {
            log.error "Dataport [${params.contextName}] not found"
            render(text: "{\"message\":\"Could not locate dataport [${params.contextName}]\"}", contentType: "application/json", encoding: "UTF-8", status: 404)
            return
        }

        log.debug "Request method: ${request.method}"
                
        // Check for Cross-domain OPTIONS check (often used by Chrome)
        if (request.method == "OPTIONS") {
            response.addHeader('Access-Control-Allow-Origin', '*')
            render(text: '', contentType: "text/plain", encoding: "UTF-8", status: 200)
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
            render(text: "{\"message\":\"Error loading dataport [${params.contextName}]\"}", contentType: "application/json", encoding: "UTF-8", status: 500)
            return
        }
        
        def results
        try {
            results = dataportService.retrieveItems(dataport, params, request)
            
            // Trying to snuff this so proxied redirects do not affect our result to the client 
            request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
            log.debug "Redirect header set: ${request.getAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)}"
            
        } catch (Exception e) {
            log.error "Error retrieving data from warehouse [${dataport.contextName}]: $e"
            //if (e instanceof RuntimeException) {
                e.printStackTrace()
            //}
            render(text: "{\"message\":\"Could not load dataset [${params.contextName}]\"}", contentType: "application/json", encoding: "UTF-8", status: 500)
            return
        }
        
        log.trace "================== rendering results [${results}] ========================"

        def jsonOut
        
        if (dataport.isLocalDatasource()) {
            // Assemble results in to a JSON list
            JSONArray alist = new JSONArray()
            results.each {
                log.trace "[${it.class.name}] ==> $it"
                alist.add it.toJson(dataport)
            }
            def JSONObject json = new JSONObject()
            json.put("items", alist)
            json.put("numItems", alist.size())
            
            jsonOut = json.toString()
            render(text: jsonOut, contentType: "application/json", encoding: "UTF-8", status: 200)
        }
        else {
            //jsonOut = results?.data ?: ""
            
            //def curDomain = "${request.scheme}://${request.serverName}:${request.serverPort}"
            //log.debug "Cur domain:: $curDomain"
            
            // Instead of doing this I installed the CORS plugin which enhances all response objects
            //    this only would do the GET request; OPTIONS was still getting rejected in Ffox
            //response.setHeader('Access-Control-Allow-Origin', '*')
            
            if (results instanceof wslite.http.HTTPResponse) {
                log.debug "Response is a ${results.class.name}"
                log.debug "Sitemesh is active: ${response.pageResponseWrapper.isSitemeshActive()}"
                log.debug "Sitemesh page: ${request.getAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE)}"
                log.debug "Sitemesh is used: ${request.getAttribute(GrailsPageFilter.GSP_SITEMESH_PAGE)?.isUsed()}"
                
                /*
                log.debug "Status: ${results.statusCode}"
                response.status = results.statusCode
                
                log.debug "Content type: ${results.contentType}"
                //response.contentType = ''
                //response.contentType = results.contentType
                
                */
                results.getHeaders().each { key, val ->
                    log.debug "Header: $key: $val"
                    
                    // Including this header breaks the response!! Content will be squelched,
                    //   i.e. this creates empty responses.
                    // Removing still allows the CORS plugin to do its job.
                    if (key != 'Access-Control-Allow-Origin') {
                        response.addHeader(key, val)
                    }
                }
                
                log.debug "Char enc: ${results.charset}"
                response.setCharacterEncoding(results.charset)
                
                if (results.contentType?.contains("json") || results.contentType?.contains("text")) {
                    jsonOut = new String(results.data)
                    log.debug "JSON content to return:: $jsonOut"
                    
                    render (text: jsonOut, contentType: results.contentType, encoding: results.charset, status: results.statusCode)
                }
                else {
                    try {
                        response.outputStream << new ByteArrayInputStream(results.data)
                        response.outputStream.flush()
                        
                        response.contentType = results.contentType
                        
                        log.debug "Preparing response:: $response"
                        log.debug "..with content:: ${response.content}"
                        
                    } catch (Exception e) {
                        log.error "Error writing response: $e"
                        e.printStackTrace()
                    }
                    
                    return null
                }
            }
            else {
                render(text: "{\"message\":\"Unexpected return type [${results?.class?.name}]\"}", contentType: "application/json", encoding: "UTF-8", status: 500)
            }
        }
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
            render(text: "{\"message\":\"Error retrieving dataport [${params.contextName}]: ${e?.toString()?.encodeAsHTML()} \"}", contentType: "application/json", encoding: "UTF-8", status: 500)
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
				render(text: "{\"message\":\"$msg\"}", contentType: "application/json", encoding: "UTF-8", status: 500)
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
			render(text: "{\"message\":\"$msg\"}", contentType: "application/json", encoding: "UTF-8", status: 500)
			return
		}

		// RETURN SUCCESS
        render(text: "{\"message\":\"successfully generated endpoint ${dataport.contextName}\"}", contentType: "application/json", encoding: "UTF-8", status: 200)
    }  
	
}
