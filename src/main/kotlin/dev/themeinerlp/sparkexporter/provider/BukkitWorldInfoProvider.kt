package dev.themeinerlp.sparkexporter.provider

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import org.bukkit.Bukkit
import java.time.Instant

class BukkitWorldInfoProvider : Provider {

    override fun writeData(writeApiBlocking: WriteApi) {

        val morePoints = Bukkit.getWorlds().map { world ->
            val loadedChunks = world.loadedChunks
            val point = Point.measurement("world")
                .time(Instant.now().toEpochMilli(), WritePrecision.MS)
                .addTag("world", world.name)
                .addField("entities", world.entityCount)
                .addField("tileEntities", world.tileEntityCount)
                .addField("chunks", world.chunkCount)
            val points = loadedChunks.map chunks@{ chunk ->
                val chunkPoint = Point.measurement("chunk")
                    .time(Instant.now().toEpochMilli(), WritePrecision.MS)
                    .addTag("world", world.name)
                    .addTag("x", chunk.x.toString())
                    .addTag("y", chunk.z.toString())
                chunk.entities.groupBy { it.type }.forEach { (entity, count) ->
                    chunkPoint.addTag("entity", entity.name)
                    chunkPoint.addField("value", count.count())
                }
                return@chunks chunkPoint
            }
            return@map points + arrayOf(point)
        }
        val finalPoints = morePoints.reduce { acc, anies -> acc + anies }.filter { it.hasFields() }

        writeApiBlocking.writePoints(finalPoints.toMutableList())

    }
}