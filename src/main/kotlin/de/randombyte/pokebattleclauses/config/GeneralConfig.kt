package de.randombyte.pokebattleclauses.config

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable class GeneralConfig(
        @Setting("debug", comment = "Activate to get more info about how your clauses' black/whitelists work") val debug: Boolean = false
)