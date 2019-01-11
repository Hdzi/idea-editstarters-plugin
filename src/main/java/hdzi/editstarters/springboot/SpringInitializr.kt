package hdzi.editstarters.springboot

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.util.io.HttpRequests
import hdzi.editstarters.springboot.bean.DepResponse
import hdzi.editstarters.springboot.bean.StarterInfo
import hdzi.editstarters.springboot.bean.Version
import java.util.*

/**
 * Created by taojinhou on 2018/12/21.
 */
class SpringInitializr(url: String, versionStr: String) {
    val modulesMap = linkedMapOf<String, List<StarterInfo>>()
    private val idsMap = hashMapOf<String, StarterInfo>()
    private val anchorsMap = hashMapOf<String, StarterInfo>()
    private val gson = Gson()
    val version: Version
    val existStarters = linkedSetOf<StarterInfo>()

    init {
        // 请求initurl
        var request = HttpRequests.request(url)
        request.accept("application/json")
        val baseInfoJSON = this.gson.fromJson(request.readString(), JsonObject::class.java)

        parseSpringBootModules(baseInfoJSON)

        val dependenciesUrl = parseDependenciesUrl(baseInfoJSON, versionStr)
        request = HttpRequests.request(dependenciesUrl)
        val depsJSON = this.gson.fromJson(request.readString(), JsonObject::class.java)
        parseDependencies(depsJSON)

        this.version = this.gson.fromJson(baseInfoJSON.getAsJsonObject("bootVersion"), Version::class.java)
    }

    fun addExistsStarter(groupId: String, artifactId: String) {
        val starterInfo = this.anchorsMap[depName(groupId, artifactId)]
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
                dependencies.add(starterInfo)
            }
            this.modulesMap[module.get("name").asString] = dependencies
        }

    }

    private fun parseDependenciesUrl(json: JsonObject, version: String): String {
        return json.getAsJsonObject("_links")
            .getAsJsonObject("dependencies")
            .get("href").asString
            .replace("{?bootVersion}", "?bootVersion=$version")
    }

    private fun parseDependencies(json: JsonObject) {
        // 仓库信息
        val depResponse = this.gson.fromJson(json, DepResponse::class.java)
        depResponse.repositories!!.forEach { id, repository -> repository.id = id }

        // 合并信息
        for ((id, dependency) in depResponse.dependencies!!) {
            val starterInfo = this.idsMap[id]!!

            starterInfo.groupId = dependency.groupId
            starterInfo.artifactId = dependency.artifactId
            starterInfo.version = dependency.version
            starterInfo.scope = dependency.scope

            starterInfo.addRepository(depResponse.repositories!![dependency.repository])
            val bom = depResponse.boms!![dependency.bom]
            if (bom != null) {
                starterInfo.bom = bom
                bom.repositories!!.forEach { rid -> starterInfo.addRepository(depResponse.repositories!![rid]) }
            }

            this.anchorsMap[depName(starterInfo.groupId!!, starterInfo.artifactId!!)] = starterInfo
        }
    }

    private fun depName(groupId: String, artifactId: String): String {
        return "$groupId:$artifactId"
    }
}
