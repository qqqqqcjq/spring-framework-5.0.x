package org.springframework.scripting.groovy

import org.springframework.beans.factory.FactoryBean

class TestFactoryBean implements FactoryBean {

	boolean isSingleton() {
		true
	}

    Class getObjectType() {
		String.class
	}

    Object getObject() {
		"test"
	}
}
