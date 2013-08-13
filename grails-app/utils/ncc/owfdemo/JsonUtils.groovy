package ncc.owfdemo

/**
 * @author kent
 *
 */
class JsonUtils {

	static def fixSimpleJsonErrors (sin) {
		if (!sin) return
		
		// remove double quotes
		sin = sin.replaceAll("\"\"","\"")
		
		// remove newlines
		sin = sin.replaceAll("\n"," ")
	}
	
	
	/**
	 * TBD
	 * @param sin
	 * @return
	static def fixJsonErrors (sin) {
		if (!sin) return
		
		StringBuilder out = new StringBuilder(sin.size())
		
		def prevChar, prevQuotePos = -1, inQuotes = false, i = 0 
		
	}
	 */
}
