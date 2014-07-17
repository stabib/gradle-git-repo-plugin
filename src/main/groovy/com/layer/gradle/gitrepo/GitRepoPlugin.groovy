package com.layer.gradle.gitrepo

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Use a (possibly private) github repo as a maven dependency.
 * Created by drapp on 7/16/14.
 */
class GitRepoPlugin  implements Plugin<Project> {
    void apply(Project project) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'github', String, String, String, String, Object)) {
            project.repositories.metaClass.github = { String org, String repo, String branch = "master", String type = "releases", def closure = null ->
                String gitUrl = "git@github.com:$org/${repo}.git"
                def orgDir = project.file("build/repos/" + org)
                addLocalRepo(project, orgDir, repo, gitUrl, branch, type)
            }
        }
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'git', String, String, String, String, Object)) {
            project.repositories.metaClass.git = { String gitUrl, String name, String branch = "master", String type = "releases", def closure = null ->
                def orgDir = project.file("build/repos/" + name)
                addLocalRepo(project, orgDir, name, gitUrl, branch, type)
            }
        }
    }

    private static void addLocalRepo(Project project, File directory, String name, String gitUrl, String branch, String type) {
        def repoDir = new File(directory, name)
        if(!repoDir.directory) {
            project.mkdir(directory)
            project.exec {
                workingDir directory
                executable "git"
                args "clone", gitUrl, name
            }
        }
        project.exec {
            workingDir repoDir
            executable "git"
            args "checkout", branch
        }
        project.exec {
            workingDir repoDir
            executable "git"
            args "pull"
        }
        project.repositories.maven {
            url repoDir.getAbsolutePath() + "/" + type
        }

    }

}
