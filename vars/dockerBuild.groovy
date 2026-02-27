// vars/dockerBuild.groovy
// Usage in Jenkinsfile:
// dockerBuild([
//   registryUrl  : "123456789012.dkr.ecr.ap-south-1.amazonaws.com",
//   repo         : "my-service",
//   tag          : env.BUILD_ID,
//   credentialsId: "ecr:ap-south-1:aws-jenkins-creds"
// ])

def call(Map cfg) {
    // 1. Validate required keys
    def required = ['registryUrl', 'tag', 'credentialsId']
    required.each { key ->
        if (!cfg.containsKey(key) || cfg[key] == null || cfg[key].toString().trim() == '') {
            error "dockerBuild: Missing required parameter '${key}'. Got cfg=${cfg}"
        }
    }

    // 2. Build registry URL and image name
    String registry  = "https://${cfg.registryUrl}"
    String imageName = "${cfg.registryUrl}:${cfg.tag}"

    // 3. Login to registry, build and push image
    docker.withRegistry(registry, cfg.credentialsId) {
        def image = docker.build(imageName)
        image.push()          // pushes :tag
        image.push('latest')  // pushes :latest
        echo "Pushed ${imageName} and latest to ${registry}"
    }
}
