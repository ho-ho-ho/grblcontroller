package in.co.gorest.grblcontroller.util;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.library.baseAdapters.BR;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import in.co.gorest.grblcontroller.model.Tool;

public class ToolDatabase extends BaseObservable {
    private static ToolDatabase toolDatabase = null;

    private final Vector<Tool> tools = new Vector<>();

    private ToolDatabase() {
    }

    public static ToolDatabase getInstance() {
        if (toolDatabase == null) {
            toolDatabase = new ToolDatabase();
        }

        return toolDatabase;
    }

    @Bindable
    public List<Tool> getTools() {
        return tools;
    }

    public Tool getTool(int number) {
        return tools.stream()
                .filter(tool -> tool.getNumber() == number)
                .findAny()
                .orElse(null);
    }

    public void load(InputStream stream) {
        List<Tool> tools = new LinkedList<>();
        InputStreamReader isr = new InputStreamReader(stream);
        CsvReader<NamedCsvRecord> reader = CsvReader.builder().ofNamedCsvRecord(isr);
        reader.forEach(record -> {
            int number = Integer.parseInt(record.getField("Number (tool_number)"));
            String description = record.getField("Description (tool_description)");
            int spindleSpeed = Integer.parseInt(
                    record.getField("Spindle Speed (tool_spindleSpeed)"));
            double diameter = Double.parseDouble(
                    record.getField("Diameter (tool_diameter)"));
            double cuttingFeedrate = Double.parseDouble(
                    record.getField("Cutting Feedrate (tool_feedCutting)"));
            double plungeFeedrate = Double.parseDouble(
                    record.getField("Plunge Feedrate (tool_feedPlunge)"));
            // TODO: check if those values are valid
            tools.add(new Tool(number, description, spindleSpeed, diameter, cuttingFeedrate,
                    plungeFeedrate));
        });

        this.tools.clear();
        this.tools.addAll(tools);
        notifyPropertyChanged(BR.tools);
    }
}
