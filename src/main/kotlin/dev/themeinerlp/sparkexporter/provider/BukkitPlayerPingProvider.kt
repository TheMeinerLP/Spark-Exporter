package dev.themeinerlp.sparkexporter.provider

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import org.bukkit.Bukkit
import java.time.Instant

class BukkitPlayerPingProvider : Provider {
    override fun writeData(writeApiBlocking: WriteApi) {
        val points = Bukkit.getOnlinePlayers().map {
            val point = Point.measurement("playerPing")
                .time(Instant.now().toEpochMilli(), WritePrecision.MS)
            point.addTag("name", it.name)
            point.addField("ping", it.ping)
            point
        }.filter { it.hasFields() }
        writeApiBlocking.writePoints(points)
    }
}