package com.VanLesh.macsv10.macs;

import android.support.v4.app.Fragment;

import com.VanLesh.macsv10.macs.Fragments.CalculationFragment;

import java.util.UUID;

public abstract class CalculationActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        //return new CalculationFragment();
        UUID calculationId = (UUID) getIntent()
         .getSerializableExtra(CalculationFragment.EXTRA_CALCULATION_ID);
        return CalculationFragment.newInstance(calculationId);

    }
}