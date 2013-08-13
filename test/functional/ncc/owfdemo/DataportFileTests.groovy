package ncc.owfdemo

import ncc.owfdemo.data.RandomGenerator
import net.sf.json.JSONArray
import net.sf.json.JSONObject

import org.apache.commons.logging.LogFactory
import org.grails.plugins.test.GenericRestFunctionalTests

import com.grailsrocks.functionaltest.BrowserTestCase

@Mixin(GenericRestFunctionalTests)
class DataportFileTests extends BrowserTestCase {

	def log = LogFactory.getLog(getClass())
	def messageSource
	def dataport
	def dataportService
	
	//TODO Figure out how to detect which OS we are running on to modify path
	
	void setUp() {
		super.setUp()
		setupData()
	}
	
	void setupData() {
		log.debug "================================ setupData() ========================================="
		dataport = new Dataport(contextName:'ufo-mini',endpoint:'file:///opt/projects/dataportal/data/ufo-mini.json',type:'json',description:'Test description')
		dataport.mapUuid = 'uuid'
		dataport.mapName = 'location'
		dataport.mapDescr = 'description'
		dataport.mapLocation = 'location'
		dataport.mapCreateDate = 'reported_at'
		dataport.mapEventDate = 'sighted_at'
		dataport.mapCreateDateFormat = 'yyyyMMdd'
		dataport.mapEventDateFormat = 'yyyyMMdd'
	}
	
	void tearDown() {
		super.tearDown()
	}

	
	// ----------- Test the /api/dataport REST interface ------------------
	
	void testList() {
		assertNotNull "Test dataport is null", dataport	
		assertNull "Test dataport id is not null", dataport.id
		genericTestList(dataport)
	}
	void testCreate() {
		assertNotNull "Test dataport is null", dataport
		genericTestCreate(dataport)
	}
	
	void testShow() {
		assertNotNull "Test dataport is null", dataport
		genericTestShow(dataport)
	}
	
	void testUpdate() {
		assertNotNull "Test dataport is null", dataport
		genericTestUpdate(dataport, [contextName:"ctx-two"])
	}
	
	void testDelete() {
		assertNotNull "Test dataport is null", dataport
		genericTestDelete(dataport)
	}

	// ----------- Test the /data/<contextName> REST interface ------------------
	
	void testLoadData() {
		def data = Dataport.findByContextName('ufo-mini')
		if (!data) {
			data = dataport.save(flush:true)
		}
		dataport = data
		assertNotNull "Test dataport is null", dataport
		
		// list all records of dataset
		get("/data/ufo-mini")

		// *** Examine Result ***
		def model = response.contentAsString
		log.debug "Response: $model"
		
		assertStatus 200
		JSONObject json = JSONObject.fromObject(model)
		def numItems = json.optInt("numItems", 0)
		assertTrue "0 item count in results", numItems > 0
		
		JSONArray jlist = json.getJSONArray("items")
		assertNotNull jlist
		assertTrue "list is empty", jlist.size() > 0
		assertTrue "list length is not same as count", jlist.size() == numItems
	}
	
	void testLoadDataLike() {
		def data = Dataport.findByContextName('ufo-mini')
		if (!data) {
			data = dataport.save(flush:true)
		}
		dataport = data
		assertNotNull "Test dataport is null", dataport
		
		// *** Search for just a few records
		get("/data/ufo-mini?descriptionLike=human")
		
		def model = response.contentAsString
		log.debug "Response: $model"
		
		assertStatus 200
		def json = JSONObject.fromObject(model)
		def numItems = json.optInt("numItems", 0)
		assertTrue "0 item count in results", numItems > 0
		
		def jlist = json.getJSONArray("items")
		assertNotNull jlist
		assertTrue "list is empty", jlist.size() > 0
		assertTrue "list length is not correct for human search", jlist.size() == 1

	}

