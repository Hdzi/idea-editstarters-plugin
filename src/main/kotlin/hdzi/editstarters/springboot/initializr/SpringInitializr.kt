package hdzi.editstarters.springboot.initializr

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.util.io.HttpRequests
import hdzi.editstarters.buildsystem.ProjectDependency
import hdzi.editstarters.ui.ShowErrorException

/**
 * Created by taojinhou on 2018/12/21.
 */
class SpringInitializr(url: String, bootVersion: String) {
    val modulesMap = linkedMapOf<String, List<StarterInfo>>()
    private val pointMap = hashMapOf<String, StarterInfo>()
    private val gson = Gson()
    var version: InitializrVersion
    val existStarters = linkedSetOf<StarterInfo>()
    val currentVersionID: String

    init {
        // 请求initurl
        val baseInfoJSON = HttpRequests.request(url).accept("application/json").connect {
            this.gson.fromJson(it.readString(null), JsonObject::class.java)
        }
        this.version = this.gson.fromJson(baseInfoJSON.getAsJsonObject("bootVersion"), InitializrVersion::class.java)
        this.currentVersionID = bootVersion.versionNum()

        val dependenciesUrl = parseDependenciesUrl(baseInfoJSON, this.currentVersionID)
        try {
            val depsJSON = HttpRequests.request(dependenciesUrl).connect {
                this.gson.fromJson(it.readString(null), JsonObject::class.java)
            }
            parseDependencies(baseInfoJSON, depsJSON)
        } catch (ignore: HttpRequests.HttpStatusException) {
            throw ShowErrorException("Request failure! v$currentVersionID may not be supported, please try again.")
        }
    }

    fun addExistsStarter(depend: ProjectDependency) {
        val starterInfo = this.pointMap[depend.point]
        if (starterInfo != null) {
            starterInfo.exist = true
            this.existStarters.add(starterInfo)
        }
    }

    private fun parseDependenciesUrl(json: JsonObject, version: String): String =
        json.getAsJsonObject("_links")
            .getAsJsonObject("dependencies")
            .get("href").asString
            .replace("{?bootVersion}", "?bootVersion=$version")

    private fun parseDependencies(baseInfoJSON: JsonObject, depJSON: JsonObject) {
        // 设置仓库信息的id
        val depResponse = this.gson.fromJson(depJSON, InitializrResponse::class.java)
        depResponse.repositories.forEach { (id, repository) -> repository.id = id }

        val modulesJSON = baseInfoJSON.getAsJsonObject("dependencies").getAsJsonArray("values")
        for (moduleEle in modulesJSON) {
            val module = moduleEle.asJsonObject

            val dependenciesJSON = module.getAsJsonArray("values")
            val dependencies = ArrayList<StarterInfo>(dependenciesJSON.size())
            for (depEle in dependenciesJSON) {
                val starterInfo = this.gson.fromJson(depEle.asJsonObject, StarterInfo::class.java)
                val dependency = depResponse.dependencies[starterInfo.id] ?: continue
                starterInfo.groupId = dependency.groupId
                starterInfo.artifactId = dependency.artifactId
                starterInfo.version = dependency.version
                starterInfo.scope = dependency.scope
                val bom = depResponse.boms[dependency.bom]
                if (bom != null) {
                    starterInfo.bom = bom
                    bom.repositories.forEach { rid -> starterInfo.addRepository(depResponse.repositories[rid]) }
                }

                this.pointMap[starterInfo.point] = starterInfo

                dependencies.add(starterInfo)
            }

            this.modulesMap[module.get("name").asString] = dependencies
        }
    }

    private fun String.versionNum() = this.replace("""^(\d+\.\d+\.\d+).*$""".toRegex(), "$1")
}
