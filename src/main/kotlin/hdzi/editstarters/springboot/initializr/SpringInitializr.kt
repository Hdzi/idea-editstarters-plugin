package hdzi.editstarters.springboot.initializr

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.util.io.HttpRequests
import hdzi.editstarters.buildsystem.BuildDependency
import hdzi.editstarters.ui.ShowErrorException

/**
 * Created by taojinhou on 2018/12/21.
 */
class SpringInitializr(url: String, bootVersion: String) {
    val modulesMap = linkedMapOf<String, List<StarterInfo>>()
    val searchDB = linkedMapOf<String, StarterInfo>()
    private val idsMap = hashMapOf<String, StarterInfo>()
    private val anchorsMap = hashMapOf<String, StarterInfo>()
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

        parseSpringBootModules(baseInfoJSON)

        val dependenciesUrl = parseDependenciesUrl(baseInfoJSON, this.currentVersionID)
        try {
            val depsJSON = HttpRequests.request(dependenciesUrl).connect {
                this.gson.fromJson(it.readString(null), JsonObject::class.java)
            }
            parseDependencies(depsJSON)
        } catch (ignore: HttpRequests.HttpStatusException) {
            throw ShowErrorException("Request failure! v$currentVersionID may not be supported, please try again.")
        }
    }

    fun addExistsStarter(depend: BuildDependency) {
        val starterInfo = this.anchorsMap[depend.point]
        if (starterInfo != null) {
            starterInfo.exist = true
            this.existStarters.add(starterInfo)
        }
    }

    private fun parseSpringBootModules(json: JsonObject) {
        val dependenciesJSON = json.getAsJsonObject("dependencies").getAsJsonArray("values")
        for (moduleEle in dependenciesJSON) {
            val module = moduleEle.asJsonObject

            val values = module.getAsJsonArray("values")
            val dependencies = ArrayList<StarterInfo>(values.size())
            for (depEle in values) {
                val baseInfo = depEle.asJsonObject

                val starterInfo = this.gson.fromJson(baseInfo, StarterInfo::class.java)

                this.idsMap[starterInfo.id!!] = starterInfo
                this.searchDB[starterInfo.searchKey] = starterInfo
                dependencies.add(starterInfo)
            }
            this.modulesMap[module.get("name").asString] = dependencies
        }
    }

    private fun parseDependenciesUrl(json: JsonObject, version: String): String =
        json.getAsJsonObject("_links")
            .getAsJsonObject("dependencies")
            .get("href").asString
            .replace("{?bootVersion}", "?bootVersion=$version")

    private fun parseDependencies(json: JsonObject) {
        // 仓库信息
        val depResponse = this.gson.fromJson(json, InitializrResponse::class.java)
        depResponse.repositories?.forEach { id, repository -> repository.id = id }

        // 合并信息
        for ((id, dependency) in depResponse.dependencies!!) {
            val starterInfo = this.idsMap[id]!!

            starterInfo.groupId = dependency.groupId
            starterInfo.artifactId = dependency.artifactId
            starterInfo.version = dependency.version
            starterInfo.scope = dependency.scope

            starterInfo.addRepository(depResponse.repositories?.get(dependency.repository))
            val bom = depResponse.boms?.get(dependency.bom)
            if (bom != null) {
                starterInfo.bom = bom
                bom.repositories?.forEach { rid -> starterInfo.addRepository(depResponse.repositories?.get(rid)) }
            }

            this.anchorsMap[starterInfo.point] = starterInfo
        }
    }

    private fun String.versionNum() = this.replace("""^(\d+\.\d+\.\d+).*$""".toRegex(), "$1")
}