	void testSearchUnknownDataset() {
		get("/data/unknown")
		def model = response.contentAsString
		log.debug "Response: $model"
		
		assertStatus 404
		def json = JSONObject.fromObject(model)
		def msg = json.optString("message", "")
		assertTrue "Message is incorrect [$msg]", msg.startsWith("Could not locate")
	}		

	void testLoadExtraFields() {
		def data = Dataport.findByContextName('ufo-mini')
		if (!data) {
			data = dataport.save(flush:true)
		}
		dataport = data
		assertNotNull "Test dataport is null", dataport
		
		get("/data/ufo-mini")
		
		def model = response.contentAsString
		log.debug "Response: $model"
		
		assertStatus 200
		def json = JSONObject.fromObject(model)
		def numItems = json.optInt("numItems", 0)
		assertTrue "0 item count in results", numItems > 0
		
		def jlist = json.getJSONArray("items")
		assertNotNull jlist
		assertTrue "list is empty", jlist.size() > 0
		
		def jsonRec = jlist.get(0)
		assertNotNull "Item at pos 0 is null", jsonRec
		// Expecting some extra fields here
		assertNotNull "Extra field 'shape' is null", jsonRec.optString("shape")
		assertNotNull "Extra field 'duration' is null", jsonRec.optString("duration")
		
	}

	void testFieldMappingAndDataFormat() {
		def data = Dataport.findByContextName('ufo-mini')
		if (!data) {
			data = dataport.save(flush:true)
		}
		dataport = data
		assertNotNull "Test dataport is null", dataport
		
		get("/data/ufo-mini")
		
		def model = response.contentAsString
		log.debug "Response: $model"
		
		assertStatus 200
		def json = JSONObject.fromObject(model)
		def numItems = json.optInt("numItems", 0)
		assertTrue "0 item count in results", numItems > 0
		
		def jlist = json.getJSONArray("items")
		assertNotNull jlist
		assertTrue "list is empty", jlist.size() > 0
		
		def jsonRec = jlist.get(0)
		assertNotNull "Item at pos 0 is null", jsonRec

		def createDate = jsonRec.optString(Dataport.STD_CREATE_DATE)
		assertNotNull "Standard re-mapped field 'createDate' is null", createDate 
		try {
			def date = new Dataport().getOutputDateFormatter().parse(createDate)
			assertNotNull "Parsed createDate is null", date
			
		} catch (Exception e) {
		    fail "Error raised when parsing createDate: [$e]"
		}
	}
	
	void testDataGen() {
		// Generate: dataport [test]
		//   name: len 2
		//   testDate: date    //REMOVED UNTIL TBD: issue with specifying format
		//   num: int 1..2
		//   floater: float 1..10.0
		//   picker: a|b
		
		def numRecords = 5  // Num to generate
		
		def args = "genSize=$numRecords&name=${RandomGenerator.TYPE_TEXT}max2&num=${RandomGenerator.TYPE_LONG}min1max2"
		args += "&floater=${RandomGenerator.TYPE_DOUBLE}min1max10&picker=${RandomGenerator.TYPE_PICK}a|b"
		// REMOVED:  &testDate=${RandomGenerator.TYPE_DATE}
		
		get("/gen/test?$args")
		
		def model = response.contentAsString
		log.debug "Response: $model"
		
		assertStatus 200
		def json = JSONObject.fromObject(model)
		def msg = json.optString("message", "")
		assertTrue "String response not received", msg?.startsWith("successfully generated")
		
		def dataport = Dataport.findByContextName('test')
		assertNotNull "Test dataport was not created", dataport
		
		// Retrieve records for this Dataport
		
		//TODO assert file was created and is non-empty! how to find it...?
		def dir = new File("..")
		log.debug " PATH: "
		log.debug "${dir}"
		
		// VERIFY THEY EXIST NOW
		get("/data/test")
		
		model = response.contentAsString
		log.debug "Response: $model"
		
		assertStatus 200
		json = JSONObject.fromObject(model)
		def numItems = json.optInt("numItems", 0)
		assertTrue "Returned [$numItems] records not the expected [$numRecords]", numItems > 0
		

	}

}
