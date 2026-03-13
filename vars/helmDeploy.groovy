def call(Map config) {
    def releaseName      = config.releaseName
    def chartPath        = config.chartPath ?: '.'
    def namespace        = config.namespace
    def setValues        = config.setValues ?: []
    def awsCredentialsId = config.awsCredentialsId
    def clusterName      = config.clusterName
    def region           = config.region ?: 'us-east-1'

    if (!releaseName || !namespace || !clusterName || !awsCredentialsId) {
        error "releaseName, namespace, clusterName, awsCredentialsId required for EKS"
    }
    // EKS auth
    //withAWS(credentials: awsCredentialsId, region: region) {
    //    sh """
    //     aws eks update-kubeconfig --name ${clusterName} --region ${region}
    //    """
    def regularValues = setValues.findAll { !it.startsWith('ingress.annotations.') }
    def annotationValues = setValues.findAll { it.startsWith('ingress.annotations.') }

    def setArgs = regularValues.collect { "--set ${it}" }.join(' ')

    if (annotationValues) {
        def annotationsMap = annotationValues.collectEntries { entry ->
            def parts = entry.split('=', 2)
            def key = parts[0].replace('ingress.annotations.', '')
            [(key): parts[1]]
        }
        def jsonAnnotations = groovy.json.JsonOutput.toJson(annotationsMap)
        setArgs += " --set-json 'ingress.annotations=${jsonAnnotations}'"
    }

    sh """
     #helm lint ${chartPath}
    """

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
        helm status ${releaseName} -n ${namespace}
    """
}
