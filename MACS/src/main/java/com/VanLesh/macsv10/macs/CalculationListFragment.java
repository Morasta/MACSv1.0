package com.VanLesh.macsv10.macs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.VanLesh.macsv10.macs.Fragments.SplashScreen;
import com.VanLesh.macsv10.macs.Models.Calculation;

import java.util.ArrayList;


/**
 * Created by samvanryssegem on 2/27/14.
 */
class CalculationListFragment extends ListFragment {

    private boolean mSubtitleVisible;
    private Callbacks mCallbacks;

    public interface Callbacks {
        void onCalculationSelected(Calculation calc);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void updateUI() {
        ((CalculationAdapter) getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.calculations_title);
        ArrayList<Calculation> calculations = CalculationLab.get(getActivity()).getCalculations();

        CalculationAdapter adapter = new CalculationAdapter(calculations);

        setListAdapter(adapter);
        setRetainInstance(true);
        mSubtitleVisible = false;
    }

    @TargetApi(11)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        View v = super.onCreateView(inflater, parent, savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (mSubtitleVisible) {
                getActivity().getActionBar().setSubtitle(R.string.subtitle);
            }
        }

        ListView listView = (ListView) v.findViewById(android.R.id.list);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            registerForContextMenu(listView);
        else {
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.calc_list_item_context, menu);
                    return true;
                }

                public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                      long id, boolean checked) {
                }

                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_item_delete_calc:
                            CalculationAdapter adapter = (CalculationAdapter) getListAdapter();
                            CalculationLab calcLab = CalculationLab.get(getActivity());
                            for (int i = adapter.getCount() - 1; i >= 0; i--) {
                                if (getListView().isItemChecked(i)) {
                                    calcLab.deleteCalculation(adapter.getItem(i));
                                }
                            }
                            mode.finish();
                            adapter.notifyDataSetChanged();
                            return true;
                        default:
                            return false;
                    }
                }

                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public void onDestroyActionMode(ActionMode mode) {

                }
            });

        }

        return v;

    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        Calculation c = ((CalculationAdapter) getListAdapter()).getItem(position);
        mCallbacks.onCalculationSelected(c);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ((CalculationAdapter) getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_calc_list, menu);

    }

    @TargetApi(11)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_new_calc:
                Calculation calc = new Calculation();
                CalculationLab.get(getActivity()).addCalculation(calc);
                ((CalculationAdapter) getListAdapter()).notifyDataSetChanged();
                mCallbacks.onCalculationSelected(calc);
                return true;

            case R.id.menu_item_publication:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse( "http://people.forestry.oregonstate.edu/ben-leshchinsky/"));
                startActivity(browserIntent);

            case R.id.menu_about:
                Intent splashIntent = new Intent(getActivity(), SplashScreen.class);
                startActivity(splashIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.calc_list_item_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        CalculationAdapter adapter = (CalculationAdapter) getListAdapter();
        Calculation calculation = adapter.getItem(position);

        switch (item.getItemId()) {
            case R.id.menu_item_delete_calc:
                CalculationLab.get(getActivity()).deleteCalculation(calculation);
                adapter.notifyDataSetChanged();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private class CalculationAdapter extends ArrayAdapter<Calculation> {
        public CalculationAdapter(ArrayList<Calculation> calculations) {
            super(getActivity(), 0, calculations);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //if we dont have a view inflate one
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                .inflate(R.layout.list_item_calculation, null);
            }

            //configure view for this calculation
            Calculation c = getItem(position);

            TextView titleTextView =
            (TextView) convertView.findViewById(R.id.calculation_list_item_titleTextView);
            titleTextView.setText(c.getTitle());

            TextView dateTextView =
            (TextView) convertView.findViewById(R.id.calculation_list_item_dateTextView);
            dateTextView.setText(c.getDate().toString());
            return convertView;
        }
    }


}
