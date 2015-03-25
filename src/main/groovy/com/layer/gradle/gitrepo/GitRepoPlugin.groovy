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

    void apply(Project project) {

		project.extensions.create("repoconfig",GitRepoPluginExtension)
		project.extensions.create("taskconfig",PublishTaskExtension)

        // allow declaring special repositories
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'github', String, String, String, String, Object)) {
            project.repositories.metaClass.github = { String org, String repo, String branch = "master", String type = "releases", def closure = null ->
                String gitUrl = gitCloneUrl(project)
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

        project.afterEvaluate {
            if(hasPublishTask(project)) {
                // add a publishToGithub task
                Task cloneRepo = project.tasks.create("cloneRepo")
                cloneRepo.doFirst{
                    ensureLocalRepo(
                            project,
                            repositoryDir(project, project.repoconfig.org),
                            project.repoconfig.repo,
                            gitCloneUrl(project),
                            project.repoconfig.branch)
                }
                publishTask(project).dependsOn(cloneRepo)

                Task publishToGithub = project.tasks.create(project.repoconfig.taskname)
                publishToGithub.doFirst {
                    def gitDir = repositoryDir(project, project.repoconfig.org + "/" + project.repoconfig.repo)
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
                publishToGithub.dependsOn(publishTask(project))
            }
        }
    }

    private static boolean hasPublishTask(project) {
        try {
            publishTask(project)
            return true;
        } catch (UnknownTaskException e) {
            return false;
        }

    }

    private static Task publishTask(Project project) {
		project.tasks.getByName(project.taskconfig.publishTask)
    }

    private static File repositoryDir(Project project, String name) {
        return project.file("${project.repoconfig.gitrepohome}/$name")
    }

    private static String gitCloneUrl(Project project) {
		def String url = ""
		if(project.repoconfig.giturl != ""){
			url = project.repoconfig.giturl
		} else {
			url = "git@${project.repoconfig.provider}:${project.repoconfig.org}/${project.repoconfig.repo}.git"
		}
        return url
    }

    private static File ensureLocalRepo(Project project, File directory, String name, String gitUrl, String branch) {
        def repoDir = new File(directory, name)

		println "Ensure local repo : dir ${directory.getAbsolutePath()}, name : ${name}, url ${gitUrl}, branch ${branch}"

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

class GitRepoPluginExtension{
	def String org = ""
	def String repo = ""
	def String provider = "github.com" //github.com, gitlab or others
	def String giturl = "" //used to replace git@${provider}:${org}/${repo}.git
	def String branch = "master"
	def String gitrepohome = "${System.properties['user.home']}/.gitRepos"
}

class PublishTaskExtension{
	def String newpublishtaskname = "publishToGithub"
	def String publishTask = "publish" //default publish tasks added by maven-publish plugin
}
