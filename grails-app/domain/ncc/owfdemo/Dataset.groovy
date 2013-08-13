package ncc.owfdemo

import java.text.SimpleDateFormat

import net.sf.json.JSONObject


/**
 * Represents the set of data exposed by the dataportal.  Maps fields and renders JSON.
 * Backed by MongoDB.
 *
 * @author kent
 */
class Dataset {
	
	String id
	String uuid
	String name
	String description
	String location
	String createDate
	String eventDate
	def fieldNames = []
	
	static mapWith = 'mongo'
	
	static mapping = {
		uuid index:true, indexAttributes: [unique:true, dropDups:true]
		name index:true, indexAttributes: [unique:false, dropDups:false]
		description index:true, indexAttributes: [unique:false, dropDups:false]
		location index:true, indexAttributes: [unique:false]
	}
	
	String toString() {
		def sb = new StringBuilder(64)
		sb.append "\n---- Dataset ----"
		sb.append "\n   id: $id"
		sb.append "\n   uuid: $uuid"
		sb.append "\n   Name: $name"
		sb.append "\n   Descr: $description"
		sb.append "\n   Location: $location"
		sb.append "\n   CreateDate: $createDate"
		sb.append "\n   EventDate: $eventDate"
		sb.append "\n   Field-names: $fieldNames"
		return sb
	}
	
	JSONObject toJson(def dataport) {
		JSONObject json = new JSONObject()
		log.trace "#### Rendering dataset as JSON"
		
		json.put(Dataport.STD_UUID, uuid)
		json.put(Dataport.STD_NAME, name)
		json.put(Dataport.STD_DESCRIPTION, description)
		json.put(Dataport.STD_LOCATION, location)
		
		// Perhaps we should do this on ingest, but then that would only be for local datasources
		def dateVal
		if (createDate && dataport.getCreateDateFormatter()) {
			//log.trace "  Formatting createDate [$createDate] for output"
			try {
				dateVal = dataport.getCreateDateFormatter().parse(createDate)
				//log.trace "  createdate parsed as [$dateVal]"
				//log.trace "  createdate formatted as [${dataport.getOutputDateFormatter().format(dateVal)}]"
				json.put(Dataport.STD_CREATE_DATE, dataport.getOutputDateFormatter().format(dateVal))
			}
			catch (Exception e) {
				log.warn "unable to parse createDate [$createDate] using format [${dataport.getCreateDateFormatter()}]" 
			}
		}
		else {
			json.put(Dataport.STD_CREATE_DATE, createDate)
		}
		
		if (eventDate && dataport.getEventDateFormatter()) {
			try {
				dateVal = dataport.getEventDateFormatter().parse(eventDate)
				json.put(Dataport.STD_EVENT_DATE, dataport.getOutputDateFormatter().format(dateVal))
			}
			catch (Exception e) {
				log.warn "unable to parse eventDate [$eventDate] using format [${dataport.getEventDateFormatter()}]" 
			}
		}
		else {
			json.put(Dataport.STD_EVENT_DATE, eventDate)
		}

		fieldNames.each {
			log.trace "toJSON ==> adding expando: [$it] val this[it]"
			json.put(it, this[it])
		}
		
		return json
	}
	
	/**
	 * For initial data discovery - creates a persistable Dataset from an input JSONObject,
	 * to be filed under the given Dataport.
	 * @param dataport
	 * @param json
	 * @return
	 */
	static Dataset fromJson (Dataport dataport, JSONObject json) {
		if (!dataport || !json) return
		
		def dataset = new Dataset()
		//def fieldJson
		
		// Access the standard fields using mappings defined in Dataport
		// Here we translate and store with standard naming
		dataset.uuid = json.optString(dataport.mapUuid, null) 
		dataset.name = json.optString(dataport.mapName, "nameUnknown")
		dataset.description = json.optString(dataport.mapDescr, "description")
		dataset.location = json.optString(dataport.mapLocation, "location")
		dataset.createDate = json.optString(dataport.mapCreateDate, "createDate")
		dataset.eventDate = json.optString(dataport.mapEventDate, "eventDate")
		
		// Add additional unmapped fields
		def keys = []
		json.keySet().each { keys << it.toString() }
		
		keys.remove(dataport.mapUuid)
		keys.remove(dataport.mapName)
		keys.remove(dataport.mapDescr)
		keys.remove(dataport.mapLocation)
		keys.remove(dataport.mapCreateDate)
		keys.remove(dataport.mapEventDate)
		
		dataport.log.trace "Dataset.fromJson() extra fields to be persisted:: $keys"
		
		def fname
		keys.each { key ->
			// Need to track field names so we can retrieve them later
			dataset.fieldNames << key
			dataset[key] = json.optString(key)
		}
		
		return dataset
	}
}
