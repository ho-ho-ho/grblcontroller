package in.co.gorest.grblcontroller.util;

import in.co.gorest.grblcontroller.model.Position;
import in.co.gorest.grblcontroller.model.Tool;

public abstract class GcodeGenerator {
    private Direction direction;
    private Tool tool;

    // in WCS coordinates
    private Position startPosition; // only X and Y is used
    private Position endPosition;    // only X and Y is used
    private double zTop;
    private double zBottom;
    private double zClearance;
    private double stepdown;
    private double stepover;

    // returns the Z heights at which all layers are cut, from lowest to highest Z value
    protected double[] getLayerDepths() {
        double depth = zTop - zBottom;
        int layersNum = (int) Math.ceil(depth / stepdown);
        if (layersNum < 1) {
            // ensure at least 1 layer
            layersNum = 1;
        }

        double[] layerDepths = new double[layersNum];
        for (int i = 0; i < layersNum; i++) {
            layerDepths[i] = zBottom + i * stepdown;
        }

        return layerDepths;
    }

    private void moveToX(double x) {
    }

    private void moveToY(double y) {
    }

    private void moveToZ(double z) {
    }

    private void rapidToXY(double x, double y) {
        // write "G0X$xY$y" to stream
    }

    private void rapidToZ(double z) {
        // write "G0Z$z" to stream
    }

    public abstract void build();

    public enum Direction {Conventional, Climb, Both}

    public class FacingOperationGenerator extends GcodeGenerator {
        private void generateYLines(double depth, double startX, int linesNum, double stepover,
                double startY, double endY) {
            for (int i = 0; i < linesNum; i++) {
                rapidToXY(startX + i * stepover, startY);
                moveToZ(depth);
                moveToY(endY);
                rapidToZ(zClearance);
            }
        }

        private void generateLines(double depth) {
            double deltaX = endPosition.getCordX() - startPosition.getCordX();
            double deltaY = endPosition.getCordY() - startPosition.getCordY();
            double toolRadius = tool.getDiameter() / 2.0;

            rapidToZ(zClearance);

            if ((direction == Direction.Conventional && deltaX >= 0 && deltaY < 0) || (
                    direction == Direction.Climb && deltaX >= 0 && deltaY >= 0)) {
                // lines along Y axis, starting with the left edge
                double startX = startPosition.getCordX() - toolRadius + stepover;
                double endX = endPosition.getCordX() - toolRadius;
                int linesNum = (int) Math.ceil((endX - startX) / stepover);
                generateYLines(depth, startX, linesNum, stepover,
                        startPosition.getCordY() + toolRadius, endPosition.getCordY());
            } else if ((direction == Direction.Conventional && deltaX < 0 && deltaY >= 0) || (
                    direction == Direction.Climb && deltaX < 0 && deltaY < 0)) {
                // lines along Y axis, starting with the right edge
                double startX = startPosition.getCordX() + toolRadius - stepover;
                double endX = endPosition.getCordX() + toolRadius;
                int linesNum = (int) Math.ceil((endX - startX) / stepover);
                generateYLines(depth, startX, linesNum, -stepover,
                        startPosition.getCordY() - toolRadius, endPosition.getCordY());
            }
        }

        @Override
        public void build() {
            double[] layersZ = getLayerDepths();

            for (int i = layersZ.length - 1; i >= 0; i--) {
                generateLines(layersZ[i]);
            }
        }
    }
}
