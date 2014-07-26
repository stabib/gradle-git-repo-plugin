# The Gradle Git Repo Plugin

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

Run `gradle build` to build, and `gradle -Porg=layerhq -Prepo=gradle-releases publishToGithub`
to publish. The plugin uses itself to publish itself :).

## Usage

This plugin needs to be added via the standard plugin mechanism with this buildscript in your top level project

    buildscript {
        repositories {
            maven { url "https://github.com/layerhq/releases-gradle/raw/master/releases" }
        }
        dependencies {
            classpath group: 'com.layer', name: 'git-repo-plugin', version: '1.0.0'
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
handle publishing in gradle (also the maven-publish plugin is infuratingly
tamper-proof). You're expected to have a task called `publish` by default, that
publishes to the locally cloned github repo. That task gets wrapped into a
`publishToGithub` task that handles committing and pushing the change. Then you
can run

    gradle -Porg=layerhq -Prepo=gradle-releases publishToGithub

You can also run 

    gradle -Porg=layerhq -Prepo=gradle-releases publish

to stage a release in the local github repo and commit it manually.

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
            }
        }
    }

## Flags

The following flags are supported

* `gitRepoHome` (optional for dependencies and publishing, default is ~/.gitRepos) The location for cloned gitrepos
* `org` (required for publishing) The github org to publish to
* `repo` (required for publishing) The github repo to publish to
* `publishTask` (option for publishing, default is publish) The publish task to use

## Futures

It would be nice to make publishing seamless, without the flags, and completely
hide the locally cloned repo. That might require reimplementing maven
publishing though. The `maven-publish` plugin isn't amenable to having its
settings messed with after it's been applied unfortunately.

## Credits

Douglas Rapp

- http://github.com/drapp
- http://twitter.com/platykurtic
- douglas.rapp@gmail.com

## License

The gradle git repo plugin is available under the Apache 2 License. See the LICENSE file for more info.
