package hdzi.editstarters.bean.initializr

class InitializrResponse(
    var repositories: Map<String, InitializrRepository>?,
    var boms: Map<String, InitializrBom>?,
    var dependencies: Map<String, InitializrDependency>?
)