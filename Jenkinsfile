/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(useContainerAgent: true, configurations: [
  [ platform: 'linux', jdk: '11'],
  // Remove the Jenkins version once baseline version bumped to one where Java 17 is supported
  [ platform: 'linux', jdk: '17', jenkins: '2.356' ],
])
