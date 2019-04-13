package de.randombyte.pokebattleclauses

import com.pixelmonmod.pixelmon.api.pokemon.Pokemon
import com.pixelmonmod.pixelmon.battles.attacks.Attack
import com.pixelmonmod.pixelmon.battles.rules.clauses.BattleClause
import com.pixelmonmod.pixelmon.enums.EnumSpecies
import com.pixelmonmod.pixelmon.enums.EnumType
import de.randombyte.pokebattleclauses.config.ClausesConfig.ClauseConfig
import de.randombyte.pokebattleclauses.config.ListType.BLACK
import de.randombyte.pokebattleclauses.config.ListType.WHITE

class VariableClause(id: String, val clauseConfig: ClauseConfig) : BattleClause(id) {

    init {
        description = clauseConfig.description
    }

    override fun validateSingle(pokemon: Pokemon): Boolean {

        val debugEnabled = PokeBattleClauses.INSTANCE.configAccessors.general.get().debug

        fun debug(message: String) {
            if (debugEnabled) PokeBattleClauses.INSTANCE.logger.info("{Clause '$id' debug ${pokemon.displayName}} $message")
        }

        val typeCheckPassed = clauseConfig.types?.let { typeConfig ->
            val typeCheck: (EnumType) -> Boolean = typeCheck@ { type ->
                val allowed = typeConfig.isAllowed(type)
                debug("Type '$type' allowed: $allowed")
                return@typeCheck allowed
            }

            when (typeConfig.listType) {
                WHITE -> pokemon.species.baseStats.types.any(typeCheck)
                BLACK -> pokemon.species.baseStats.types.all(typeCheck)
            }
        } ?: true
        debug("--> Type check passed: $typeCheckPassed")

        val movesCheckPassed = clauseConfig.moves?.let { moveConfig ->
            val moveCheck: (Attack) -> Boolean = moveCheck@ { move ->
                val allowed = moveConfig.isAllowed(move)
                debug("Move '$move' allowed: $allowed")
                return@moveCheck allowed
            }

            when (moveConfig.listType) {
                WHITE -> pokemon.moveset.any(moveCheck)
                BLACK -> pokemon.moveset.all(moveCheck)
            }
        } ?: true
        debug("--> Moves check passed: $movesCheckPassed")

        val abilitiesCheckPassed = clauseConfig.abilities?.let { abilityConfig ->
            val abilityClass = pokemon.ability::class.java
            val abilityAllowed = abilityConfig.isAllowed(abilityClass)
            debug("Ability '${abilityClass.simpleName}' allowed: $abilityAllowed")
            return@let abilityAllowed
        } ?: true
        debug("--> Abilities check passed: $abilitiesCheckPassed")

        val itemsCheckPassed = clauseConfig.items?.let { itemConfig ->
            val heldItemClass = pokemon.heldItemAsItemHeld::class.java
            val itemAllowed = itemConfig.isAllowed(heldItemClass)
            debug("Item '${heldItemClass.simpleName}' allowed: $itemAllowed")
            return@let itemAllowed
        } ?: true
        debug("--> Items check passed: $itemsCheckPassed")

        val levelsCheckPassed = clauseConfig.levels?.let { levelConfig ->
            var levelAllowed = false
            if (levelConfig.ensureInitialization()) {
                val levelInRange = levelConfig.listValues!!.any { range -> pokemon.level in range }
                levelAllowed = when (levelConfig.listType) {
                    WHITE -> levelInRange
                    BLACK -> !levelInRange
                }
            }
            debug("Level '${pokemon.level}' allowed: $levelAllowed")
            return@let levelAllowed
        } ?: true
        debug("--> Levels check passed: $levelsCheckPassed")

        val legendaryCheckPassed = clauseConfig.legendary?.let { legendary ->
            legendary == pokemon.species in EnumSpecies.LEGENDARY_ENUMS
        } ?: true
        debug("--> Legendary check passed: $legendaryCheckPassed")

        val result = typeCheckPassed && movesCheckPassed && abilitiesCheckPassed && itemsCheckPassed && levelsCheckPassed && legendaryCheckPassed

        debug("==> This pokemon is allowed: $result")

        return result
    }
}