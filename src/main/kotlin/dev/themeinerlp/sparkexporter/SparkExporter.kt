package dev.themeinerlp.sparkexporter

import com.influxdb.client.*
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import dev.themeinerlp.sparkexporter.provider.BukkitPlayerPingProvider
import dev.themeinerlp.sparkexporter.provider.BukkitWorldInfoProvider
import dev.themeinerlp.sparkexporter.provider.Provider
import io.papermc.lib.PaperLib
import me.lucko.spark.api.Spark
import me.lucko.spark.api.statistic.StatisticWindow
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.time.Instant
import java.util.logging.Level


class SparkExporter : JavaPlugin() {

    private lateinit var influxDB: InfluxDBClient
    private lateinit var spark: Spark
    private lateinit var writeApi: WriteApi
    private val bukkitPlayerPingProvider: Provider = BukkitPlayerPingProvider()
    private val bukkitWorldInfoProvider: Provider = BukkitWorldInfoProvider()

    override fun onEnable() {
        saveDefaultConfig()
        PaperLib.suggestPaper(this, Level.SEVERE)
        if (!Bukkit.getPluginManager().isPluginEnabled("spark")) {
            componentLogger.warn(
                MiniMessage.miniMessage().deserialize("<yellow>Disable SparkExporter. Spark not detected")
            )
            return
        }
        influxDB = createInfluxDBConnection()
        writeApi = influxDB.makeWriteApi(WriteOptions.builder().batchSize(5000).flushInterval(1000)
            .bufferLimit(10000)
            .jitterInterval(1000)
            .retryInterval(5000)
            .build())
        try {
            val provider = Bukkit.getServicesManager().getRegistration(Spark::class.java)
            if (provider != null) {
                spark = provider.provider
                Bukkit.getScheduler().runTaskTimer(this, this::collect10Seconds, 0, 10 * 20)
            }

        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }


    private fun collect10Seconds() {
        val cpu = this.spark.cpuProcess().poll(StatisticWindow.CpuUsage.SECONDS_10)
        val tps = this.spark.tps()?.poll(StatisticWindow.TicksPerSecond.SECONDS_10)
        val mspt = this.spark.mspt()?.poll(StatisticWindow.MillisPerTick.SECONDS_10)
        val cpuSystem = this.spark.cpuSystem().poll(StatisticWindow.CpuUsage.SECONDS_10)
        val sparkPoint = Point
            .measurement("spark")
            .time(Instant.now().toEpochMilli(), WritePrecision.MS)
            .addField("cpu", cpu)
            .addField("cpuSystem", cpuSystem)
            .addField("msptMedian", mspt?.median() ?: -1.0)
            .addField("msptMax", mspt?.max() ?: -1.0)
            .addField("msptMin", mspt?.min() ?: -1.0)
            .addField("msptPercentile95th", mspt?.percentile95th() ?: -1.0)
            .addField("tps", tps ?: -1.0)

        writeApi.writePoint(sparkPoint)
        var entities = 0
        var tileEntities = 0
        var chunks = 0
        Bukkit.getWorlds().forEach {
            entities += it.entityCount
            tileEntities += it.tileEntityCount
            chunks += it.chunkCount
        }
        val bukkitPoint = Point.measurement("bukkit")
            .time(Instant.now().toEpochMilli(), WritePrecision.MS)
            .addTag("serverImplementation", Bukkit.getName())
            .addTag("serverVersion", Bukkit.getVersion())
            .addTag("bukkitVersion", Bukkit.getBukkitVersion())
            .addTag("minecraftVersion", Bukkit.getMinecraftVersion())
            .addField("onlinePlayers", Bukkit.getOnlinePlayers().size)
            .addField("maxPlayers", Bukkit.getMaxPlayers())
            .addField("serverImplementation", Bukkit.getName())
            .addField("serverVersion", Bukkit.getVersion())
            .addField("bukkitVersion", Bukkit.getBukkitVersion())
            .addField("minecraftVersion", Bukkit.getMinecraftVersion())
            .addField("entities", entities)
            .addField("tileEntities", tileEntities)
            .addField("chunks", chunks)
        writeApi.writePoint(bukkitPoint)
        bukkitPlayerPingProvider.writeData(writeApi)
        bukkitWorldInfoProvider.writeData(writeApi)
    }

    private fun createInfluxDBConnection(): InfluxDBClient {
        val url = config.getString("influx.v2.url")
        val token = config.getString("influx.v2.token")
        val org = config.getString("influx.v2.org")
        val bucket = config.getString("influx.v2.bucket")
        return InfluxDBClientFactory.create(url ?: "", token?.toCharArray() ?: charArrayOf(), org, bucket)
    }

}