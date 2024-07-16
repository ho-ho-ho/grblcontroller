package in.co.gorest.grblcontroller.events;

import java.util.List;

import in.co.gorest.grblcontroller.model.Tool;

public class ToolsImportedEvent {
    private List<Tool> tools;

    public ToolsImportedEvent(List<Tool> tools) {
        this.tools = tools;
    }

    public List<Tool> getTools() {
        return tools;
    }
}
