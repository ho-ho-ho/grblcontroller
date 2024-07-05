package in.co.gorest.grblcontroller.util;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.databinding.ObservableDouble;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;

import in.co.gorest.grblcontroller.GrblController;
import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Position;
import in.co.gorest.grblcontroller.ui.BaseFragment;

// handles different scenarios for probing work
public class GrblProbe {
    public enum ProbeType {Straight, ToolLengthOffset, Center}

    public enum Axis {X, Y, Z}

    public enum Direction {
        Minus, Plus;

        public boolean isPositive() {
            return this == Plus;
        }
    }

    public enum CenterDirection {Inside, Outside}

    public static class Configuration {
        protected final ProbeType type;
        protected final LinkedList<Pair<Axis, Direction>> axes;
        protected final CenterDirection centerDir;
        protected final boolean zeroZ;
        protected final boolean referenceZ;

        Configuration(Builder builder) {
            type = builder.type;
            axes = builder.axes;
            centerDir = builder.centerDir;
            zeroZ = builder.zeroZ;
            referenceZ = builder.referenceZ;
        }

        public static class Builder {
            private ProbeType type = null;
            private final LinkedList<Pair<Axis, Direction>> axes = new LinkedList<>();
            private CenterDirection centerDir = null;
            private boolean zeroZ = false;
            private boolean referenceZ = false;

            public Builder type(ProbeType probeType) {
                type = probeType;
                return this;
            }

            public Builder addAxisDir(Axis axis, Direction dir) {
                axes.add(new Pair<>(axis, dir));
                return this;
            }

            public Builder centerDirection(CenterDirection centerDirection) {
                centerDir = centerDirection;
                return this;
            }

            public Builder zeroZ(boolean zero) {
                zeroZ = zero;
                return this;
            }

            public Builder referenceZ(boolean reference) {
                referenceZ = reference;
                return this;
            }

            public Configuration build() {
                return new Configuration(this);
            }
        }
    }

    private static GrblProbe grblProbe = null;

    private final BaseFragment.OnFragmentInteractionListener grblHandler;
    private final EnhancedSharedPreferences sharedPref;
    private final MachineStatusListener machineStatus;

    // state variables
    private boolean isRunning = false;
    private Configuration stateConfig;
    private Position startPosition = null;
    private String distanceMode;
    private String unitSelection;

    private final ObservableDouble referenceZValue = new ObservableDouble(Double.MAX_VALUE);
    private final ObservableDouble centerPosLeft = new ObservableDouble(Double.MAX_VALUE);
    private final ObservableDouble centerPosRight = new ObservableDouble(Double.MAX_VALUE);
    private final ObservableDouble centerPosBack = new ObservableDouble(Double.MAX_VALUE);
    private final ObservableDouble centerPosFront = new ObservableDouble(Double.MAX_VALUE);

    private GrblProbe(BaseFragment.OnFragmentInteractionListener handler, EnhancedSharedPreferences enhancedSharedPreferences) {
        grblHandler = handler;
        sharedPref = enhancedSharedPreferences;
        machineStatus = MachineStatusListener.getInstance();

        EventBus.getDefault().register(this);
    }

    public static GrblProbe getInstance(Context context, BaseFragment.OnFragmentInteractionListener handler) {
        if (grblProbe == null) {
            grblProbe = new GrblProbe(
                    handler,
                    EnhancedSharedPreferences.getInstance(context, context.getString(R.string.shared_preference_key))
            );
        }

        return grblProbe;
    }

    public ObservableDouble getReferenceZValue() {
        return referenceZValue;
    }

    public ObservableDouble getCenterPosLeft() {
        return centerPosLeft;
    }

    public ObservableDouble getCenterPosRight() {
        return centerPosRight;
    }

    public ObservableDouble getCenterPosBack() {
        return centerPosBack;
    }

    public ObservableDouble getCenterPosFront() {
        return centerPosFront;
    }

    // since we don't have access to the context here, we'll use the GrblController instance to get those strings
    private double getSettingProbingDistanceXY() {
        return Double.parseDouble(sharedPref.getString(
                GrblController.getInstance().getString(R.string.preference_probing_touch_distance_xy),
                "15.0"
        ));
    }

    private double getSettingProbingDepthXY() {
        return Double.parseDouble(sharedPref.getString(
                GrblController.getInstance().getString(R.string.preference_probing_touch_depth_xy), "10.0"
        ));
    }

    private double getSettingProbingFeedRate() {
        return Double.parseDouble(sharedPref.getString(
                GrblController.getInstance().getString(R.string.preference_probing_feed_rate), "50.0"
        ));
    }

