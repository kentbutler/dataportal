package ncc.owfdemo

import grails.test.mixin.*
import grails.test.mixin.support.*
import ncc.owfdemo.data.RandomGenerator

import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class RandomGeneratorTests {

    void setUp() {
        // Setup logic here
    }

    void tearDown() {
        // Tear down logic here
    }

    void testNullInputType() {
		def r = new RandomGenerator().getGenerator(null)
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.TextGenerator
		def val = r.getVal()
		assertNotNull "Generated random default was null", val
		assertTrue "Generated random default was empty", val.size() > 0
		assertTrue "Generated random default is not default size", val.size() == RandomGenerator.DEFAULT_TEXT_LEN
    }
    void testText() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_TEXT)
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.TextGenerator
		def val = r.getVal()
		assertNotNull "Generated random text was null", val
		assertTrue "Generated random text was empty", val.size() > 0
		assertTrue "Generated random text is not default size", val.size() == RandomGenerator.DEFAULT_TEXT_LEN
    }
    void testRandomInstance() {
		// we should always get back the same Random instance from a single RandomGenerator instance; assures randomness
		def gen = new RandomGenerator()
		def r, prev, cur
		10.times {
			r = gen.getGenerator(RandomGenerator.TYPE_TEXT)
			assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.TextGenerator
			cur = r.getGenerator()
			assertTrue "Generator [${cur}] is different from prev [${prev}]", prev ? (prev == cur) : true
			prev = cur
		}
    }
    void testTextOnlyMax() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_TEXT+"max55")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.TextGenerator
		def val = r.getVal()
		assertNotNull "Generated random text was null", val
		assertTrue "Generated random text was empty", val.size() > 0
		assertTrue "Generated random text is not 55", val.size() == 55
    }
    void testTextBadMax() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_TEXT+"maxasd")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.TextGenerator
		def val = r.getVal()
		assertNotNull "Generated random text was null", val
		assertTrue "Generated random text was empty", val.size() > 0
		assertTrue "Generated random text is not default size", val.size() == RandomGenerator.DEFAULT_TEXT_LEN
    }
    void testTextOnlyMin() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_TEXT+"min55")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.TextGenerator
		def val = r.getVal()
		assertNotNull "Generated random text was null", val
		assertTrue "Generated random text was empty", val.size() > 0
		assertTrue "Generated random text is not 55", val.size() == 55
    }
    void testTextMaxMin() {
		// Technically not  meaningful for TextGen, will always gen the max len
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_TEXT+"min55max66")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.TextGenerator
		def val = r.getVal()
		assertNotNull "Generated random text was null", val
		assertTrue "Generated random text was empty", val.size() > 0
		assertTrue "Generated random text is not 66", val.size() == 66
    }
	
	
	void testInt() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_LONG)
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.LongGenerator
		def val = r.getVal()
		assertNotNull "Generated random int was null", val
		assertTrue "Generated random int was too small ", val > RandomGenerator.DEFAULT_MIN_LONG
		assertTrue "Generated random int was too big ", val < RandomGenerator.DEFAULT_MAX_LONG
	}
	void testIntOnlyMax() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_LONG+"max55")
		def val 
		20.times {
			val = r.getVal()
			assertNotNull "Generated random int was null", val
			assertTrue "Generated random int [$val] was < MIN", val > RandomGenerator.DEFAULT_MIN_LONG
			assertTrue "Generated random int [$val] was > 55", val < 55
		}
	}
	void testIntOnlyMin() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_LONG+"min55")
		def val 
		20.times {
			val = r.getVal()
			assertNotNull "Generated random int was null", val
			assertTrue "Generated random int [$val] was < 55", val > 55
			assertTrue "Generated random int [$val] was > MAX", val < RandomGenerator.DEFAULT_MAX_LONG
		}
	}
	void testIntMaxMin() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_LONG+"min55max66")
		def val 
		20.times {
			val = r.getVal()
			assertNotNull "Generated random int was null", val
			assertTrue "Generated random int [$val] was < 55", val >= 55
			assertTrue "Generated random int [$val] was > 66", val <= 66
		}
	}
	void testIntCloseMaxMin() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_LONG+"min1max2")
		def val
		20.times {
			val = r.getVal()
			assertNotNull "Generated random int was null", val
			assertTrue "Generated random int [$val] was < 1", val >= 1
			assertTrue "Generated random int [$val] was > 2", val <= 2
		}
	}

	
	void testDouble() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_DOUBLE)
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.DoubleGenerator
		def val = r.getVal()
		assertNotNull "Generated random double was null", val
		assertTrue "Generated random double was too small ", val > RandomGenerator.DEFAULT_MIN_DOUBLE
		assertTrue "Generated random double was too big ", val < RandomGenerator.DEFAULT_MAX_DOUBLE
	}
	void testDoubleOnlyMax() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_DOUBLE+"max55")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.DoubleGenerator
		def val 
		20.times {
			val = r.getVal()
			assertNotNull "Generated random double was null", val
			assertTrue "Generated random double [$val] was < MIN", val > RandomGenerator.DEFAULT_MIN_DOUBLE
			assertTrue "Generated random double [$val] was > 55", val < 55
		}
	}
	void testDoubleOnlyMin() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_DOUBLE+"min55")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.DoubleGenerator
		def val 
		20.times {
			val = r.getVal()
			assertNotNull "Generated random double was null", val
			assertTrue "Generated random double [$val] was < 55", val > 55
			assertTrue "Generated random double [$val] was > MAX", val < RandomGenerator.DEFAULT_MAX_DOUBLE
		}
	}
	void testDoubleMaxMin() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_DOUBLE+"min55max66")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.DoubleGenerator
		def val 
		20.times {
			val = r.getVal()
			assertNotNull "Generated random double was null", val
			assertTrue "Generated random double [$val] was < 55", val >= 55
			assertTrue "Generated random double [$val] was > 66", val <= 66
		}
	}

	void testPick() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_PICK+"a|b|c")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.PickGenerator
		def vals = ['a','b','c']
		20.times {
		def val = r.getVal()
			assertNotNull "Generated random pick was null", val
			assertTrue "Generated random pick [$val] not in set", vals.contains(val)
		}
	}
	void testPick2() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_PICK+"a|b")
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.PickGenerator
		def vals = ['a','b']
		20.times {
		def val = r.getVal()
			assertNotNull "Generated random pick was null", val
			assertTrue "Generated random pick [$val] not in set", vals.contains(val)
		}
	}
	void testPickEmpty() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_PICK)
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.PickGenerator
		def vals = []
		def val = r.getVal()
		assertNotNull "Generated random pick was null", val
		assertTrue "Generated random pick [$val] not empty string", val == ""
	}

	void testDate() {
		def r = new RandomGenerator().getGenerator(RandomGenerator.TYPE_DATE)
		assertTrue "Wrong type [${r.class.name}]", r instanceof RandomGenerator.DateGenerator
		10.times {
			def val = r.getVal()
			assertNotNull "Generated random date was null", val
			assertTrue "Generate value not a Date [${val.class.name}]", val instanceof java.util.Date
		}
	}

}
