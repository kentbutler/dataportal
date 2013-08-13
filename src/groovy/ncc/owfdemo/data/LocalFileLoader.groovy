package ncc.owfdemo.data

import ncc.owfdemo.Dataport;

class LocalFileLoader {
	def log
	
	LocalFileLoader(def logger) {
		log = logger
	}
	
	def load (Dataport data) {
		log?.debug "------ loading local data from: $data.endpoint ------"
		
		File infile
		
		// Read file
		try {
			infile = new File(data.endpoint)
			
		} catch (Exception e) {
			e.printStackTrace()
			log?.error "Error reading file [${data.endpoint}]: $e"
		}
		
		log?.debug "------checking content ------"
		if (!(infile?.exists())) {
			// Try reading as a URL
			def urlObj = new URL(data.endpoint)
			if (urlObj?.toURI()) {
				infile = new File(urlObj.toURI())
			}
			
			if (!(infile?.exists())) {
				log?.debug "------file does not exist ------"
				
				log?.error "#### local endpoint invalid: file ${data.endpoint} does not exist"
				return
			}
		}
		
		log?.debug "------reading content ------"
		def txt = new ArrayList<String>(2048)
		infile.eachLine {
			txt << it
			log?.trace it
		}
		log?.debug "------returning content with size [${txt?.size()}] ------"
		
		return txt
	}
	

}
