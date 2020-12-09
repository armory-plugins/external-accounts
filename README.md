## external-accounts

Dynamically read accounts from external sources

### Installation

The plugin can be installed using two different methods:
1. Docker image as an init container on clouddriver
1. Using a remote plugin repository

#### Docker image as init container

##### Spinnaker operator

This is a sample configuration to use with spinnaker operator:

```yaml
apiVersion: spinnaker.armory.io/v1alpha2
kind: SpinnakerService
metadata:
  name: spinnaker
spec:
  spinnakerConfig:
    profiles:
      clouddriver:
        armory:
          eap:
            git:
              enabled: true
              syncIntervalSecs: 5
              repo: https://github.com/myorg/myrepo
              branch: master
              repoSubdir: ""
        credentials:
          poller:
            enabled: true
            types:
              kubernetes:
                reloadFrequencyMs: 5000
        spinnaker:
          extensibility:
            plugins:
              Armory.EAP:
                enabled: true
            repositories:
              eap:
                enabled: true
                url: file:///opt/spinnaker/lib/local-plugins/eap/plugins.json
  kustomize:
    clouddriver:
      deployment:
        patchesStrategicMerge:
          - |
            spec:
              template:
                spec:
                  initContainers:
                  - name: eap
                    image: docker.io/armory/eap-plugin:<PLUGIN_VERSION>
                    volumeMounts:
                      - mountPath: /opt/eap/target
                        name: eap-vol
                  containers:
                  - name: clouddriver
                    volumeMounts:
                      - mountPath: /opt/spinnaker/lib/local-plugins
                        name: eap-vol
                  volumes:
                  - name: eap-vol
                    emptyDir: {}
```

##### Halyard

Content for `profiles/clouddriver-local.yml`:
```yaml
armory:
  eap:
    git:
      enabled: true
      syncIntervalSecs: 5
      repo: https://github.com/myorg/myrepo
      branch: master
      repoSubdir: ""
credentials:
  poller:
    enabled: true
    types:
      kubernetes:
        reloadFrequencyMs: 5000
spinnaker:
  extensibility:
    plugins:
      Armory.EAP:
        enabled: true
    repositories:
      eap:
        enabled: true
        url: file:///opt/spinnaker/lib/local-plugins/eap/plugins.json
```

Content for `service-settings/clouddriver.yml`:
```yaml
kubernetes:
  volumes:
  - id: eap-vol
    type: emptyDir
    mountPath: /opt/spinnaker/lib/local-plugins
```

Content for `.hal/config`:
```yaml
deploymentConfigurations:
  - name: default
    deploymentEnvironment:
      initContainers:
        clouddriver:
          - name: eap
            image: docker.io/armory/eap-plugin:<PLUGIN_VERSION>
            volumeMounts:
              - mountPath: /opt/eap/target
                name: eap-vol
```

#### Remote plugin repository

The configuration is mostly the same as with the docker image method, but omitting all volumes and init container configurations, and replacing all occurrences of 

```yaml
url: file:///opt/spinnaker/lib/local-plugins/eap/plugins.json
``` 

 with:
 
```yaml
url: https://armory.jfrog.io/artifactory/plugins/eap/plugins.json
``` 
