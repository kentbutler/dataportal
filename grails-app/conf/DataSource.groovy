hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}
// environment specific settings
environments {
    development {
		dataSource {
			// run as create-drop for initial testing; note that you must clear mongo manually
			dbCreate = "create-drop"  
			driverClassName="com.mysql.jdbc.Driver"
			url="jdbc:mysql://localhost:3306/dataportal_dev"
			dialect="org.hibernate.dialect.MySQL5InnoDBDialect"
			username="root"
			password="dbpa55"
			pooled=true
			properties {
				testOnBorrow = true
				testWhileIdle = true
				testOnReturn = true
				validationQuery = "SELECT 1"
				numTestsPerEvictionRun = 3
				timeBetweenEvictionRunsMillis = 180000
				minEvictableIdleTimeMillis = 180000
			}
		}
        grails {
            mongo {
                host = "localhost"
                databaseName = "datadev"
                port = 27017
                //username = "blah"
                //password = "blah"
            }
        }
    }
    test {
		dataSource {
			// run as create-drop for initial testing; note that you must clear mongo manually
			dbCreate = "create-drop"  
			driverClassName="com.mysql.jdbc.Driver"
			url="jdbc:mysql://localhost:3306/dataportal_test"
			dialect="org.hibernate.dialect.MySQL5InnoDBDialect"
			username="root"
			password="dbpa55"
			pooled=true
			properties {
				testOnBorrow = true
				testWhileIdle = true
				testOnReturn = true
				validationQuery = "SELECT 1"
				numTestsPerEvictionRun = 3
				timeBetweenEvictionRunsMillis = 180000
				minEvictableIdleTimeMillis = 180000
			}
		}
        grails {
            mongo {
                host = "localhost"
                databaseName = "datatest"
                port = 27017
                //username = "blah"
                //password = "blah"
            }
        }
    }
    production {
		dataSource {
			// run as create-drop for initial testing; note that you must clear mongo manually
			dbCreate = "update"  
			driverClassName="com.mysql.jdbc.Driver"
			url="jdbc:mysql://localhost:3306/dataportal"
			dialect="org.hibernate.dialect.MySQL5InnoDBDialect"
			username="root"
			password="dbpa55"
			pooled=true
			properties {
				testOnBorrow = true
				testWhileIdle = true
				testOnReturn = true
				validationQuery = "SELECT 1"
				numTestsPerEvictionRun = 3
				timeBetweenEvictionRunsMillis = 180000
				minEvictableIdleTimeMillis = 180000
			}
		}
        grails {
            mongo {
                host = "localhost"
                databaseName = "dataportal"
                port = 27017
                //username = "blah"
                //password = "blah"
            }
        }
    }
}
