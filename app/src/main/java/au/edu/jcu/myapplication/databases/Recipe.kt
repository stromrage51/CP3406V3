package au.edu.jcu.myapplication.databases

import java.io.Serializable

data class Recipe(
    var id: String? = null,
    var name: String,
    var ingredient: String,
    var steps: String,
    var category: String,
    var image: String,
    var authorId: String,
    var authorId_name_lc: String? = null
) : Serializable {
    constructor() : this(
        id = null,
        name = "",
        ingredient = "",
        steps = "",
        category = "",
        image = "",
        authorId = ""
    )
}
