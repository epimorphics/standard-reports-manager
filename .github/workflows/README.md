# github-workflows

## Reusble Workflows

The following is a example workflow for a client repository that creates docker images and uploads this artifact to AWS Elactic Container Repository.

In the case of Kubernetes deployment the `deploy` stansa may be deleted (and the file renamed `publish.yaml`).

__Note__: The repository specific sections will require updating.

`publish-deploy.yml`
```
name: "Build & Publish Docker Image"
on:
  workflow_dispatch: {}
  push: {}

jobs:
  publish:
    uses:  "epimorphics/github-workflows/.github/workflows/publish.yml@reusable"
    secrets:
      # Repostory specific
      aws_access_key_id:     "${{ secrets.BUILD_XXXX_AWS_ACCESS_KEY_ID }}"
      aws_secret_access_key: "${{ secrets.BUILD_XXXX_AWS_SECRET_ACCESS_KEY }}"
      # Fixed
      epi_gpr_access_token:  "${{ secrets.EPI_GPR_ACCESS_TOKEN }}"
  deploy: 
    needs: "publish"
    uses:  "epimorphics/github-workflows/.github/workflows/deploy.yml@reusable"
    with:
      # Repostory specific
      ansible_repo:     epimorphics/xxxx-ansible-deployment
      ansible_repo_ref: master/main
      host_prefix:      xxxx
      # Fixed
      deploy: "${{ needs.publish.outputs.deploy }}"
      key:    "${{ needs.publish.outputs.key }}"
      tag:    "${{ needs.publish.outputs.tag }}"
    secrets:
      # Repostory specific
      ansible_vault_password: "${{ secrets.XXXX_ANSIBLE_VAULT_PASSWORD }}"
      aws_access_key_id:      "${{ secrets.BUILD_XXXX_AWS_ACCESS_KEY_ID }}"
      aws_secret_access_key:  "${{ secrets.BUILD_XXXX_AWS_SECRET_ACCESS_KEY }}"
      ssh_key:                "${{ secrets.XXXX_SSH_KEY }}"
      # Fixed
      github_pat: "${{ secrets.GIT_REPOSITORY_FULL_ACCESS_PAT }}"
```

## Using github-workflows as a subrepo DEPRICATED

This repository is intended to hold common github workflow scripts.

Each customer installation has its own branch. There is also a branch `expt` for publishing to our own ECR registry.

The workflow should be installed into a applications repository using
git-subrepo. See https://github.com/ingydotnet/git-subrepo#installation-instructions.


To copy this subrepo into an application's repository:
```
git subrepo clone git@github.com:epimorphics/github-workflows.git .github/workflows -b hmlr
```

To update this subrepo in an application's repository:
```
git subrepo pull .github/workflows
```

## Requirements

Whether using reusable workflows or github-workflows as a subrepo, the common workflows makes the same requirements of the client/application repository. 

### Makefile 

This sub-repository makes certain requirements of the application `Makefile`.

|Target|Result|
|---|---|
| tag | Output the docker tag of the image to be published or deployed |
| image | Build the docker image |
| publish | Write the docker image to ECR |

### deployment.yaml

The mapping of application source repository branch/tag to published ECR location and deployed environment is controlled by the `deployment.yaml` configuration file. For the specification of this file see [here](https://github.com/epimorphics/deployment-mapper#version-2).
