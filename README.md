# git-repo-plugin

This plugin allows you to add a git repository as a maven repo, even if the git repository is private. 

## Building

Run `gradle publishToMavenLocal` or `gradle publish` to build and push a new version. The project can also be opened in intellij

## Usage

repositories {
    github("layerhq", "maven-private", "master", "releases")
}

## Futures

Accept closures for nicer syntax
