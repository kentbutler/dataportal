class UrlMappings {

	static mappings = {
		/*
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}
		*/
		"/data/$contextName/$id?"{
			controller="dataport"
			action="proxy"
		}
		"/gen/$contextName" {
			controller="dataport"
			action="generate"
		}
		"/"(view:"/index")
		"500"(view:'/error')
	}
}
