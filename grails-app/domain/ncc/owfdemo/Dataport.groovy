package ncc.owfdemo

import java.text.DateFormat
import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Describes a source of data - what to call it (contextName, description), how to contact it (endpoint), 
 * and how to translate some of its fields (type and map* fields).
 *  
 * @author kent
 */
class Dataport {

	String contextName
	String endpoint
	String type
	String description
	String outputDateFormat = DEFAULT_OUTPUT_DATE_FORMAT
	Boolean loaded = Boolean.FALSE
	
	// How Dataportal regards the fields
	static final String STD_UUID = "uuid"
	static final String STD_NAME = "name"
	static final String STD_DESCRIPTION = "description"
	static final String STD_LOCATION = "location"
	static final String STD_CREATE_DATE = "createDate"
	static final String STD_EVENT_DATE = "eventDate"
	static final STD_FIELDS = [STD_UUID, STD_NAME, STD_DESCRIPTION, STD_LOCATION, STD_CREATE_DATE, STD_EVENT_DATE]
	
	static final String DEFAULT_OUTPUT_DATE_FORMAT = "MM/dd/yyyy"
	
	// How the incoming Dataset regards them - defaults as normalized already,
	//    user should override these as necessary
	String mapUuid = STD_UUID
	String mapName = STD_NAME
	String mapDescr = STD_DESCRIPTION 
	String mapLocation = STD_LOCATION
	String mapCreateDate = STD_CREATE_DATE
	String mapEventDate = STD_EVENT_DATE
	String mapCreateDateFormat = "yyyyMMdd"
	String mapEventDateFormat = "yyyyMMdd"
	
	// Grails2 autoTimestamp feature
	Date dateCreated
	Date lastUpdated
	

	transient def createDateFormatter
	transient def eventDateFormatter
	transient def outputDateFormatter
	
	static transients = ['createDateFormatter','eventDateFormatter','outputDateFormatter']
	
	static constraints = {
		contextName(blank:false,nullable:false,maxSize:32)
		endpoint(blank:false,nullable:false,maxSize:1024)
		description(blank:false,nullable:false,maxSize:2048)
	}
	
	String toString() {
		StringBuilder sb = new StringBuilder()
		sb.append("\n---- Dataport ----")
		sb.append("\n    id:    ").append(id)
		sb.append("\n    Context:    ").append(contextName)
		sb.append("\n    Endpoint:    ").append(endpoint)
		sb.append("\n    IsLocal?:    ").append(isLocalDatasource())
		sb.append("\n    Type:    ").append(type)
		sb.append("\n    Description:    ").append(description)
		sb.append("\n    Loaded:    ").append(loaded)
		sb.append("\n    DateCreated: ").append(dateCreated)
		sb.append("\n    LastUpdated: ").append(lastUpdated)
		sb.toString()
	}
	
	// -- Local logic
	
	boolean isLocalDatasource() {
		def url = RegexUtils.parseUrl(endpoint)
		return url?.scheme == 'file'
	}
	
	/**
	 * Provided as an optimization - note not thread safe!
	 * @return
	 */
	DateFormat getCreateDateFormatter() {
		getDateFormatter (createDateFormatter, mapCreateDateFormat) 
	}
	
	/**
	 * Provided as an optimization - note not thread safe!
	 * @return
	 */
	DateFormat getEventDateFormatter() {
		getDateFormatter (eventDateFormatter, mapEventDateFormat) 
	}

	/**
	 * Provided as an optimization - note not thread safe!
	 * @return
	 */
	DateFormat getOutputDateFormatter() {
		getDateFormatter (outputDateFormatter, outputDateFormat) 
	}

	DateFormat getDateFormatter(def formatter, def format) {
		if (!formatter && format?.size() > 0) {
			formatter = new SimpleDateFormat(format)
		}
		return formatter
	}

    // --json-rest-api fields
    static expose = 'dataport'

    static api = [
        excludedFields:[],
        list : { params -> 
			//TODO Use Dataset to retrieve items matching query
			if (params.items) {
				getService()?.retrieveItems(this, params)
			}
			else {
				Dataport.list(params)
			} 
		}
    ]

	/**
	 * Rending this object into a JSONObject; allows more flexibility and efficiency in how
	 * the object is eventually included in larger JSON structures before ultimate rendering;
	 * MessageSource offered for i18n conversion before exporting for user audience.
	 * @param messageSource
	 * @return
	 */
	JSONObject toJSON(def messageSource) {
		JSONObject json = new JSONObject()
		json.put('id', id)
		json.put('contextName', contextName)
		json.put('type', type)
		json.put('endpoint', endpoint)
		json.put('description', description)
		return json
	}
	
	def getService() {
		return domainClass?.grailsApplication?.mainContext?.dataportService
	}


}
