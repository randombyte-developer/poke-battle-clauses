package de.randombyte.pokebattleclauses.config

import com.pixelmonmod.pixelmon.battles.attacks.Attack
import com.pixelmonmod.pixelmon.battles.attacks.AttackBase
import com.pixelmonmod.pixelmon.entities.pixelmon.abilities.AbilityBase
import com.pixelmonmod.pixelmon.enums.EnumType
import com.pixelmonmod.pixelmon.items.ItemHeld
import com.pixelmonmod.pixelmon.items.heldItems.NoItem
import de.randombyte.kosp.extensions.orNull
import de.randombyte.pokebattleclauses.PokeBattleClauses
import de.randombyte.pokebattleclauses.config.ListType.BLACK
import de.randombyte.pokebattleclauses.config.ListType.WHITE
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable
import org.spongepowered.api.Sponge
import org.spongepowered.api.item.ItemType
import org.spongepowered.api.item.inventory.ItemStack

@ConfigSerializable class ClausesConfig(
        @Setting("clauses") val clauses: Map<String, ClauseConfig> = emptyMap()
) {

    // default config
    constructor() : this(
            clauses = mapOf(
                    "an_example" to ClauseConfig(
                            description = "An example clause",
                            types = BlackWhiteList(
                                    listType = WHITE,
                                    list = listOf("electric", "ground")
                            ),
                            moves = BlackWhiteList(
                                    listType = BLACK,
                                    list = listOf("Tackle")
                            ),
                            abilities = BlackWhiteList(
                                    listType = WHITE,
                                    list = listOf("Overgrow")
                            ),
                            items = BlackWhiteList(
                                    listType = BLACK,
                                    list = listOf("pixelmon:smoke_ball")
                            )
                    )
            )
    )

    fun parseValues() {
        clauses.values.forEach { it.parseValues() }
    }

    @ConfigSerializable class ClauseConfig(
            @Setting("description") val description: String = "",
            @Setting("types") val types: BlackWhiteList<EnumType>? = null,
            @Setting("moves") val moves: BlackWhiteList<Attack>? = null,
            @Setting("abilities") val abilities: BlackWhiteList<Class<out AbilityBase>>? = null,
            @Setting("items") val items: BlackWhiteList<Class<out ItemHeld>>? = null
    ) {

        /**
         * Must be called after loading the config
         *
         * @return true if the parsing was successful, false if not
         */
        fun parseValues(): Boolean {
            val logger = PokeBattleClauses.INSTANCE.logger

            types?.parseTypeValues { typeName ->
                val type = EnumType.values().singleOrNull { it.name.equals(typeName, ignoreCase = true) }
                if (type == null) {
                    logger.error("Could not find Pokemon type '$typeName'")
                    return false
                }

                return@parseTypeValues type
            }

            moves?.parseTypeValues { moveName ->
                val attackBase = AttackBase.getAttackBase(moveName).orNull()
                if (attackBase == null) {
                    logger.error("Could not find move '$moveName'!")
                    return false
                }

                return@parseTypeValues Attack(attackBase)
            }

            abilities?.parseTypeValues { abilityName ->
                val ability = AbilityBase.getAbility(abilityName).orNull()
                if (ability == null) {
                    logger.error("Could not find ability '$abilityName'!")
                    return false
                }

                return@parseTypeValues ability::class.java
            }

            items?.parseTypeValues { itemName ->
                val itemType = Sponge.getRegistry().getType(ItemType::class.java, itemName).orNull()
                if (itemType == null) {
                    logger.error("Could not find item type '$itemName'!")
                    return false
                }

                val itemStack = ItemStack.of(itemType, 1)
                val pixelmonItemHeld = ItemHeld.getItemHeld(itemStack as net.minecraft.item.ItemStack)
                if (pixelmonItemHeld == NoItem.noItem) {
                    logger.error("'$itemName' is not a pixelmon held item!")
                    return false
                }

                return@parseTypeValues pixelmonItemHeld::class.java
            }

            return true
        }
    }
}

enum class ListType { WHITE, BLACK }

@ConfigSerializable class BlackWhiteList<T>(
        @Setting("list-type") val listType: ListType = WHITE,
        @Setting("list") val list: List<String> = emptyList()
) {

    var listValues: List<T>? = null

    // Must be called after loading the config
    inline fun parseTypeValues(parser: (String) -> T) {
        listValues = list.map(parser)
    }

    fun isAllowed(obj: T): Boolean {
        if (listValues == null) {
            PokeBattleClauses.INSTANCE.logger.error("The clause values were not initialized, check for previous errors!")
            return false
        }
        return when (listType) {
            WHITE -> obj in listValues!!
            BLACK -> obj !in listValues!!
        }
    }
}