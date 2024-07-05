package in.co.gorest.grblcontroller.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ProbingAdapter extends FragmentStateAdapter {
    public ProbingAdapter(FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return ProbingTloFragment.newInstance();
            case 2:
                return ProbingCenterFragment.newInstance();
            default:
                return ProbingBasicFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
