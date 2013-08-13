package ncc.owfdemo

import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class RequestUtilsTests {

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }

    void testFinderName() {
        def m = ['name':'jon', 'age':'44']
		def f = RequestUtils.createFinderName(m)
		assertNotNull "Finder name is null", f
		assertTrue "Finder name is empty", f.size() > 0
		assertTrue "Finder name [$f] is simply wrong", f == "findAllByNameAndAge"
    }		
	
    void testFinderNameLike() {
        def m = ['descriptionLike':'jon', 'max':'25','action':'list','controller':'goose','sort':'asc']
		def f =  RequestUtils.createFinderName(m)
		assertNotNull "Finder name is null", f
		assertTrue "Finder name is empty", f.size() > 0
		assertTrue "Finder name [$f] is simply wrong", f == "findAllByDescriptionLike"
    }
	
	void testExtractSearchTokensMap() {
        def m = ['descriptionLike':'test', 'name':'jon', 'age':'44','max':'25','action':'list','controller':'goose','sort':'asc']
		def out = RequestUtils.extractSearchKeys(m)
		
		assertNotNull "Out list is null", out
		assertTrue "Out list is empty", out.size() > 0
		assertTrue "Out list is not 3 in size", out.size() == 3
		assertTrue "Out list does not contain name", out.contains('name')
		assertTrue "Out list does not contain age", out.contains ('age')
		assertTrue "Out list does not contain descriptionLike", out.contains ('descriptionLike')
	}
	void testExtractSearchTokensList() {
		def m = ['descriptionLike', 'name', 'age','max','action','controller','sort']
		def out = RequestUtils.extractSearchKeys(m)
		
		assertNotNull "Out list is null", out
		assertTrue "Out list is empty", out.size() > 0
		assertTrue "Out list is not 3 in size", out.size() == 3
		assertTrue "Out list does not contain name", out.contains('name')
		assertTrue "Out list does not contain age", out.contains ('age')
		assertTrue "Out list does not contain descriptionLike", out.contains ('descriptionLike')
	}

	void testExtractSearchTokensListExcl() {
		def m = ['descriptionLike', 'name', 'age','max','action','controller','sort']
		def excl = ['max','action','controller','sort']
		def out = RequestUtils.extractSearchKeys(m, excl)
		
		assertNotNull "Out list is null", out
		assertTrue "Out list is empty", out.size() > 0
		assertTrue "Out list is not 3 in size", out.size() == 3
		assertTrue "Out list does not contain name", out.contains('name')
		assertTrue "Out list does not contain age", out.contains ('age')
		assertTrue "Out list does not contain descriptionLike", out.contains ('descriptionLike')
		assertTrue "Out list contains max", !out.contains('max')
		assertTrue "Out list contains action", !out.contains('action')
		assertTrue "Out list contains controller", !out.contains('controller')
		assertTrue "Out list contains sort", !out.contains('sort')
	}

	void testPrepareSearchParamsSql() {
		def m = ['descriptionLike':'test', 'name':'jon', 'age':'44','max':'25','action':'list','controller':'goose','sort':'asc']
		def out = RequestUtils.extractSearchValues(m)
		
		assertNotNull "Out map is null", out
		assertTrue "Out map is empty", out.size() > 0
		assertTrue "Out map is not 3 in size", out.size() == 3
		assertNotNull "Out map does not contain name value: $out", out["name"]
		assertNotNull "Out map does not contain age value: $out", out["age"]
		assertTrue "Out map does not contain description value: $out", out["descriptionLike"] == "%test%"
	}

	void testPrepareSearchParamsMongo() {
		def m = ['descriptionLike':'test', 'name':'jon', 'age':'44','max':'25','action':'list','controller':'goose','sort':'asc']
		def out = RequestUtils.extractSearchValues(m, RequestUtils.MONGO)
		
		assertNotNull "Out map is null", out
		assertTrue "Out map is empty", out.size() > 0
		assertTrue "Out map is not 3 in size", out.size() == 3
		assertNotNull "Out map does not contain name value: $out", out["name"]
		assertNotNull "Out map does not contain age value: $out", out["age"]
		assertTrue "Out map does not contain description value: $out", out["descriptionLike"] == "/test/"
	}
}
