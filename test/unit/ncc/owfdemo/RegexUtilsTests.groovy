package ncc.owfdemo

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class RegexUtilsTests {

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }

    void testSomething() {
        
		def url = RegexUtils.parseUrl("file:/opt/projects/dataportal/web-app/test.json")
		
		assertNotNull "URL result is null", url
		assertTrue "Returned non-file scheme [${url.scheme}]", url.scheme == 'file'
		
    }
}
