package `in`.co.gorest.grblcontroller.util

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import de.siegmar.fastcsv.reader.CsvReader
import `in`.co.gorest.grblcontroller.BR
import `in`.co.gorest.grblcontroller.model.Tool
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

// we're using a List instead of a Set because a gcode file can use the same tool multiple times
// (i.e. T12 -> T25 -> T32 -> T12 )
typealias ToolList = ArrayList<Tool>

object ToolLibrary : BaseObservable() {
    val tools: ToolList = ToolList()
        @Bindable
        get

    fun getTool(number: Int): Tool? = tools.find { tool -> tool.number == number }

    fun getToolNumbers(): List<Int> = tools.map { tool -> tool.number }

    fun loadFromFusionCsv(stream: InputStream) {
        val tools = ToolList()
        val isr = InputStreamReader(stream)
        val reader = CsvReader.builder().ofNamedCsvRecord(isr)
        for (record in reader) {
            val number = record.getField("Number (tool_number)").toInt()
            val description = record.getField("Description (tool_description)")
            val spindleSpeed = record.getField("Spindle Speed (tool_spindleSpeed)").toInt()
            val diameter = record.getField("Diameter (tool_diameter)").toDouble()
            val cuttingFeedrate = record.getField("Cutting Feedrate (tool_feedCutting)").toDouble()
            val plungeFeedrate = record.getField("Plunge Feedrate (tool_feedPlunge)").toDouble()
            tools += Tool(
                number,
                description,
                diameter,
                spindleSpeed,
                cuttingFeedrate,
                plungeFeedrate
            )
        }

        this.tools.clear()
        this.tools += tools
        this.tools.sortBy { t -> t.number }
        notifyPropertyChanged(BR.tools)
    }

    fun load(stream: InputStream) {
        try {
            this.tools.clear()
            this.tools += Json.Default.decodeFromStream(
                ListSerializer(Tool.serializer()),
                stream
            ) as ToolList
            this.tools.sortBy { t -> t.number }
            notifyPropertyChanged(BR.tools)
        } catch (ignored: IllegalArgumentException) {
        }
    }

    fun save(stream: OutputStream) {
        val writer = OutputStreamWriter(stream)
        writer.write(Json.Default.encodeToString(ListSerializer(Tool.serializer()), tools))
        writer.flush()
    }
}