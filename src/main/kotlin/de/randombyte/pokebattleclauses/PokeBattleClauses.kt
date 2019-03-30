package de.randombyte.pokebattleclauses

import com.google.inject.Inject
import com.pixelmonmod.pixelmon.battles.rules.clauses.BattleClause
import com.pixelmonmod.pixelmon.battles.rules.clauses.BattleClauseRegistry
import de.randombyte.kosp.extensions.*
import de.randombyte.pokebattleclauses.config.ConfigAccessor
import org.bstats.sponge.Metrics2
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.GameReloadEvent
import org.spongepowered.api.event.game.state.GamePostInitializationEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.Dependency
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.scheduler.Task
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit

@Plugin(id = PokeBattleClauses.ID,
        name = PokeBattleClauses.NAME,
        version = PokeBattleClauses.VERSION,
        dependencies = [Dependency(id = "pixelmon")],
        authors = [(PokeBattleClauses.AUTHOR)])
class PokeBattleClauses @Inject constructor(
        val logger: Logger,
        @ConfigDir(sharedRoot = false) configPath: Path,
        private val pluginContainer: PluginContainer,
        private val bStats: Metrics2
) {
    internal companion object {
        const val ID = "poke-battle-clauses"
        const val NAME = "PokeBattleClauses"
        const val VERSION = "1.2.0"
        const val AUTHOR = "RandomByte"

        private val _INSTANCE = lazy { Sponge.getPluginManager().getPlugin(ID).get().instance.get() as PokeBattleClauses }
        val INSTANCE: PokeBattleClauses
            get() = _INSTANCE.value
    }

    val configAccessors = ConfigAccessor(configPath)

    val registry: BattleClauseRegistry<BattleClause>
        get() = BattleClauseRegistry.getClauseRegistry()

    val metricsNoteSent = mutableSetOf<UUID>()

    @Listener
    fun onInit(event: GamePostInitializationEvent) {
        configAccessors.reloadAll()
        registerClauses()

        logger.info("$NAME loaded: $VERSION")
    }

    @Listener
    fun onReload(event: GameReloadEvent) {
        unregisterOurClauses()
        configAccessors.reloadAll()
        registerClauses()

        logger.info("Reloaded!")
    }

    @Listener
    fun onPlayerJoin(event: ClientConnectionEvent.Join) {
        val uuid = event.targetEntity.uniqueId
        if (
                !configAccessors.general.get().disableMetricsMessages &&
                uuid !in metricsNoteSent &&
                event.targetEntity.hasPermission("nucleus.mute.base") &&
                !Sponge.getMetricsConfigManager().areMetricsEnabled(this)
        ) {
            Task.builder()
                    .delay(10, TimeUnit.SECONDS)
                    .execute { ->
                        val player = uuid.getPlayer() ?: return@execute
                        metricsNoteSent += uuid
                        player.sendMessage("[Server Warning][PokeBattleClauses] ".yellow() +
                                "Metrics are disabled for this plugin or globally! Please consider enabling metrics.".aqua())
                        player.sendMessage("Metrics are anonymous usage data (how many players are on the server, which minecraft version the server is on, etc.".green())
                        player.sendMessage("With that data I can see how much the plugin is used. Plugins with many users motivate me more to release new updates. :)".gold())
                        player.sendMessage("To disable this message, go to the Sponge global config, and enable metrics collection for at least this plugin, thanks! ;)".aqua())
                    }
                    .submit(this)
        }
    }

    private fun unregisterOurClauses() {
        val ourClauseIds = configAccessors.clauses.get().clauses.keys
        val notOurClauses = registry.customClauses.filterNot { it.id in ourClauseIds }
        registry.replaceCustomClauses(notOurClauses)
    }

    private fun registerClauses() {
        val clausesConfig = configAccessors.clauses.get().clauses

        val alreadyUsedIds = clausesConfig.keys.filter { id -> registry.hasClause(id) }
        if (alreadyUsedIds.isNotEmpty()) {
            logger.error("The following clause ID(s) are already in use: ${alreadyUsedIds.joinToString()}")
            return
        }

        clausesConfig.forEach { (id, clauseConfig) ->
            registry.registerCustomClause(VariableClause(id, clauseConfig))
        }
    }
}