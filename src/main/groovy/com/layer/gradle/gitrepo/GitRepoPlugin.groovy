package com.layer.gradle.gitrepo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException

import org.ajoberstar.grgit.*

/**
 * Use a (possibly private) github repo as a maven dependency.
 * Created by drapp on 7/16/14.
 */
class GitRepoPlugin  implements Plugin<Project> {
    void apply(Project project) {

		project.extensions.create("gitRepoConfig", GitRepoConfig)

        // allow declaring special repositories
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'github', String, String, String, String, Object)) {
            project.repositories.metaClass.github = { String org, String repo, String branch = "master", String type = "releases", def closure = null ->
                String gitUrl = githubCloneUrl(org, repo)
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
                            repositoryDir(project, project.gitRepoConfig.org),
                            project.gitRepoConfig.repo,
                            gitCloneUrl(project),
                            project.gitRepoConfig.branch)
                }
                publishTask(project).dependsOn(cloneRepo)

                Task publishAndPush = project.tasks.create(project.gitRepoConfig.publishAndPushTask)
                publishAndPush.doFirst {
                    def gitDir = repositoryDir(project, project.gitRepoConfig.org + "/" + project.gitRepoConfig.repo)
                    def gitRepo= Grgit.open(dir: gitDir)

                    gitRepo.add(patterns: ['*'])
                    gitRepo.commit(message: "published artifacts for  ${project.getGroup()} ${project.version}")
                    gitRepo.push()
                }
                publishAndPush.dependsOn(publishTask(project))
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
		project.tasks.getByName(project.gitRepoConfig.publishTask)
    }

    private static File repositoryDir(Project project, String name) {
        return project.file("${project.gitRepoConfig.gitRepoHome}/$name")
    }
    private static String githubCloneUrl(String org, String repo) {
        return "git@github.com:$org/${repo}.git"
    }

    private static String gitCloneUrl(Project project) {
		def String url = ""
		if(project.gitRepoConfig.gitUrl != ""){
			url = project.gitRepoConfig.gitUrl
		} else {
			url = "git@${project.gitRepoConfig.provider}:${project.gitRepoConfig.org}/${project.gitRepoConfig.repo}.git"
		}
        return url
    }

    private static File ensureLocalRepo(Project project, File directory, String name, String gitUrl, String branch) {
        def repoDir = new File(directory, name)
        def gitRepo;
        println("repoDir: " + repoDir)

        if(repoDir.directory) {
            gitRepo= Grgit.open(dir: repoDir)
        } else {
            gitRepo= Grgit.clone(dir: repoDir, uri: gitUrl)
        }
        gitRepo.checkout(branch: branch)
        gitRepo.pull()

        return repoDir;
    }

    private static void addLocalRepo(Project project, File repoDir, String type) {
        project.repositories.maven {
            url repoDir.getAbsolutePath() + "/" + type
        }

    }

}

class GitRepoConfig {
	def String org = ""
	def String repo = ""
	def String provider = "github.com" //github.com, gitlab or others
	def String gitUrl = "" //used to replace git@${provider}:${org}/${repo}.git
	def String branch = "master"
	def String gitRepoHome = "${System.properties['user.home']}/.gitRepos"
    def String publishAndPushTask = "publishToGithub"
    def String publishTask = "publish" //default publish tasks added by maven-publish plugin
}
