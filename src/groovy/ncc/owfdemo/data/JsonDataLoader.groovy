package ncc.owfdemo.data

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
        if (data instanceof java.util.Collection) {
            def errCount = 0, str
            data.each {
				// Remove some potential JSON format tokens
				str = it
				if (str && str.size() > 0 && str[0] == '[') {
					str = str[1..-1]
				}
				if (str && str.size() > 0 && str[-1] == ',') {
					str = str[0..-2]
				}
				log.trace "----------------------- loading JSON from file ----------------\n $str"
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
						if (e instanceof RuntimeException) {
							e.printStackTrace()
						}
	                    log.error "### Error parsing JSON:\n${json?.toString()}\n$e"
						dataset = null
	                }
	
	                if (dataset) {
						log.trace "================== created dataset ========================\n$dataset"
	                
	                    //TODO Save this in a data collection named for our dataport
	                    //   following did not work
	                    //dataset.mapping['collection'] = dataport.contextName
	                    //dataset.dataportName = dataport.contextName
	                    
	                    if (!dataset.uuid) {
	                        // Generate some identifier
	                        dataset.uuid = UUID.randomUUID().toString()
	                    }
	                    try {
	                        log.trace "================== storing data into Mongo ========================"
	                         dataset.save()
	                        
	                    } catch (Exception e) {
	                        e.printStackTrace()
	                        log.error "### Error putting record to Mongo:\n$dataset\n$e"
	                    }
	                }
	                else {
	                    log.error "### Some error creating a dataset from Dataport\n$dataport \n ---- and JSON ---\n [$json]"
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
            def dataset = Dataset.fromJson(dataport, json)
            try {
                dataset.save(flush:true)
                
            } catch (Exception e) {
                e.printStackTrace()
                log.error "Error putting record to Mongo:\n$dataset\n$e"
            }
        }
    }
}
