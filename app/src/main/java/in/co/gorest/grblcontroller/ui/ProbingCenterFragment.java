package in.co.gorest.grblcontroller.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;

import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingCenterBinding;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.model.Position;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingCenterFragment extends BaseFragment {
    private enum Axis {X, Y}

    private enum Direction {
        Plus,
        Minus;

        public int value() {
            switch (this) {
                case Plus:
                    return 1;
                case Minus:
                    return -1;
            }

            return 0;
        }
    }

    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;

    private Position probeStartPosition;
    private String distanceMode;
    private String unitSelection;
    private Axis currentAxis;
    private Direction currentDirection;
    private final ObservableBoolean posLeftValid = new ObservableBoolean(false);
    private final ObservableBoolean posRightValid = new ObservableBoolean(false);
    private final ObservableBoolean posFrontValid = new ObservableBoolean(false);
    private final ObservableBoolean posBackValid = new ObservableBoolean(false);
    private double posLeft = 0.0;
    private double posRight = 0.0;
    private double posFront = 0.0;
    private double posBack = 0.0;
    private boolean fromOutside = false;

    public ProbingCenterFragment() {
    }

    public static ProbingCenterFragment newInstance() {
        return new ProbingCenterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(requireActivity().getApplicationContext(), getString(R.string.shared_preference_key));

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("posLeft")) {
                posLeft = savedInstanceState.getDouble("posLeft");
                posLeftValid.set(true);
            }
            if (savedInstanceState.containsKey("posRight")) {
                posRight = savedInstanceState.getDouble("posRight");
                posRightValid.set(true);
            }
            if (savedInstanceState.containsKey("posFront")) {
                posFront = savedInstanceState.getDouble("posFront");
                posFrontValid.set(true);
            }
            if (savedInstanceState.containsKey("posBack")) {
                posBack = savedInstanceState.getDouble("posBack");
                posBackValid.set(true);
            }
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (posLeftValid.get()) {
            outState.putDouble("posLeft", posLeft);
        }
        if (posRightValid.get()) {
            outState.putDouble("posRight", posRight);
        }
        if (posFrontValid.get()) {
            outState.putDouble("posFront", posFront);
        }
        if (posBackValid.get()) {
            outState.putDouble("posBack", posBack);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentProbingCenterBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_center, container, false);
        binding.setMachineStatus(machineStatus);
        binding.setPosLeftValid(posLeftValid);
        binding.setPosRightValid(posRightValid);
        binding.setPosFrontValid(posFrontValid);
        binding.setPosBackValid(posBackValid);

        View view = binding.getRoot();

        ToggleButton outsideBtn = view.findViewById(R.id.probing_center_outside_btn);
        fromOutside = outsideBtn.isChecked();
        outsideBtn.setOnClickListener(v -> {
            posLeftValid.set(false);
            posRightValid.set(false);
            posBackValid.set(false);
            posFrontValid.set(false);
            fromOutside = outsideBtn.isChecked();
        });

        IconButton leftBtn = view.findViewById(R.id.probe_center_x_left_btn);
        leftBtn.setOnClickListener(view1 -> {
            currentAxis = Axis.X;
            currentDirection = Direction.Minus;
            startProbing();
        });

        IconButton rightBtn = view.findViewById(R.id.probe_center_x_right_btn);
        rightBtn.setOnClickListener(view1 -> {
            currentAxis = Axis.X;
            currentDirection = Direction.Plus;
            startProbing();
        });

        IconButton backBtn = view.findViewById(R.id.probe_center_y_back_btn);
        backBtn.setOnClickListener(view1 -> {
            currentAxis = Axis.Y;
            currentDirection = Direction.Plus;
            startProbing();
        });

        IconButton frontBtn = view.findViewById(R.id.probe_center_y_front_btn);
        frontBtn.setOnClickListener(view1 -> {
            currentAxis = Axis.Y;
            currentDirection = Direction.Minus;
            startProbing();
        });

        IconButton applyBtn = view.findViewById(R.id.probe_center_apply_btn);
        applyBtn.setOnClickListener(view1 -> {
            if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
                return;
            }

            if (posLeftValid.get() && posRightValid.get()) {
                double center = (posLeft + posRight) / 2;
                applyCenter(Axis.X, center);
                posLeftValid.set(false);
                posRightValid.set(false);
            }

            if (posBackValid.get() && posFrontValid.get()) {
                double center = (posBack + posFront) / 2;
                applyCenter(Axis.Y, center);
                posBackValid.set(false);
                posFrontValid.set(false);
            }

            EventBus.getDefault().post(new UiToastEvent("Center applied successfully."));
        });

        return view;
    }

    private void probeXYAxis(double probeTo) {
        double touchDistanceXY = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_touch_distance_xy), "10.0")
        );
        double touchDepthXY = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_touch_depth_xy), "10.0")
        );
        double touchFeedrate = Double.parseDouble(
                sharedPref.getString(getString(R.string.preference_probing_feed_rate), "50.0")
        );
        double relativeMove = (currentDirection == Direction.Minus) ? -touchDistanceXY : touchDistanceXY;

        fragmentInteractionListener.onGcodeCommandReceived("G91");
        if (fromOutside) {
            fragmentInteractionListener.onGcodeCommandReceived("G53G0 " + currentAxis.name() + (probeTo + relativeMove));
            fragmentInteractionListener.onGcodeCommandReceived("G0 Z-" + touchDepthXY);
            fragmentInteractionListener.onGcodeCommandReceived("G38.3 " + currentAxis.name() + (-relativeMove) + " F" + touchFeedrate);
        } else {
            fragmentInteractionListener.onGcodeCommandReceived("G0 Z-" + touchDepthXY);
            fragmentInteractionListener.onGcodeCommandReceived("G38.3 " + currentAxis.name() + relativeMove + " F" + touchFeedrate);
        }
        fragmentInteractionListener.onGcodeCommandReceived("G90");
    }

    private void onProbeXYAxis(double start, double probedPosition) {
        switch (currentAxis) {
            case X:
                if (currentDirection == Direction.Plus) {
                    posRightValid.set(true);
                    posRight = probedPosition;
                } else {
                    posLeftValid.set(true);
                    posLeft = probedPosition;
                }
                break;
            case Y:
                if (currentDirection == Direction.Plus) {
                    posBackValid.set(true);
                    posBack = probedPosition;
                } else {
                    posFrontValid.set(true);
                    posFront = probedPosition;
                }
                break;
        }

        if (fromOutside) {
            fragmentInteractionListener.onGcodeCommandReceived("G53G0" + currentAxis.name() + (probedPosition + currentDirection.value()));
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.getCordZ());
            fragmentInteractionListener.onGcodeCommandReceived("G53G0" + currentAxis.name() + start);
        } else {
            fragmentInteractionListener.onGcodeCommandReceived("G53G0" + currentAxis.name() + start);
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.getCordZ());
        }
    }

    private void applyCenter(Axis axis, double center) {
        fragmentInteractionListener.onGcodeCommandReceived("G53G0" + axis.name() + center);
        fragmentInteractionListener.onGcodeCommandReceived("G90G10L20P0" + axis.name() + "0");
    }

    private void startProbing() {
        if (!machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)) {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle), true, true));
            return;
        }

        fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

        // Wait for few milliseconds, just to make sure we got the parser state
        new Handler().postDelayed(() -> {
            distanceMode = machineStatus.getParserState().distanceMode;
            unitSelection = machineStatus.getParserState().unitSelection;

            probeStartPosition = machineStatus.getMachinePosition();
            probeXYAxis(probeStartPosition.getCordX());
        }, (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblProbeEvent(GrblProbeEvent event) {
        if (!event.getIsProbeSuccess()) {
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_failed), true, true));
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.getCordZ());

            return;
        }

        double startPosition = currentAxis == Axis.X ? probeStartPosition.getCordX() : probeStartPosition.getCordY();
        double probedPosition = currentAxis == Axis.X ? event.getProbeCordX() : event.getProbeCordY();
        onProbeXYAxis(startPosition, probedPosition);

        fragmentInteractionListener.onGcodeCommandReceived(distanceMode + unitSelection);

        machineStatus.setLastProbePosition(event.getProbePosition());
    }
}
