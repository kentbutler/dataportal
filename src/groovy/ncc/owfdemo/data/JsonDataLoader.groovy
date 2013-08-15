package ncc.owfdemo.data

import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;

import com.sun.org.apache.xalan.internal.xsltc.compiler.ForEach;

import ncc.owfdemo.Dataset
import ncc.owfdemo.JsonUtils
import net.sf.json.JSONObject

class JsonDataLoader {
    def log
    
    JsonDataLoader(def logger) {
        log = logger
    }
    
    void parseAndStore (def dataport, def data) {
        def json
		log.trace "----------------------- loading JSON from file ----------------"
        if (data instanceof java.util.Collection) {
            def errCount = 0
			
			for (str in data) {
				log.trace "----- JSON:: $str"
				// GUARD
				if (!str || str.size() <= 0) {
					log.trace "...skipping..."
					continue
				}
				
				// Remove some potential JSON format tokens
				if (str[0] == '[') {
					str = str[1..-1]
				}
				if (str[-1] == ',') {
					str = str[0..-2]
				}
                try {
                    json = JSONObject.fromObject(str)
                    
                } catch (Exception e) {
                    // try to fix common errors
                    try {
                        json = JSONObject.fromObject(JsonUtils.fixSimpleJsonErrors(str))
                        
                    } catch (e2) {
                        log.error "### Unable to Parse JSON:\n ERROR: $e2 \nJSON: $str"
                        ++errCount
						json = null
                    }
                }
                // Put JSON record into Mongo intermediate storage
                if (json) {
	                def dataset
	                try {
	                    dataset = Dataset.fromJson(dataport, json)
	                }
	                catch (Exception e) {
						//if (e instanceof RuntimeException) {
							e.printStackTrace()
						//}
	                    log.error "### Error parsing JSON: $e"
	                    log.error "### JSON:\n${json?.toString()}"
						dataset = null
	                }
	
	                if (dataset) {
	                    //TODO Save this in a data collection named for our dataport
	                    //   following did not work
	                    //dataset.mapping['collection'] = dataport.contextName
	                    //dataset.dataportName = dataport.contextName
	                    
	                    if (!dataset.uuid) {
	                        // Generate some identifier
	                        dataset.uuid = UUID.randomUUID().toString()
	                    }
	                    try {
	                        log.trace "================== storing data into Mongo ========================\n$dataset"
							Dataset.useCollection(dataport.contextName) 
							dataset.save()
	                        
	                    } catch (Exception e) {
	                        e.printStackTrace()
	                        log.error "### Error putting record to Mongo:\n$dataset\n$e"
	                    }
	                }
	                else {
	                    log.error "### Some error creating a dataset from Dataport from JSON"
	                    log.error "${json?.toString()}"
	                }
                }
            }
            
            // Flush additions
            Dataset.withSession { session ->
                session.flush()
            }
            log.debug "After JSON processing:: ERROR COUNT: $errCount\n"
        }
        else {
			log.warn "JSON is not detected as being a collection; check the raw data format for issues (jsonlint.com)"
            json = JSONObject.fromObject(data)
            // Put JSON record into Mongo intermediate storage
            try {
                def dataset = Dataset.fromJson(dataport, json)
                Dataset.useCollection(dataport.contextName) 
                dataset?.save(flush:true)
                
            } catch (Exception e) {
                e.printStackTrace()
                log.error "Error putting record to Mongo:\n$dataset\n$e"
            }
        }
    }
}
