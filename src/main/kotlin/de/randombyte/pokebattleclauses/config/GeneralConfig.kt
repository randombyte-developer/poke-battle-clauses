package de.randombyte.pokebattleclauses.config

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable class GeneralConfig(
        @Setting("debug", comment = "Activate to get more info about how your clauses' black/whitelists work") val debug: Boolean = false,
        @Setting("disable-metrics-messages", comment = "If you really don't want to enable metrics " +
                "and don't want to receive any messages anymore, you can disable this. " +
                "But I would be very glad if metrics would be enabled in the Sponge global config!") val disableMetricsMessages: Boolean = false
)