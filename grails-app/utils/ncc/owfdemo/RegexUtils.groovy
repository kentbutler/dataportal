package ncc.owfdemo

class RegexUtils {
	static def URL_PATTERN = ~/^(([^#:\/?]+):)?(\/\/([^#\/?]*))?([^#?]*)(\\?([^#]*))?(#(.*))?/
	
	static def dumpMatcher = {m ->
		println "Matcher: $m"
		println "Num matches: ${m.size()}"
		println "Num groups: ${m.groupCount()}"
		println "Direct access to first match: ${m ? m[0][0] : 'empty'}"
		println "Iterated matches:: -- START --"
		m.each { println it[1] }
		println "Iterated matches:: -- END --"
		println "String matches? ${m.matches()}"
		// Iterate matched groups
		m.eachWithIndex { it, i ->
		   println "[Group $i]"
		   it.each { p ->
			   println "\t$p"
		   }
		}
	}
	
	static def parseUrl (def url) {
	   def retVal = [:]
	   def URL_PATTERN = ~/^(([^#:\/?]+):)?(\/\/([^#\/?]*))?([^#?]*)(\\?([^#]*))?(#(.*))?/
	   if (url && url.size() > 0) {
			  def m =(url =~ URL_PATTERN)
			  if (m?.groupCount() > 0) {
					if (m[0][2]) retVal['scheme'] = m[0][2]
					if (m[0][5]) retVal['path'] = m[0][5]
					if (m[0][6]) retVal['args'] = m[0][6]
					if (m[0][9]) retVal['related'] = m[0][9]
			  }
					if (m[0][4] != null && m[0][4].size() > 0) {
						def l = m[0][4].split(':')
						if (l?.size() > 0) {
							retVal['host'] = l[0]
						}
						if (l?.size() > 1) {
							retVal['port'] = l[1]
						}
					}
			  
		 }
		 return retVal
	}
}