    private double getSettingProbingToolDiameter() {
        return Double.parseDouble(sharedPref.getString(
                GrblController.getInstance().getString(R.string.preference_probing_tool_diameter), "6.0"
        ));
    }

    private double getSettingProbingPlateOffsetX() {
        return Double.parseDouble(sharedPref.getString(
                GrblController.getInstance().getString(R.string.preference_probing_touch_plate_x_offset), "10.0"
        ));
    }

    private double getSettingProbingPlateOffsetY() {
        return Double.parseDouble(sharedPref.getString(
                GrblController.getInstance().getString(R.string.preference_probing_touch_plate_y_offset), "10.0"
        ));
    }

    private double getSettingProbingPlateOffsetZ() {
        return Double.parseDouble(sharedPref.getString(
                GrblController.getInstance().getString(R.string.preference_probing_plate_thickness), "10.0"
        ));
    }

    private double getTouchDepth() {
        return startPosition.getCordZ() - getSettingProbingDepthXY();
    }

    private double getTouchPlateOffsetForAxis(Axis axis) {
        switch (axis) {
            case X:
                return getSettingProbingPlateOffsetX();
            case Y:
                return getSettingProbingPlateOffsetY();
            default:
                return getSettingProbingPlateOffsetZ();
        }
    }

    private double getStartForAxis(Axis axis) {
        switch (axis) {
            case Y:
                return startPosition.getCordY();
            case Z:
                return startPosition.getCordZ();
            default:
                return startPosition.getCordX();
        }
    }

    // the probe first travels to the waypoint in starting Z height and then lowers by touchDepth for the actual probing towards the probe target.
    private double getWaypointXY(Pair<Axis, Direction> axisDir) {
        if (stateConfig.type == ProbeType.Center && stateConfig.centerDir == CenterDirection.Inside) {
            return getStartForAxis(axisDir.first);
        } else {
            double touchPlateOffset = getSettingProbingDistanceXY();
            double offset = axisDir.second.isPositive() ? touchPlateOffset : -touchPlateOffset;
            return getStartForAxis(axisDir.first) + offset;
        }
    }

    private double getProbeTargetXY(Pair<Axis, Direction> axisDir) {
        if (stateConfig.type == ProbeType.Center && stateConfig.centerDir == CenterDirection.Inside) {
            double touchPlateOffset = getSettingProbingDistanceXY();
            double offset = axisDir.second.isPositive() ? touchPlateOffset : -touchPlateOffset;
            return getStartForAxis(axisDir.first) + offset;
        } else {
            return getStartForAxis(axisDir.first);
        }
    }

    private double getProbeTargetZ() {
        return startPosition.getCordZ() - getSettingProbingDepthXY();
    }

    private double getBackoffXY(Pair<Axis, Direction> axisDir) {
        if (stateConfig.type == ProbeType.Center && stateConfig.centerDir == CenterDirection.Inside) {
            return getStartForAxis(axisDir.first);
        } else {
            double touchPlateOffset = getSettingProbingDistanceXY();
            double offset = axisDir.second.isPositive() ? (touchPlateOffset + 1) : -(touchPlateOffset + 1);
            return getStartForAxis(axisDir.first) + offset;
        }
    }

    private void probeXYAxis(Pair<Axis, Direction> axisDir) {
        double waypoint = getWaypointXY(axisDir);
        double target = getProbeTargetXY(axisDir);
        double offset = target - waypoint;

        grblHandler.onGcodeCommandReceived("G53G0" + axisDir.first.name() + waypoint);
        grblHandler.onGcodeCommandReceived("G53G0Z" + getTouchDepth());
        grblHandler.onGcodeCommandReceived("G91");
        grblHandler.onGcodeCommandReceived("G38.3" + axisDir.first.name() + offset + "F" + getSettingProbingFeedRate());
    }

