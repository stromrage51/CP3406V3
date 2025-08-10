package au.edu.jcu.myapplication.offlinemode

import au.edu.jcu.myapplication.databases.Recipe
import au.edu.jcu.myapplication.databases.RecipeEntity


//network > room
    fun Recipe.toEntity() = RecipeEntity(
        id = this.id ?: "",            // ensure non-null id
        name = this.name,
        ingredient = this.ingredient,
        steps = this.steps,
        category = this.category,
        image = this.image,
        authorId = this.authorId
    )

    //room > doomain
    fun RecipeEntity.toDomain() = Recipe(
        id = this.id,
        name = this.name,
        ingredient = this.ingredient,
        steps = this.steps,
        category = this.category,
        image = this.image,
        authorId = this.authorId
    )
