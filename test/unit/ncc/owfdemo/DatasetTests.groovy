package ncc.owfdemo

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import net.sf.json.JSONObject

import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class DatasetTests {

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }
	
    void testStaticCreate() {
		setUp();
		def dataport = new Dataport(contextName:'ufo-test',endpoint:'file:///opt/projects/dataportal/data/ufo-mini.json',type:'json',description:'Test description')
		dataport.mapUuid = '_uuid'
		dataport.mapName = '_name'
		dataport.mapDescr = '_description'
		dataport.mapLocation = '_location'
		dataport.mapCreateDate = '_reported_at'
		dataport.mapEventDate = '_sighted_at'
		dataport.mapCreateDateFormat = 'YYYYMMdd'
		dataport.mapEventDateFormat = 'YYYYMMdd'
		
		JSONObject json = new JSONObject()
		json.put('_uuid', '5')
		json.put('_name', 'oo115')
		json.put('_description', 'test')
		json.put('_location', '332542')
		json.put('_reported_at', '20001111')
		json.put('_sighted_at', '20001111')
		// extra fields
		json.put('key1', 'val1')
		json.put('key2', 'val2')
		
		def dset = Dataset.fromJson dataport, json

		assertNotNull "Dataset is null", dset
		
		// Std fields
		assertTrue "id incorrect", dset.uuid == '5'
		assertTrue "name incorrect", dset.name == 'oo115'
		assertTrue "description incorrect", dset.description == 'test'
		assertTrue "location incorrect", dset.location == '332542'
		assertTrue "createDate incorrect", dset.createDate == '20001111'
		assertTrue "eventDate incorrect", dset.eventDate == '20001111'
		
		// Extra fields
		assertNotNull "Field 1 is empty", dset.fields['key1']
		assertNotNull "Field 2 is empty", dset.fields['key2']
		assertTrue "Field 1 val incorrect", dset.fields['key1'] == 'val1'
		assertTrue "Field 2 val incorrect", dset.fields['key2'] == 'val2'
		
    }
}
