package ncc.owfdemo.data

class RandomGenerator {
	
	static final TYPE_DATE = 'RANDDATE'
	static final TYPE_TEXT = 'RANDTEXT'
	static final TYPE_LONG = 'RANDINT'
	static final TYPE_DOUBLE = 'RANDFLOAT'
	static final TYPE_PICK = 'RANDPICK'
	
	static final int DEFAULT_TEXT_LEN = 15
	static final long DEFAULT_MAX_LONG = 32767
	static final long DEFAULT_MIN_LONG = -32767
	static final double DEFAULT_MAX_DOUBLE = 32767d
	static final double DEFAULT_MIN_DOUBLE = -32767d
	static final long DEFAULT_MIN_DATE = -3313422000000 // 1/1/1865
	
	static final def MIN_PATTERN = ~/.*min([0-9]+).*/
	static final def MAX_PATTERN = ~/.*max([0-9]+).*/
  
	long seed
	Random rand
	
	/**
	 * Factory method to get an appropriate type of Generator
	 */
	def getGenerator(def type) {
		def ret
		if (type?.startsWith(TYPE_TEXT)) {
			ret = new TextGenerator(type)
		}
		else if (type?.startsWith(TYPE_DATE)) {
			ret = new DateGenerator(type)
		}
		else if (type?.startsWith(TYPE_LONG)) {
			ret =  new LongGenerator(type)
		}
		else if (type?.startsWith(TYPE_DOUBLE)) {
			ret =  new DoubleGenerator(type)
		}
		else if (type?.startsWith(TYPE_PICK)) {
			ret =  new PickGenerator(type)
		}
		else {
			ret = new TextGenerator(type)
		}
		return ret
	}
	
	long getSeed() {
		seed ?: (seed = new Date().getTime())
		return seed
	}
	Random getRandom() {
		rand ?: (rand = new Random(getSeed()))
		return rand
	}
	
	
	class DateGenerator extends ConstrainedGenerator {
		
		DateGenerator(type) {
			super(type,DEFAULT_MIN_DATE, new Date().time)
		}
		Date getVal() {
			// Generate at wild random
			def val = (long)min + (long)(getRandom().nextFloat() * (max-min))
			//def delta = curTime + Math.abs(DEFAULT_MIN_DATE)
			//def v = (long)(r * delta)
			return new Date(val)
		}
	}
	
	// Base class for generators that have a Max length
	class ConstrainedGenerator {
		def min,max
		def defaultMin
		def defaultMax
		ConstrainedGenerator(type, _defaultMin, _defaultMax) {
			defaultMin = _defaultMin
			defaultMax = _defaultMax
			// if there was a max provided it's in the second position
			min = parseParam(type, MIN_PATTERN, defaultMin)			
			max = parseParam(type, MAX_PATTERN, defaultMax)		
			
			if (min > max) {
				max = min
			}	
		}
		def parseParam (def val, def pattern, def dflt) {
			try {
				def strVal, m1 = (val =~ pattern)
				
				if (m1 && m1[0]?.size() > 0) {
					strVal = m1[0][1]
					if (strVal?.size() > 0) {
						return Integer.parseInt(strVal)
					}
				}
			}
			catch (Exception e) {
			}
			return dflt
		}
	}
	
	class TextGenerator extends ConstrainedGenerator {
		TextGenerator(type) {
			super(type, 1, DEFAULT_TEXT_LEN)
		}
		String getVal() {
			def out = ""
			def val
			max.times {
			   // Caps: 65-90
			   val = getRandom().nextInt(26) + 65
			   out += (char)val
			}
			return out
		 }
		 def getGenerator() {
			 return getRandom()
		 }
	}
	class LongGenerator extends ConstrainedGenerator {
		LongGenerator(type) {
			super(type,DEFAULT_MIN_LONG,DEFAULT_MAX_LONG)
		}
		long getVal() {
			return (long)min + (long)(getRandom().nextFloat() * (max-min))
		}
	}
	class DoubleGenerator extends ConstrainedGenerator {
		DoubleGenerator(type) {
			super(type,DEFAULT_MIN_DOUBLE,DEFAULT_MAX_DOUBLE)
		}
		double getVal() {
			return min + (getRandom().nextDouble() * (max-min))
		}
	}
	class PickGenerator {
		def values = []
		PickGenerator(type) {
			values = type ? type.substring(TYPE_PICK.size()).tokenize("|") : []
		}
		String getVal() {
			if (!values || values.size() == 0) return ""
			// just randomly generate the index
			return values[(int)(getRandom().nextFloat() * (values.size()))]
		}
	}
}
