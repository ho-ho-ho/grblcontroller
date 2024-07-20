package in.co.gorest.grblcontroller.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableArrayList;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentCamBinding;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Tool;
import in.co.gorest.grblcontroller.util.ToolDatabase;

public class CamFragment extends BaseFragment {
    private final ObservableArrayList<Tool> tools = new ObservableArrayList<>();
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;
    private ToolDatabase toolDatabase;

    public CamFragment() {
    }

    public static CamFragment newInstance() {
        return new CamFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(
                requireActivity().getApplicationContext(),
                getString(R.string.shared_preference_key));
        toolDatabase = ToolDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentCamBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cam,
                container, false);
        binding.setToolDatabase(toolDatabase);
        View view = binding.getRoot();

        return view;
    }

}