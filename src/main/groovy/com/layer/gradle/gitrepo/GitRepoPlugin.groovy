package com.layer.gradle.gitrepo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.Exec

/**
 * Use a (possibly private) github repo as a maven dependency.
 * Created by drapp on 7/16/14.
 */
class GitRepoPlugin  implements Plugin<Project> {
    private static final String PUBLISH = "publish";

    void apply(Project project) {
        project.extensions.create("githubPublish", GithubPublish)

        // allow declaring special repositories
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'github', String, String, String, String, Object)) {
            project.repositories.metaClass.github = { String org, String repo, String branch = "master", String type = "releases", def closure = null ->
                String gitUrl = gitCloneUrl(org, repo)
                def orgDir = repositoryDir(project, org)
                addLocalRepo(project, ensureLocalRepo(project, orgDir, repo, gitUrl, branch), type)
            }
        }
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'git', String, String, String, String, Object)) {
            project.repositories.metaClass.git = { String gitUrl, String name, String branch = "master", String type = "releases", def closure = null ->
                def orgDir = repositoryDir(project, name)
                addLocalRepo(project, ensureLocalRepo(project, orgDir, name, gitUrl, branch), type)
            }
        }

        if(hasPublishTask(project)) {
            // add a publishToGithub task
            Task cloneRepo = project.tasks.create("cloneRepo")
            cloneRepo.doFirst{
                ensureLocalRepo(
                        project,
                        repositoryDir(project, project.githubPublish.org),
                        project.githubPublish.repo,
                        gitCloneUrl(project.githubPublish.org, project.githubPublish.repo),
                        project.githubPublish.branch)
            }
            project.tasks.getByName(PUBLISH).dependsOn(cloneRepo)

            Task publishToGithub = project.tasks.create("publishToGithub")
            publishToGithub.doFirst {
                def gitDir = repositoryDir(project, project.githubPublish.org + "/" + project.githubPublish.repo)
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
            publishToGithub.dependsOn(project.tasks.getByName(PUBLISH))
        }
    }

    static class GithubPublish {
        def String org = null
        def String repo = null
        def String branch = "master"
    }

    private static boolean hasPublishTask(project) {
        try {
            project.tasks.getByName(PUBLISH)
            return true;
        } catch (UnknownTaskException e) {
            return false;
        }

    }

    private static File repositoryDir(Project project, String name) {
        if(project.hasProperty("gitRepositories")) {
            project.file(project.property("gitRepositories"))
        } else {
            project.file("${System.env.HOME}/.gitRepos/${name}")
        }
    }

    private static String gitCloneUrl(String org, String repo) {
        return "git@github.com:$org/${repo}.git"
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
