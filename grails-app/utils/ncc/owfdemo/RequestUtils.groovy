package ncc.owfdemo

import groovy.util.logging.Log4j

import javax.servlet.ServletContext

@Log4j
class RequestUtils {
	
	/**
	 * Given a list of tokens, construct a GORM finder method name. Uses 'And'
	 * in the event of multiple params by default
	 * @param params List or Map
	 * @return
	 */
	static String createFinderName (def m, def logic = 'And') {
		def out = new StringBuilder(256), cnt = 0
		
		def p = RequestUtils.extractSearchKeys(m)
		
		p.each { val -> 
			if (cnt == 0) out << "findAllBy"
			if (cnt > 0) out << logic
			out << val.capitalize()
			++cnt
		}
		if (out.size() == 0) {
			out << "list"
		}
		return out.toString()
	}
	
	/**
	 * Remove contents from the given map which are used for controller meta information,
	 * such as 'action', 'controller', 'max', 'sort'.
	 * 
	 * Supports 'like' searches by returning parameter Map including with %values%  
 	 * Do this for directing GORM how to query - i.e. in constructing findBy statements.
 	 * 
	 * @param params List or Map
	 * @return list of keys
	 */
	static def extractSearchKeys(def m, def exclList = []) {
		def out = [], keyval, iter = m, isMap = false
		
		if (m instanceof Map) {
			iter = m.keySet()
			isMap = true
		 }
		
		 iter.each { key ->
			keyval = isMap ? key.toString() : key
			
			if (!RequestUtils.STD_CONTROLLER_META_TAGS.contains(keyval) && !exclList.contains(keyval)) {
				out << keyval
			}
		 }
		return out
	}
	
	/**
	 * Remove keywords appended to search tokens like 'Like' and modifies search value
	 * with respect to the token, e.g. nameLike=jon becomes name=%jon%.
	 * Use this for query input parameter sets.
	 * @param params Map
	 * @param optional db style of wildcarding to use - 'sql' or 'mongo' -- NOTE the Grails plugin will translate SQL style for you
	 * @return list of params
	 */
	static def extractSearchValues(Map params, def style=SQL) {
		def out = [:], isLikeModifier = false
		def tok = (style==SQL ? "%" : (style==MONGO ? "/":""))
		def keyval
		
		params.keySet().each { key ->
			keyval = key.toString()
			isLikeModifier = keyval.endsWith("Like") || keyval.endsWith("Ilike")
			
			if (!RequestUtils.STD_CONTROLLER_META_TAGS.contains(keyval)) {
				out[keyval] = (isLikeModifier ? "$tok${params[keyval]}$tok" : params[keyval])
			}
		}
		return out
	}
	
	/**
	 * Given a web ServletContext calculate the location of external files produced by the app.
	 * @param ctx
	 * @return
	 */
	static String getFileOutputLocation(ServletContext ctx) {
        def base = ctx.getRealPath("/")
        def dirName = "${base}${File.separator}tmp"
        def backup = "/var/data"
        
        def dir = new File(dirName)
        
        if (!dir.exists()) {
            log.error "Unable to access file $dirName - using backup dir $backup"
            dir = new File(backup)
        }
        else {
            def f = new File("${dir.absolutePath}${File.separator}test.txt")
            if (!f.exists()) {
                log.error "Unable to access file ${f.absolutePath} - using backup dir $backup"
                dir = new File(backup)
            }
        }
        return dir.absolutePath
    }      
	
	
	// Standard controller meta tags which are often included in the params map
	// Included here is 'contextName' which is given by UrlMapping but should not affect app business logic
	final static def STD_CONTROLLER_META_TAGS = ['action', 'controller', 'max', 'sort','contextName', 'dojo','dojo.preventCache'] 
	static final String SQL = 'sql'
	static final String MONGO = 'mongo'

}
