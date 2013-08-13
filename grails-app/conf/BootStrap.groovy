import grails.util.Environment
import ncc.owfdemo.Dataport
import ncc.owfdemo.Dataset

class BootStrap {

    def init = { servletContext ->
        
        println "Current Environment: ${Environment.current.name}"
        
        if (Environment.current.name != 'production') {
			
            println "Loading test data..."
			
			def dataport = Dataport.findByContextName('ufo-mini')
			if (!dataport) {
				new Dataport(contextName:'ufo-mini',
					endpoint:'file:///opt/projects/dataportal/data/ufo-mini.json',
					type: 'json',
					description:'UFO sighting data from a JSON file',
					mapId: 'id',
					mapName: 'location',
					mapDescr: 'description',
					mapLocation: 'location',
					mapCreateDate: 'reported_at',
					mapEventDate: 'sighted_at',
					mapCreateDateFormat: 'yyyyMMdd',
					mapEventDateFormat: 'yyyyMMdd'
				).save(flush:true)
			}
                
			dataport = Dataport.findByContextName('ufo')
			if (!dataport) {
				new Dataport(contextName:'ufo',
					endpoint:'file:///opt/projects/dataportal/data/ufo.json',
					type: 'json',
					description:'UFO sighting data from a JSON file',
					mapId: 'id',
					mapName: 'location',
					mapDescr: 'description',
					mapLocation: 'location',
					mapCreateDate: 'reported_at',
					mapEventDate: 'sighted_at',
					mapCreateDateFormat: 'yyyyMMdd',
					mapEventDateFormat: 'yyyyMMdd'
				).save(flush:true)
			}
            
        }
        
        if (Environment.current.name != 'production') {
            
            // DROP MONGO DB
            Dataset.collection.getDB().dropDatabase()
            
        }
    }
    
    def destroy = {
    }
}