    private void onProbeXYAxis(Pair<Axis, Direction> axisDir, double probedPosition) {
        double start = getStartForAxis(axisDir.first);
        double backoff = getBackoffXY(axisDir);

        double offset = getTouchPlateOffsetForAxis(axisDir.first) + getSettingProbingToolDiameter() / 2.0;
        double finalOffset = axisDir.second.isPositive() ? offset : -offset;

        if (stateConfig.type == ProbeType.Straight) {
            grblHandler.onGcodeCommandReceived("G53G0" + axisDir.first.name() + probedPosition);
            grblHandler.onGcodeCommandReceived("G90");
            grblHandler.onGcodeCommandReceived("G10L20P0" + axisDir.first.name() + finalOffset);
        } else if (stateConfig.type == ProbeType.Center) {
            if (axisDir.first == Axis.X) {
                if (axisDir.second == Direction.Minus) {
                    centerPosLeft.set(probedPosition);
                } else {
                    centerPosRight.set(probedPosition);
                }
            } else {
                if (axisDir.second == Direction.Minus) {
                    centerPosFront.set(probedPosition);
                } else {
                    centerPosBack.set(probedPosition);
                }
            }
        }

        grblHandler.onGcodeCommandReceived("G53G0" + axisDir.first.name() + backoff);
        grblHandler.onGcodeCommandReceived("G53G0Z" + startPosition.getCordZ());
        grblHandler.onGcodeCommandReceived("G53G0" + axisDir.first.name() + start);
    }

    private void probeZAxis(Pair<Axis, Direction> axisDir) {
        grblHandler.onGcodeCommandReceived("G91");
        grblHandler.onGcodeCommandReceived("G38.3Z" + getSettingProbingDepthXY() + "F" + getSettingProbingFeedRate());
    }

    private void onProbeZAxis(Pair<Axis, Direction> axisDir, double probedPosition) {
        if (stateConfig.type != ProbeType.ToolLengthOffset) {
            if (stateConfig.zeroZ) {
                grblHandler.onGcodeCommandReceived("G53G0Z" + probedPosition);
                grblHandler.onGcodeCommandReceived("G90");
                grblHandler.onGcodeCommandReceived("G10L20P0Z" + getSettingProbingPlateOffsetZ());
            }
        } else {
            if (stateConfig.referenceZ) {
                referenceZValue.set(probedPosition);
                EventBus.getDefault().post(new UiToastEvent(GrblController.getInstance().getString(R.string.text_probing_tlo_probe_reference_success)));
            } else {
                double toolOffset = referenceZValue.get() - probedPosition;
                grblHandler.onGcodeCommandReceived("G43.1Z" + toolOffset);
                grblHandler.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                EventBus.getDefault().post(new UiToastEvent(GrblController.getInstance().getString(R.string.text_probe_success_with_tlo)));
            }
        }

        grblHandler.onGcodeCommandReceived("G53G0Z" + getStartForAxis(axisDir.first));
    }

    private void probeAxis(Pair<Axis, Direction> axisDir) {
        if (axisDir.first != Axis.Z) {
            probeXYAxis(axisDir);
        } else {
            probeZAxis(axisDir);
        }
    }

    private void stateMachine() {
        Pair<Axis, Direction> axisDir = stateConfig.axes.peekFirst();
        if (axisDir == null) {
            // finished probing
            grblHandler.onGcodeCommandReceived("G53G0Z" + startPosition.getCordZ());
            grblHandler.onGcodeCommandReceived(distanceMode + unitSelection);
            isRunning = false;
            stateConfig = null;
            return;
        }

        if (stateConfig.type != ProbeType.ToolLengthOffset) {
            probeAxis(axisDir);
        } else {
            probeZAxis(axisDir);
        }
    }

    public void probe(Configuration config) {
        if (isRunning) {
            return;
        }

        if (config.type == ProbeType.ToolLengthOffset) {
            if (config.axes.size() != 1) {
                return;
            }
            if (config.axes.get(0).first != Axis.Z) {
                return;
            }
        } else {
            //TODO: check if one axis is present with both Plus and Minus directions and return if true
        }

        stateConfig = config;
        startPosition = machineStatus.getMachinePosition();
        distanceMode = machineStatus.getParserState().distanceMode;
        unitSelection = machineStatus.getParserState().unitSelection;
        isRunning = true;
        stateMachine();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblProbeEvent(GrblProbeEvent event) {
        if (!isRunning) {
            return;
        }

        if (!event.getIsProbeSuccess()) {
            if (startPosition != null) {
                grblHandler.onGcodeCommandReceived("G53G0Z" + startPosition.getCordZ());
            }
            grblHandler.onGcodeCommandReceived(distanceMode + unitSelection);

            isRunning = false;
            stateConfig = null;
            startPosition = null;
            return;
        }

        Pair<Axis, Direction> axisDir = stateConfig.axes.removeFirst();
        if (axisDir.first != Axis.Z) {
            onProbeXYAxis(axisDir, (axisDir.first == Axis.X) ? event.getProbeCordX() : event.getProbeCordY());
        } else {
            onProbeZAxis(axisDir, event.getProbeCordZ());
        }

        stateMachine();

        machineStatus.setLastProbePosition(event.getProbePosition());
    }
}
