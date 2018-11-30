package de.randombyte.pokebattleclauses

import com.pixelmonmod.pixelmon.battles.rules.clauses.BattleClause
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.links.PokemonLink
import de.randombyte.pokebattleclauses.config.ClausesConfig.ClauseConfig

class VariableClause(id: String, val clauseConfig: ClauseConfig) : BattleClause(id) {
    override fun getDescription() = clauseConfig.description

    override fun validateSingle(pokemon: PokemonLink): Boolean {

        val debugEnabled = PokeBattleClauses.INSTANCE.configAccessors.general.get().debug

        fun debug(message: String) {
            if (debugEnabled) PokeBattleClauses.INSTANCE.logger.info("{Clause '$id' debug ${pokemon.nickname}} $message")
        }

        val typeCheckPassed = clauseConfig.types?.let { typeConfig ->
            pokemon.type.all {
                val allowed = typeConfig.isAllowed(it)
                debug("Type '$it' allowed: $allowed")
                return@all allowed
            }
        } ?: true
        debug("--> Type check passed: $typeCheckPassed")

        val movesCheckPassed = clauseConfig.moves?.let { moveConfig ->
            pokemon.moveset.all {
                val moveAllowed = moveConfig.isAllowed(it)
                debug("Move '$it' allowed: $moveAllowed")
                return@all moveAllowed
            }
        } ?: true
        debug("--> Moves check passed: $movesCheckPassed")

        val abilityClass = pokemon.ability::class.java
        val abilityAllowed = clauseConfig.abilities?.isAllowed(abilityClass)
        if (abilityAllowed != null) debug("Ability '${abilityClass.simpleName}' allowed: $abilityAllowed")
        val abilitiesCheckPassed = abilityAllowed ?: true
        debug("--> Abilities check passed: $abilitiesCheckPassed")

        val itemsCheckPassed = clauseConfig.items?.let { itemConfig ->
            val heldItemClass = pokemon.heldItem::class.java
            val itemAllowed = itemConfig.isAllowed(heldItemClass)
            debug("Item '${heldItemClass.simpleName}' allowed: $itemAllowed")
            return@let itemAllowed
        } ?: true
        debug("--> Items check passed: $itemsCheckPassed")

        val result = typeCheckPassed && movesCheckPassed && abilitiesCheckPassed && itemsCheckPassed

        debug("==> This pokemon is allowed: $result")

        return result
    }
}