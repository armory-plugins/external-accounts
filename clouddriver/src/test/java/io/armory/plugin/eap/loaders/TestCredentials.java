/*
 * Copyright 2020 Armory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.armory.plugin.eap.loaders;

import com.netflix.spinnaker.credentials.definition.CredentialsDefinition;
import java.util.List;

/**
 * Simple test credentials implementation for testing
 */
public class TestCredentials implements CredentialsDefinition {
    private String name;
    private String address;
    private String email;
    private int cacheIntervalSeconds;
    private int clientTimeoutMillis;
    private int cacheThreads;
    private int paginateSize;
    private boolean sortTagsByDate;
    private boolean trackDigests;
    private boolean insecureRegistry;
    private List<String> repositories;
    
    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public int getCacheIntervalSeconds() {
        return cacheIntervalSeconds;
    }
    
    public void setCacheIntervalSeconds(int cacheIntervalSeconds) {
        this.cacheIntervalSeconds = cacheIntervalSeconds;
    }
    
    public int getClientTimeoutMillis() {
        return clientTimeoutMillis;
    }
    
    public void setClientTimeoutMillis(int clientTimeoutMillis) {
        this.clientTimeoutMillis = clientTimeoutMillis;
    }
    
    public int getCacheThreads() {
        return cacheThreads;
    }
    
    public void setCacheThreads(int cacheThreads) {
        this.cacheThreads = cacheThreads;
    }
    
    public int getPaginateSize() {
        return paginateSize;
    }
    
    public void setPaginateSize(int paginateSize) {
        this.paginateSize = paginateSize;
    }
    
    public boolean getSortTagsByDate() {
        return sortTagsByDate;
    }
    
    public void setSortTagsByDate(boolean sortTagsByDate) {
        this.sortTagsByDate = sortTagsByDate;
    }
    
    public boolean getTrackDigests() {
        return trackDigests;
    }
    
    public void setTrackDigests(boolean trackDigests) {
        this.trackDigests = trackDigests;
    }
    
    public boolean isInsecureRegistry() {
        return insecureRegistry;
    }
    
    public void setInsecureRegistry(boolean insecureRegistry) {
        this.insecureRegistry = insecureRegistry;
    }
    
    public List<String> getRepositories() {
        return repositories;
    }
    
    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }
}
