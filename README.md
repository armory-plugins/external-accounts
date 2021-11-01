## external-accounts plugin

Dynamically read accounts from external sources.

The plugin reads account credentials information from a single URL (`http(s)` or `file`):

```yaml
armory:
  external-accounts:
    url: (http|https|file)
    url-content-format: JSON|YAML
```

Or from files in a directory in local clouddriver file system:

```yaml
armory:
  external-accounts:
    dir: /tmp/accounts
    file-prefix:
      default: clouddriver
      kubernetes: kube
```

Any sidecar can populate the accounts directory by pulling information from other sources, and this repository includes an example of how to do that pulling accounts from a git repository. 


### Spinnaker compatibility

|Min. version|Notes|
|----|-----------|
|1.23 |Kubernetes and cloudfoundry implemented|
|1.24 |AWS and ECS implemented|

|Spinnaker version|Plugin version|
|----|-----------|
|< 1.27|0.1.x|
| \>= 1.27|0.2.x|

NOTE: The plugin is not actively tested in all compatible versions with all variants, but is expected to work in the above.


### Accounts in git repository, git poller sidecar

A sidecar is deployed to clouddriver that continuously retrieves account information from a git repository, then the plugin within clouddriver loads those accounts from the git clone.

Example configuration using spinnaker operator:

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
          external-accounts:
            dir: /tmp/accounts       # (Mandatory). directory in clouddriver where git repo will be cloned and accounts will be loaded from
            file-prefix:             # (Optional). Configures the file prefixes to look for account information within the directory
              default: clouddriver   # (Optional, default: clouddriver). All files with this prefix will be scanned for loading any type of account for the supported providers
              kubernetes: kube       # (Optional, default: kube). All files with this prefix will be scanned for loading kubernetes accounts
              cloudfoundry: cf       # (Optional, default: cf). All files with this prefix will be scanned for loading cloudfoundry accounts
              aws: aws               # (Optional, default: aws). All files with this prefix will be scanned for loading AWS accounts
              ecs: ecs               # (Optional, default: ecs). All files with this prefix will be scanned for loading ECS accounts
        credentials:
          poller:
            enabled: true
            types:
              kubernetes:            # (Mandatory for each provider used: kubernetes, cloudfoundry, aws or ecs). Indicates how often account information should be read from the files
                reloadFrequencyMs: 60000
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
                  containers:
                  - name: eap
                    image: docker.io/armory/eap-plugin:<PLUGIN_VERSION>
                    command:
                    - git-poller 
                    env:
                    - name: REPO
                      value: "git@github.com:myorg/myrepo.git"    # Git repository to clone
                    - name: LOCAL_CLONE_DIR
                      value: "/tmp/accounts"                      # Should match the value in armory.eap.dir
                    - name: SYNC_INTERVAL_SECS
                      value: "60"                                 # How often to do a git pull
                    volumeMounts:
                      - mountPath: /opt/eap/target
                        name: eap-plugin
                      - mountPath: "/tmp/accounts"                
                        name: git-repo
                      - mountPath: /root/.ssh                     # Only needed if authenticating to git using SSH
                        name: ssh-keys
                  - name: clouddriver
                    volumeMounts:
                      - mountPath: /opt/spinnaker/lib/local-plugins
                        name: eap-plugin
                      - mountPath: /tmp/accounts                  # Should match the value in armory.eap.dir
                        name: git-repo
                  volumes:
                  - name: git-repo
                    emptyDir: {}
                  - name: eap-plugin
                    emptyDir: {}
                  - name: ssh-keys
                    secret:
                      secretName: ssh-keys                        # Only needed if authenticating to git using SSH
                      defaultMode: 0600
