def call(Map config) {
    def releaseName = config.releaseName
    def chartPath = config.chartPath ?: '.'
    def namespace = config.namespace
    def setValues = config.setValues ?: []
    def awsCredentialsId = config.awsCredentialsId
    def clusterName = config.clusterName
    def region = config.region ?: 'us-east-1'

    if (!releaseName || !namespace || !clusterName || !awsCredentialsId) {
        error "releaseName, namespace, clusterName, awsCredentialsId required for EKS"
    }

       // EKS auth
         withAWS(credentials: awsCredentialsId, region: region) {
                    sh """
                    mkdir -p "$PWD/.kube"
                    export KUBECONFIG="$PWD/.kube/config"
                    aws eks update-kubeconfig --name ${clusterName} --region ${region}"
                    """
                }

            sh "helm lint ${chartPath}"
            
            def setArgs = setValues.collect { "--set ${it}" }.join(' ')
            sh """
                helm upgrade --install ${releaseName} ${chartPath} \
                --namespace ${namespace} \
                --create-namespace \
                ${setArgs} \
                --atomic \
                --timeout 5m \
                --wait \
                --history-max 5
            """
            sh """
      export KUBECONFIG=\$PWD/.kube/config
      helm status ${releaseName} -n ${namespace}
    """
}
