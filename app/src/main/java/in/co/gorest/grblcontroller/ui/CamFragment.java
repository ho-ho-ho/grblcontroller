package in.co.gorest.grblcontroller.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentCamBinding;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.util.ToolLibrary;

public class CamFragment extends BaseFragment {
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentCamBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cam,
                container, false);
        binding.setToolLibrary(ToolLibrary.INSTANCE);
        View view = binding.getRoot();

        return view;
    }

}