package in.co.gorest.grblcontroller.model;

public class Tool {
    private final int number;
    private final String description;
    private final double diameter;
    private final int spindleSpeed;
    private final double cuttingFeedRate;
    private final double plungeFeedRate;

    public Tool(int number, String description, int spindleSpeed, double diameter,
            double cuttingFeedRate, double plungeFeedRate) {
        this.description = description;
        this.number = number;
        this.diameter = diameter;
        this.spindleSpeed = spindleSpeed;
        this.cuttingFeedRate = cuttingFeedRate;
        this.plungeFeedRate = plungeFeedRate;
    }

    public int getNumber() {
        return number;
    }

    public String getDescription() {
        return description;
    }

    public double getDiameter() {
        return diameter;
    }

    public int getSpindleSpeed() {
        return spindleSpeed;
    }

    public double getCuttingFeedRate() {
        return cuttingFeedRate;
    }

    public double getPlungeFeedRate() {
        return plungeFeedRate;
    }
}
