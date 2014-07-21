package com.layer.gradle.gitrepo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Exec

/**
 * Use a (possibly private) github repo as a maven dependency.
 * Created by drapp on 7/16/14.
 */
class GitRepoPlugin  implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("githubPublish", GithubPublish)

        if (!project.repositories.metaClass.respondsTo(project.repositories, 'github', String, String, String, String, Object)) {
            project.repositories.metaClass.github = { String org, String repo, String branch = "master", String type = "releases", def closure = null ->
                String gitUrl = "git@github.com:$org/${repo}.git"
                def orgDir = project.file("build/repos/" + org)
                addLocalRepo(project, ensureLocalRepo(project, orgDir, repo, gitUrl, branch), type)
            }
        }
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'git', String, String, String, String, Object)) {
            project.repositories.metaClass.git = { String gitUrl, String name, String branch = "master", String type = "releases", def closure = null ->
                def orgDir = project.file("build/repos/" + name)
                addLocalRepo(project, ensureLocalRepo(project, orgDir, name, gitUrl, branch), type)
            }
        }
        Task cloneRepo = project.tasks.create("cloneRepo")
        cloneRepo.doFirst{
          def org = project.githubPublish.org
          def repo = project.githubPublish.repo
          def branch = project.githubPublish.branch
          String gitUrl = "git@github.com:$org/${repo}.git"
          ensureLocalRepo(project, project.file("build/repos/" + org), repo, gitUrl, branch)
        }
        project.tasks.getByName("publish").dependsOn(cloneRepo)

        Task publishToGithub = project.tasks.create("publishToGithub")
        publishToGithub.doFirst {
            def org = project.githubPublish.org
            def repo = project.githubPublish.repo
            def gitDir = project.file("build/repos/" + org + "/" + repo)
            project.exec {
                executable "git"
                workingDir gitDir
                args "add", "*"
            }
            project.exec {
                executable "git"
                workingDir gitDir
                args "commit", "-a", "-m", "published artifacts for  ${project.getGroup()} ${project.version}"
            }
            project.exec {
                executable "git"
                workingDir gitDir
                args "push", "-u", "origin", "master"
            }
        }
        publishToGithub.dependsOn(project.tasks.getByName("publish"))
    }

    static class GithubPublish {
        def String org = null
        def String repo = null
        def String branch = "master"
    }

    private static File ensureLocalRepo(Project project, File directory, String name, String gitUrl, String branch) {
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
        return repoDir;
    }

    private static void addLocalRepo(Project project, File repoDir, String type) {
        project.repositories.maven {
            url repoDir.getAbsolutePath() + "/" + type
        }

    }

}
