# Run using Kubernetes and Helm

If you have Kubernetes server then you can install using [Helm](https://helm.sh/docs/).

Add helm chart repository (once)

```console
helm repo add emx2 https://github.com/molgenis/molgenis-ops-helm/tree/master/charts/molgenis-emx2
```

Run the latest release (see [Helm docs](https://helm.sh/docs/intro/using_helm/))

```console
helm install emx2/emx2
```

Update helm repository to get newest release

```console
helm repo update
```
