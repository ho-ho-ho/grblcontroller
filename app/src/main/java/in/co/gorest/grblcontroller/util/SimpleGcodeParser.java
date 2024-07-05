package in.co.gorest.grblcontroller.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.co.gorest.grblcontroller.model.Bounds;

// very rudimentary gcode parser, for now only keeps track of motion and distance modals.
// used to find the min/max values of each axis of a given gcode file
public class SimpleGcodeParser {
    private int motionMode = -1; // G0, G1, G2, G3, G38.2, G38.3, G38.4, G38.5, G80
    private int distanceMode = -1; // G90, G91
    private Pattern wordPattern = Pattern.compile("([A-Z])(-?[0-9.]+)");

    private Bounds bounds;

    public SimpleGcodeParser() {
        bounds = new Bounds();
    }

    public Bounds getBounds() {
        return bounds;
    }

    public void reset() {
        motionMode = 0;
        distanceMode = 0;
    }

    public void parseLine(String line) {
        line = line.replaceAll("\\s", "");
        line = line.replaceAll("\\(.*\\)", "");

        Integer g = null;
        Double x = null;
        Double y = null;
        Double z = null;

        Matcher m = wordPattern.matcher(line);
        while (m.find()) {
            String word = m.group(1);
            String address = m.group(2);

            if (word == null || address == null) {
                continue;
            }

            if (word.equals("X")) {
                x = Double.parseDouble(address);
            } else if (word.equals("Y")) {
                y = Double.parseDouble(address);
            } else if (word.equals("Z")) {
                z = Double.parseDouble(address);
            } else if (word.equals("G")) {
                if (address.equals("53")) {
                    // move in MCS, doesn't bother us here
                    return;
                }

                int gVal = Integer.parseInt(address);
                if (gVal == 90 || gVal == 91) {
                    distanceMode = gVal;
                } else if (gVal >= 17 && gVal <= 19) {
                    // ignore plane selection
                } else if (gVal == 93 || gVal == 94) {
                    // ignore feed mode
                } else if (gVal == 20 || gVal == 21) {
                    // ignore units mode for now
                } else if (gVal >= 40 && gVal <= 42) {
                    // ignore cutter radius compensation
                } else if (gVal == 43 || gVal == 49) {
                    // ignore tool length compensation
                } else if (gVal == 98 || gVal == 99) {
                    // ignore canned return
                } else if (gVal >= 54 && gVal <= 59) {
                    // ignore coordinate system for now
                } else {
                    g = gVal;
                }
            }
        }

        if (g == null) {
            calculateMinMax(x, y, z);
        } else if ((g >= 0) && (g <= 4)) {
            motionMode = g;
            calculateMinMax(x, y, z);
        }
    }

    private void calculateMinMax(Double x, Double y, Double z) {
        bounds.include(x, y, z);
    }
}
