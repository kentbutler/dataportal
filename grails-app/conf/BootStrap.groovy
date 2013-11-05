import grails.util.Environment
import ncc.owfdemo.Dataport
import ncc.owfdemo.Dataset

class BootStrap {

    def init = { servletContext ->
        
        println "Current Environment: ${Environment.current.name}"
        def dataport
        
        if (Environment.current.name != 'production') {
			
            println "Loading test data..."
			
			dataport = Dataport.findByContextName('ufomini')
			if (!dataport) {
				new Dataport(contextName:'ufomini',
					endpoint:'file:///opt/projects/dataportal/data/ufomini.json',
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
        
        dataport = Dataport.findByContextName('CCDBRestService')
        if (!dataport) {
            new Dataport(contextName:'CCDBRestService',
                endpoint:'http://10.1.5.14:8080/CCDBService-1.0-SNAPSHOT/CCDBRestService',
                type: 'json',
                description:'JIATFS HTTP data service',
                mapId: '',
                mapName: '',
                mapDescr: '',
                mapLocation: '',
                mapCreateDate: '',
                mapEventDate: '',
                mapCreateDateFormat: '',
                mapEventDateFormat: ''
            ).save(flush:true)
        }
            
            
        if (Environment.current.name != 'production') {
            
            // DROP MONGO DB
            Dataset.collection.getDB().dropDatabase()
            
        }
    }
    
    def destroy = {
    }
}
