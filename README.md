# git-repo-plugin

This plugin allows you to add a git repository as a maven repo, even if the git
repository is private, similar to how CocoaPods works.

Using a github repo as a maven repo is a quick and easy way to host maven jars.
Private maven repos however, aren't easily accessible via the standard maven
http interface, or at least I haven't figured out how to get the authentication
right. This plugin simply clones the repo behind the scenes and uses it as a
local repo, so if you have permissions to clone the repo you can access it.

This plugin lets you tie access to your repository to github accounts,
seamlessly. This is most useful if you've already set up to manage distribution
this way. Deliver CocoaPods and Maven artifacts with the same system, then sit
back and relax.

## Building

Run `gradle publishToMavenLocal` or `gradle publish` to build and push a new version. The project can also be opened in intellij

## Usage

This plugin needs to be added via the standard plugin mechanism with this buildscript in your top level project

    buildscript {
        repositories {
            maven { url "wherever/we/put/it" }
        }
        dependencies {
            classpath group: 'com.layer', name: 'git-repo-plugin', version: '0.1.5'
        }
    }

and then apply the plugin

    apply plugin: 'git-repo'


### Depending on github repos

This plug adds a `github` method to your repositories block

    repositories {
        github("layerhq", "maven-private", "master", "releases")
    }

Add this alongside other repositories and you're good to go

### Publishing to github repos

Publishing is a bit less seamless, mostly because there isn't one single way to
handle publishing in maven (also the maven-publish plugin is infuratingly
tamper-proof). You're expected to have a task called `publish` by default, that
by publishes to the locally cloned github repo. That task gets wrapped into a
`publishToGithub` task that handles committing and pushing the change. Then you
can run

    gradle -Porg=layerhq -Prepo=android-releases publishToGithub

The `maven-publish` plugin defines a publish task for you, so you just need to
supply the right url in the publishing block

    publishing {
        publications {
            //...
        }
        repositories {
            maven {
                url "file://${System.env.HOME}/.gitRepos/${property("org")}/${property("repo")}/releases"
            }
        }
    }

A version of this with the `maven` plugin might look like

    String url() {
        String org =  hasProperty("org") ? property("org") : "layerhq"
        String repo = hasProperty("repo") ? property("repo") : "maven-private"
        String repoHome = hasProperty("gitRepoHome") ? property("gitRepoHome") : "${System.env.HOME}/.gitRepos"
        return "file://$repoHome/$org/$repo/releases"
    }
    
    task publishJar(type: Upload, description: "Upload android Jar library") {
        configuration = configurations.sdkJar
        repositories {
            mavenDeployer {
                repository(url: url())
                pom = sdkPom("jar", artifactIdName)
            }
        }
    }

## Futures

Accept closures for nicer syntax
