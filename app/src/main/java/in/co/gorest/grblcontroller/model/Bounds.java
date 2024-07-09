package in.co.gorest.grblcontroller.model;

public class Bounds {

    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private double zMin;
    private double zMax;

    public Bounds() {
        xMin = Double.MAX_VALUE;
        xMax = -Double.MAX_VALUE;
        yMin = Double.MAX_VALUE;
        yMax = -Double.MAX_VALUE;
        zMin = Double.MAX_VALUE;
        zMax = -Double.MAX_VALUE;
    }

    public void includeX(double x) {
        if (x == Double.MAX_VALUE || x == -Double.MAX_VALUE) {
            return;
        }

        if (x < xMin) {
            xMin = x;
        }
        if (x > xMax) {
            xMax = x;
        }
    }

    public void includeY(double y) {
        if (y == Double.MAX_VALUE || y == -Double.MAX_VALUE) {
            return;
        }

        if (y < yMin) {
            yMin = y;
        }
        if (y > yMax) {
            yMax = y;
        }
    }

    public void includeZ(double z) {
        if (z == Double.MAX_VALUE || z == -Double.MAX_VALUE) {
            return;
        }

        if (z < zMin) {
            zMin = z;
        }
        if (z > zMax) {
            zMax = z;
        }
    }

    public void include(Double x, Double y, Double z) {
        if (x != null) {
            includeX(x);
        }
        if (y != null) {
            includeY(y);
        }
        if (z != null) {
            includeZ(z);
        }
    }

    public boolean isXMinValid() {
        return xMin != Double.MAX_VALUE;
    }

    public double getXMin() {
        return xMin;
    }

    public boolean isXMaxValid() {
        return xMax != -Double.MAX_VALUE;
    }

    public double getXMax() {
        return xMax;
    }

    public boolean isYMinValid() {
        return yMin != Double.MAX_VALUE;
    }

    public double getYMin() {
        return yMin;
    }

    public boolean isYMaxValid() {
        return yMax != -Double.MAX_VALUE;
    }

    public double getYMax() {
        return yMax;
    }

    public boolean isZMinValid() {
        return zMin != Double.MAX_VALUE;
    }

    public double getZMin() {
        return zMin;
    }

    public boolean isZMaxValid() {
        return zMax != -Double.MAX_VALUE;
    }

    public double getZMax() {
        return zMax;
    }
}
