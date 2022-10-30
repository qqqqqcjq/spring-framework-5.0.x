package org.springframework.scripting.groovy

import org.springframework.scripting.Messenger

class GroovyMessenger implements Messenger {

	GroovyMessenger() {
		println "GroovyMessenger"
	}

    String message
}

return new GroovyMessenger()