```

Available environment variables for git poller sidecar

|Name|Description|Default|
|----|-----------|----------|
|REPO |Git repository to clone|-|
|LOCAL_CLONE_DIR |Path in local file system to clone the repo |Automatically generated dir int /tmp|
|SYNC_INTERVAL_SECS |How often to call "git pull" in seconds|60|
|GIT_USER |Username for git authentication |-|
|GIT_PASS |Password for git authentication |-|
|TOKEN |Personal access token to use for authentication|-|

If authenticating to git using SSH, a secret with all relevant files (`id_rsa`, `known_hosts`) needs to be provided.


### Accounts in git repository, embedded git poller

In this case the plugin inside clouddriver pulls the git repository and loads account information. All git interaction is done through shell calls to `git` binary, and if that is not found the plugin fallbacks to using `jgit`. `jgit` has known performance issues, so it's recommended to be used only if the other options are not available.

Example configuration using spinnaker operator:

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
          external-accounts:
            dir: /tmp/accounts                      # (Mandatory). directory in clouddriver where git repo will be cloned and accounts will be loaded from
            git-poller:
              enabled: true
              sync-interval-secs: 5                 # (Optional, default: 60). How often to do "git pull"
              repo: git@github.com:myorg/myrepo.git # (Mandatory). Git repo to clone
              branch: master                        # (Optional, default: master). Branch from the repo to clone
              username: john                        # (Optional). Used with user/password authentication
              password: secret                      # (Optional). Used with user/password authentication
              token: secret                         # (Optional). Used with token based authentication
              ssh-private-key-file-path: ${SSH_KEYS}/id_rsa       # (Optional). Used with SSH authentication
              ssh-private-key-passphrase: secret                  # (Optional). Used with SSH authentication
              ssh-known-hosts-file-path: ${SSH_KEYS}/known_hosts  # (Optional). Used with SSH authentication
              ssh-trust-unknown-hosts: false                      # (Optional). Used with SSH authentication
            file-prefix:             # (Optional). Configures the file prefixes to look for account information within the directory
              default: clouddriver   # (Optional, default: clouddriver). All files with this prefix will be scanned for loading any type of account for the supported providers
              kubernetes: kube       # (Optional, default: kube). All files with this prefix will be scanned for loading kubernetes accounts
              cloudfoundry: cf       # (Optional, default: cf). All files with this prefix will be scanned for loading cloudfoundry accounts
              aws: aws               # (Optional, default: aws). All files with this prefix will be scanned for loading AWS accounts
              ecs: ecs               # (Optional, default: ecs). All files with this prefix will be scanned for loading ECS accounts
        credentials:
          poller:
            enabled: true
            types:
              kubernetes:             # (Mandatory for each provider used: kubernetes, cloudfoundry, aws or ecs). Indicates how often account information should be read from the files
                reloadFrequencyMs: 60000
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
                        name: eap-plugin
                  containers:
                  - name: clouddriver
                    volumeMounts:
                      - mountPath: /opt/spinnaker/lib/local-plugins
                        name: eap-plugin
                  volumes:
                  - name: eap-plugin
                    emptyDir: {}
```

Alternatively, the plugin can be installed from a remote plugin repository by replacing `spec.spinnakerConfig.profiles.clouddriver.spinnaker.extensibility.plugins.repositories.eap.url` with the URL of the repository.

### Accounts in a remote http server

The plugin inside clouddriver makes requests to the remote server and loads the accounts information it finds.

Example configuration using spinnaker operator:

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
          external-accounts:
            url: http://server.company/accounts     # (Mandatory). URL where to find account information
            url-content-format: JSON|YAML           # (Mandatory). Content-Type response of the server. Supported formats are JSON and YAML
        credentials:
          poller:
            enabled: true
            types:
              kubernetes:                           # (Mandatory for each provider used: kubernetes, cloudfoundry, aws or ecs). Indicates how often account information should be read 
                reloadFrequencyMs: 60000
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
                        name: eap-plugin
                  containers:
                  - name: clouddriver
                    volumeMounts:
                      - mountPath: /opt/spinnaker/lib/local-plugins
                        name: eap-plugin
                  volumes:
                  - name: eap-plugin
                    emptyDir: {}
```

Alternatively, the plugin can be installed from a remote plugin repository by replacing `spec.spinnakerConfig.profiles.clouddriver.spinnaker.extensibility.plugins.repositories.eap.url` with the URL of the repository.

### Expected accounts layout

This plugin can read account credentials in the following layouts:

* All accounts from all providers in one file. Example:

    ```yaml
    kubernetes:
      accounts:
      - name: kube-1
    ...

    cloudfoundry:
      accounts:
      - name: cf-1
      ...
    ```

* Many accounts from a single provider in one file. Example:

    ```yaml
    - name: kube-1
    - name: kube-2
    ...
    ```

* One account per file. Example:

    ```yaml
    name: kube-1
    ...
    ```
