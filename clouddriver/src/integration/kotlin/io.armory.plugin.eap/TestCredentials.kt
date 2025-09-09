package io.armory.plugin.eap

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition

/**
 * Test credentials class that implements CredentialsDefinition for integration tests
 */
class TestCredentials : CredentialsDefinition {
    private var nameValue: String = ""
    
    override fun getName(): String {
        return nameValue
    }
    
    fun setName(name: String) {
        this.nameValue = name
    }
}
