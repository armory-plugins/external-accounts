## external-accounts

Dynamically read accounts from external sources.

The plugin can be divided in two parts:

1. Loader: Reads account credentials information from a single URL (`http(s)` or `file`), or from files in a directory in local clouddriver file system.
1. Poller: Populates the directory in local clouddriver file system, reading accounts information from external sources. Currently only git repositories are supported.

### Accounts in git repository, git poller sidecar

Clouddriver has a sidecar with a process that continuously retrieves account information from a git repository, then the plugin within clouddriver loads accounts from the clone.

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
          eap:
            dir: /tmp/accounts            # (Mandatory). Directory in clouddriver where git repo will be cloned and accounts will be loaded
            configFilePrefix:             # (Optional). Configures the file prefixes to look for account information within the directory
              default: clouddriver        # (Optional, default: clouddriver). All files with this prefix will be scanned for loading any type of account for the supported providers
              kubernetes: kube            # (Optional, default: kube). All files with this prefix will be scanned for loading kubernetes accounts
              cloudfoundry: cf            # (Optional, default: cf). All files with this prefix will be scanned for loading cloudfoundry accounts
              aws: aws                    # (Optional, default: aws). All files with this prefix will be scanned for loading AWS accounts
              ecs: ecs                    # (Optional, default: ecs). All files with this prefix will be scanned for loading ECS accounts
        credentials:
          poller:
            enabled: true
            types:
              kubernetes:                 # (Mandatory for each provider used: kubernetes, cloudfoundry, aws or ecs). Indicates how often account information should be read from the files
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

If authenticating to git using SSH, a secret with all relevant files (`id_rsa`, `known_hosts`) needs to be provided.


### Accounts in git repository, embedded jgit poller

The plugin inside clouddriver pulls the git repository using jgit, and loads account information.

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
          eap:
            dir: /tmp/eap-test
            jGitPoller:
              enabled: true
              syncIntervalSecs: 60                  # (Optional, default: 60). How often to do "git pull"
              repo: git@github.com:myorg/myrepo.git # (Mandatory). Git repo to clone
              branch: master                        # (Optional, default: master). Branch from the repo to clone
              username: john                        # (Optional). Used with user/password authentication
              password: secret                      # (Optional). Used with user/password authentication
              token: secret                         # (Optional). Used with token based authentication
              sshPrivateKeyFilePath: /id_rsa        # (Optional). Used with SSH authentication
              sshPrivateKeyPassphrase: secret       # (Optional). Used with SSH authentication
              sshKnownHostsFilePath: /known_hosts   # (Optional). Used with SSH authentication
              sshTrustUnknownHosts: false           # (Optional). Used with SSH authentication
            configFilePrefix:             # (Optional). Configures the file prefixes to look for account information within the directory
              default: clouddriver        # (Optional, default: clouddriver). All files with this prefix will be scanned for loading any type of account for the supported providers
              kubernetes: kube            # (Optional, default: kube). All files with this prefix will be scanned for loading kubernetes accounts
              cloudfoundry: cf            # (Optional, default: cf). All files with this prefix will be scanned for loading cloudfoundry accounts
              aws: aws                    # (Optional, default: aws). All files with this prefix will be scanned for loading AWS accounts
              ecs: ecs                    # (Optional, default: ecs). All files with this prefix will be scanned for loading ECS accounts
        credentials:
          poller:
            enabled: true
            types:
              kubernetes:                 # (Mandatory for each provider used: kubernetes, cloudfoundry, aws or ecs). Indicates how often account information should be read from the files
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
          eap:
            url: http://server.company/accounts     # (Mandatory). URL where to find account information
            urlContentFormat: JSON                  # (Mandatory). Content-Type response of the server. Supported formats are JSON and YAML
        credentials:
          poller:
            enabled: true
            types:
              kubernetes:                 # (Mandatory for each provider used: kubernetes, cloudfoundry, aws or ecs). Indicates how often account information should be read 
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
