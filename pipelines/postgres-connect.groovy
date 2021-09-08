#!/usr/bin/env groovy
@GrabConfig(systemClassLoader=true)
@Grab(group='org.postgresql', module='postgresql', version='9.4-1205-jdbc42')

import groovy.sql.Sql
import java.sql.Driver

def driver = Class.forName('org.postgresql.Driver').newInstance() as Driver 

def props = new Properties()
props.setProperty("DB_user", "admin") 
props.setProperty("DB_password", "admin")

def conn = driver.connect("jdbc:postgresql://192.168.0.150:5432/keycloak", 'admin', 'admin') 
def sql = new Sql(conn)

try {
    sql.eachRow("select * from client;") {
        log.debug(it)
    }
} finally {
    sql.close()
    conn.close()
}