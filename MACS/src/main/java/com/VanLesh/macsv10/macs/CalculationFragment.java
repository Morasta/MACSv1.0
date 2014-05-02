//TODO: Emailing(report)
package com.VanLesh.macsv10.macs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnTextChanged;
import butterknife.Optional;

/**
 *
 * Created by samvanryssegem on 2/24/14.
 *
 *Fragment which defines the main Form's behavior. This also handles behavior for the Vehicle and
 * Soil popups. In short if this is a new Calculation all of the fields should be empty, otherwise
 * we load. The Calculate button @OnClick actually puts everything into a Calculation object and then
 * performs the operation. This prevents a lot of nastiness associated with running a calculation
 * more than once.
 *
 * A lot of boilerplate was removed by using ButterKnife. If you're not familiar with the library
 * check it out here... https://github.com/JakeWharton/butterknife
 *
 */

public class CalculationFragment extends Fragment{

    private static final int REQUEST_DATE = 0;
    public static String EXTRA_CALCULATION_ID = "calculationintent.calculation_id";
    private static final String DIALOG_DATE = "date";

    private Calculation mCalculation;
    Callbacks mCallbacks;

    //InjectionView definitions needed by Butterknife
    @InjectView(R.id.calculation_title) EditText calculationTitle;
    @InjectView(R.id.calculation_engineer) EditText calculationEngineer;
    @InjectView(R.id.calculation_jobsite) EditText calculationJobsite;
    @InjectView(R.id.calculation_beta) EditText calculationBeta;
    @InjectView(R.id.calculation_theta) EditText calculationTheta;
    @InjectView(R.id.calculation_anchor_height) EditText calculationAnchorHeight;
    @InjectView(R.id.calculation_anchor_setback) EditText calculationAnchorSetback;
    @InjectView(R.id.calculation_blade_depth) EditText calculationBladeDepth;

    @InjectView(R.id.dragging_value) TextView dragValue;
    @InjectView(R.id.moment_value) TextView momentValue;

    @InjectView(R.id.toggle_units) ToggleButton unitsToggle;

    @InjectView(R.id.perform_calculation) Button performCalculation;
    @InjectView(R.id.calculation_date) Button calculationDate;
    Button mReportButton;

    @Optional @InjectView(R.id.question_hg) ImageButton HgQuestion;
    @Optional @InjectView(R.id.question_db) ImageButton DbQuestion;
    @Optional @InjectView(R.id.question_wv) ImageButton WvQuestion;
    @Optional @InjectView(R.id.question_la) ImageButton LaQuestion;
    @Optional @InjectView(R.id.question_ha) ImageButton HaQuestion;
    @Optional @InjectView(R.id.question_cg) ImageButton CgQuestion;
    @Optional @InjectView(R.id.question_wb) ImageButton WbQuestion;
    @Optional @InjectView(R.id.question_fricta) ImageButton PhiQuestion;
    @Optional @InjectView(R.id.question_cohesion) ImageButton CohesionQuestion;
    @Optional @InjectView(R.id.question_ws) ImageButton UnitWeightQuestion;

    @Optional @InjectView(R.id.Ha_unit) TextView HaUnit;
    @Optional @InjectView(R.id.La_unit) TextView LaUnit;
    @Optional @InjectView(R.id.Db_unit) TextView DbUnit;


    @InjectView(R.id.vehicle_add) ImageButton addVehicle;
    @InjectView(R.id.vehicle_delete)ImageButton deleteVehicle;
    @InjectView(R.id.soil_add) ImageButton addSoil;
    @InjectView(R.id.soil_delete) ImageButton deleteSoil;

    @InjectView(R.id.current_latitude) TextView latitudeField;
    @InjectView(R.id.current_longitude)TextView longitudeField;
    @InjectView(R.id.vehicle_spinner) Spinner mVehicleSpin;

    void populateCalculation()throws NullPointerException{
        try {
            mCalculation.setTitle(calculationTitle.getText().toString());
            mCalculation.setEngineerName(calculationEngineer.getText().toString());
            mCalculation.setJobSite(calculationJobsite.getText().toString());
            mCalculation.setBeta(Integer.parseInt(calculationBeta.getText().toString()));
            mCalculation.setTheta(Integer.parseInt(calculationTheta.getText().toString()));
            mCalculation.setHa(Double.parseDouble(calculationAnchorHeight.getText().toString()));
            mCalculation.setLa(Double.parseDouble(calculationAnchorSetback.getText().toString()));
            mCalculation.setD_b(Double.parseDouble(calculationBladeDepth.getText().toString()));
        }catch(NullPointerException e){
            Toast thistoast = Toast.makeText(getActivity(),"A field was left unpopulated",Toast.LENGTH_LONG);
        }
        Log.i("Title",calculationTitle.toString());
    }

    double latitudeValue;
    double longitudeValue;

    GPSTracker gps;


    String answerUnits;
    boolean isimperial =false;
    boolean hasbeencalculated =false;

    private ArrayList<Vehicle> mVehicles;
    private ArrayList<Soil> mSoils;


    public interface Callbacks{
        void onCalculationUpdated(Calculation calculation);

    }

    public static CalculationFragment newInstance(UUID calculationId){
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_CALCULATION_ID, calculationId);
        CalculationFragment fragment = new CalculationFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        mCallbacks =(Callbacks)activity;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        mCallbacks = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        UUID calculationId = (UUID)getArguments().getSerializable(EXTRA_CALCULATION_ID);
        mCalculation = CalculationLab.get(getActivity()).getCalculation(calculationId);

        mSoils = SoilLab.get(getActivity()).getSoils();
        staticSoils(mSoils);

        mVehicles = VehicleLab.get(getActivity()).getVehicles();
        staticVehicles(mVehicles);

        gps = new GPSTracker(getActivity());
        if (gps.canGetLocation()){
            latitudeValue = gps.getLatitude();
            longitudeValue =gps.getLongitude();
        }else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    void updateDate(){
        calculationDate.setText(mCalculation.getDate().toString());
    }

    @TargetApi(11)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        //inflate our view based on the layout defined
        final View v = inflater.inflate(R.layout.fragment_calculation, parent, false);
        // inject all of those views we created earlier
        //this single line eliminates
        ButterKnife.inject(this, v);
        final Dialog soildialog = new Dialog(getActivity());
        final Dialog vehicledialog = new Dialog(getActivity());


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                 getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        answerUnits = getResources().getString(R.string.metric_answer);
        latitudeField.setText(String.valueOf(latitudeValue));
        longitudeField.setText(String.valueOf(longitudeValue));
        calculationTitle.setText(mCalculation.getTitle());
        calculationEngineer.setText(mCalculation.getEngineerName());
        calculationJobsite.setText(mCalculation.getJobSite());

        if (mCalculation.getBeta() != 0)
            calculationBeta.setText(Integer.toString(mCalculation.getBeta()));

        if (mCalculation.getTheta() != 0)
            calculationTheta.setText(Integer.toString(mCalculation.getTheta()));

        if (mCalculation.getHa() !=0)
            calculationAnchorHeight.setText(Double.toString(mCalculation.getHa()));

        if (mCalculation.getLa() != 0)
            calculationAnchorSetback.setText(Double.toString(mCalculation.getLa()));

        if (mCalculation.getD_b() != 0)
            calculationBladeDepth.setText(Double.toString(mCalculation.getD_b()));


        unitsToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                //Unit textfield declarations for main view
                //for the soil dialog
                // if it's checked lets set all of the textview to their imperial counterparts
                if (isChecked) {
                    HaUnit.setText(getResources().getString(R.string.imperial_distance));
                    LaUnit.setText(getResources().getString(R.string.imperial_distance));
                    DbUnit.setText(getResources().getString(R.string.imperial_distance));

                    answerUnits = getResources().getString(R.string.imperial_answer);
                    isimperial = true;
                    mCalculation.isimperial = true;
                    mCalculation.getVehicle().isimperial = true;
                    mCalculation.getSoil().isimperial = true;
                    hasbeencalculated = false;

                    // default behavior for the program is metric units
                } else {
                    HaUnit.setText(getResources().getString(R.string.metric_distance));
                    LaUnit.setText(getResources().getString(R.string.metric_distance));
                    DbUnit.setText(getResources().getString(R.string.metric_distance));

                    answerUnits = getResources().getString(R.string.metric_answer);
                    mCalculation.isimperial = false;
                    mCalculation.getVehicle().isimperial = false;
                    mCalculation.getSoil().isimperial = false;
                    hasbeencalculated = false;

                }
                // invalidating the view forces it to re-render. This will display our changes
                //without the user having to perform an action.
                v.invalidate();
            }
        });

        ToastMaker(R.string.ha_popup,HaQuestion,v);
        ToastMaker(R.string.la_popup,LaQuestion,v);
        ToastMaker(R.string.db_popup,DbQuestion,v);

        updateDate();
        calculationDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCalculation.getDate());
                dialog.setTargetFragment(CalculationFragment.this, REQUEST_DATE);
                dialog.show(fm, DIALOG_DATE);
            }
        });


        final ArrayList<String> vehicleoutputlist = new ArrayList<String>();
        String SingleVehicle;
        for (Vehicle vehicle : mVehicles){

            SingleVehicle = vehicle.getVehicleType();
            if (! vehicleoutputlist.contains(SingleVehicle))
                 vehicleoutputlist.add(SingleVehicle);

        }
        Collections.sort(vehicleoutputlist);

        final ArrayAdapter<String> vadapter = new ArrayAdapter<String>(getActivity(),R.layout.support_simple_spinner_dropdown_item,vehicleoutputlist);
        vadapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        mVehicleSpin.setAdapter(vadapter);

        mVehicleSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
              @Override
              public void onItemSelected(AdapterView<?> adapterView, View v, int i, long l) {
                  String vcheck =(String)mVehicleSpin.getSelectedItem();
                  Iterator<Vehicle> iterv = mVehicles.iterator();
                  while (iterv.hasNext()){
                      Vehicle currentv = iterv.next();
                      if (currentv.getVehicleType().matches(vcheck)){
                          mCalculation.setVehicle(currentv);
                      }
                  }
               }

              @Override
              public void onNothingSelected(AdapterView<?> adapterView) {}

        });

        deleteVehicle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Vehicle checkvehicle : mVehicles) {
                    if (checkvehicle.equals(mVehicleSpin.getSelectedItem()))
                        mVehicles.remove(checkvehicle);
                }
                vadapter.remove((String) mVehicleSpin.getSelectedItem());
                vadapter.notifyDataSetChanged();
            }


        });

        addVehicle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                vehicledialog.setContentView(R.layout.fragment_vehicle_picker);
                vehicledialog.setTitle("Enter Vehicle Data");

                final EditText mVehicleclass;
                final EditText mVehicletype;
                final EditText mVehicleHeight;
                final EditText mCOGDistance;
                final EditText mWeight;
                final EditText mTrackL;
                final EditText mTrackW;
                final EditText mBladeW;
                TextView VehicleWeight = (TextView) vehicledialog.findViewById(R.id.VehicleWeight_unit);
                TextView VehicleTrackLength = (TextView) vehicledialog.findViewById(R.id.VehicleTrackLength_unit);
                TextView VehicleTrackWidth = (TextView) vehicledialog.findViewById(R.id.VehicleTrackWidth_unit);
                TextView VehicleCg = (TextView) vehicledialog.findViewById(R.id.VehicleCg_unit);
                TextView VehicleBladeWidth = (TextView) vehicledialog.findViewById(R.id.VehicleBladeWidth_unit);
                TextView VehicleHeight = (TextView) vehicledialog.findViewById(R.id.VehicleHeight_unit);


                if (unitsToggle.isChecked()) {
                    VehicleHeight.setText(getResources().getString(R.string.imperial_distance));
                    VehicleWeight.setText(getResources().getString(R.string.imperial_weight));
                    VehicleCg.setText(getResources().getString(R.string.imperial_distance));
                    VehicleBladeWidth.setText(getResources().getString(R.string.imperial_distance));
                    VehicleTrackLength.setText(getResources().getString(R.string.imperial_distance));
                    VehicleTrackWidth.setText(getResources().getString(R.string.imperial_distance));


                } else {
                    VehicleHeight.setText(getResources().getString(R.string.metric_distance));
                    VehicleCg.setText(getResources().getString(R.string.metric_distance));
                    VehicleBladeWidth.setText(getResources().getString(R.string.metric_distance));
                    VehicleTrackLength.setText(getResources().getString(R.string.metric_distance));
                    VehicleTrackWidth.setText(getResources().getString(R.string.metric_distance));
                    VehicleWeight.setText(getResources().getString(R.string.metric_weight));


                }

                //this unfortunately long block defines all of our EditText fields
                // each EditText is mapped to it's resource Id, we check if the field needs to be filled
                // then assign the Calculation's appropriate vehicle parameter if something is entered into
                //the field
                mVehicleclass = (EditText) vehicledialog.findViewById(R.id.input_vehicle_class);
                //mVehicleclass.setText(mCalculation.getVehicle().getVehicleClass());
                mVehicleclass.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        mCalculation.getVehicle().setVehicleClass(charSequence.toString());

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                mVehicletype = (EditText) vehicledialog.findViewById(R.id.input_vehicle_type);
                //mVehicletype.setText(mCalculation.getVehicle().getVehicleType());
                mVehicletype.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        mCalculation.getVehicle().setType(charSequence.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                mVehicleHeight = (EditText) vehicledialog.findViewById(R.id.input_vehicle_height);
                // if (mCalculation.getVehicle().getHg() != 0)
                //     mVehicleHeight.setText(Double.toString(mCalculation.getVehicle().getHg()));

                mVehicleHeight.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sVehicleHeight = mVehicleHeight.getText().toString();
                        if (!sVehicleHeight.matches(""))
                            mCalculation.mVehicle.setHg(Double.parseDouble(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                mCOGDistance = (EditText) vehicledialog.findViewById(R.id.Cg);
                //  if(mCalculation.getVehicle().getCg() != 0)
                //      mCOGDistance.setText(Double.toString(mCalculation.getVehicle().getCg()));

                mCOGDistance.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sCOGDistance = mCOGDistance.getText().toString();
                        if (!sCOGDistance.matches(""))
                            mCalculation.getVehicle().setCg(Double.parseDouble(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                //TODO null validation
                mWeight = (EditText) vehicledialog.findViewById(R.id.Wv);
                //  if(mCalculation.getVehicle().getWv() != 0)
                //      mWeight.setText(Double.toString(mCalculation.getVehicle().getWv()));
                mWeight.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sWeight = mWeight.getText().toString();
                        if (!sWeight.matches(""))
                            mCalculation.getVehicle().setWv(Double.parseDouble(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                //TODO null validation
                mTrackL = (EditText) vehicledialog.findViewById(R.id.Tl);
                //  if(mCalculation.getVehicle().getTrackL() !=0)
                //      mTrackL.setText(Double.toString(mCalculation.getVehicle().getTrackL()));
                mTrackL.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sTrackLength = mTrackL.getText().toString();
                        if (!sTrackLength.matches(""))
                            mCalculation.getVehicle().setTrackL(Double.parseDouble(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                //TODO: null validation
                mTrackW = (EditText) vehicledialog.findViewById(R.id.Tw);
                //  if(mCalculation.getVehicle().getTrackW() != 0)
                //      mTrackW.setText(Double.toString(mCalculation.getVehicle().getTrackW()));
                mTrackW.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sTrackWidth = mTrackW.getText().toString();
                        if (!sTrackWidth.matches(""))
                            mCalculation.getVehicle().setTrackW(Double.parseDouble(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                mBladeW = (EditText) vehicledialog.findViewById(R.id.Wb);
                // if (mCalculation.getVehicle().getBladeW() !=0)
                //     mBladeW.setText(Double.toString(mCalculation.getVehicle().getBladeW()));
                mBladeW.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sBladeWidth = mBladeW.getText().toString();
                        if (!sBladeWidth.matches("")) {
                            mCalculation.getVehicle().setBladeW(Double.parseDouble(charSequence.toString()));
                        }
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }


                    @Override
                    public void afterTextChanged(Editable editable) {
                    }

                });


                Button mCancelButton = (Button) vehicledialog.findViewById(R.id.vehicle_cancel);
                mCancelButton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        mCalculation.setVehicle(null);
                        vehicledialog.dismiss();

                    }

                });

                Button mSaveButton = (Button) vehicledialog.findViewById(R.id.vehicle_save);
                mSaveButton.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        mCalculation.getVehicle().convertToMetric();
                        mVehicles.add(mCalculation.getVehicle());
                        vehicleoutputlist.add(mCalculation.getVehicle().getVehicleType());
                        VehicleLab.get(getActivity()).saveVehicles();
                        vehicledialog.dismiss();

                    }
                });


                // this code block defines the "?" buttons
                //We assign them the correct ID and provide the relevant string and ID for the toast

                ToastMaker(R.string.hg_popup, HgQuestion, v);
                ToastMaker(R.string.wb_popup, WbQuestion, v);
                ToastMaker(R.string.wv_popup, WvQuestion, v);
                ToastMaker(R.string.cg_popup, CgQuestion, v);


                vehicledialog.show();
            }
        });

        final ArrayList<String> SoilOutputList = new ArrayList<String>();
        String SingleSoil;
        for (Soil soil : mSoils) {
            SingleSoil = soil.getName();
            if (!SoilOutputList.contains(SingleSoil))
                SoilOutputList.add(SingleSoil);
        }
        Collections.sort(SoilOutputList);

        final Spinner mSoilSpin = (Spinner)v.findViewById(R.id.soil_spinner);
        final ArrayAdapter<String> sadapter = new ArrayAdapter<String>(getActivity(),R.layout.support_simple_spinner_dropdown_item,SoilOutputList);
        sadapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        mSoilSpin.setAdapter(sadapter);

        mSoilSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View v, int i, long l){
                String check = (String)mSoilSpin.getSelectedItem();

                Iterator<Soil> iter = mSoils.iterator();
                while (iter.hasNext()) {
                    Soil currents = iter.next();
                    if (currents.getName().matches(check))
                        mCalculation.setSoil(currents);

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        deleteSoil.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                for (Soil checksoil : mSoils) {
                    if (checksoil.equals(mSoilSpin.getSelectedItem()))
                        mSoils.remove(checksoil);
                }
                sadapter.remove((String) mSoilSpin.getSelectedItem());
                sadapter.notifyDataSetChanged();
            }

        });
        addSoil.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //custom soil dialog launches on new selection


                soildialog.setContentView(R.layout.fragment_soil_picker);
                soildialog.setTitle("Enter Soil Data");

                final EditText mSoilType;
                final EditText mSoilUnitWeight;
                final EditText mSoilFrictionAngle;
                final EditText mSoilCohesionFactor;

                TextView SoilWtUnit = (TextView) soildialog.findViewById(R.id.SoilWt_unit);
                TextView SoilCUnit = (TextView) soildialog.findViewById(R.id.Soilc_unit);

                if (unitsToggle.isChecked()) {
                    SoilWtUnit.setText(getResources().getString(R.string.imperial_soilunitwt));
                    SoilCUnit.setText(getResources().getString(R.string.imperial_soilc));

                } else {
                    SoilWtUnit.setText(getResources().getString(R.string.metric_soilunitwt));
                    SoilCUnit.setText(getResources().getString(R.string.metric_soilc));

                }


                mSoilType = (EditText) soildialog.findViewById(R.id.SoilName);
                mSoilType.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        mCalculation.getSoil().setName(charSequence.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                mSoilUnitWeight = (EditText) soildialog.findViewById(R.id.Soilunitwt);
                mSoilUnitWeight.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sSoilUnitWeight = mSoilType.getText().toString();
                        if (!sSoilUnitWeight.matches(""))
                            mCalculation.getSoil().setunitW(Double.parseDouble(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                mSoilCohesionFactor = (EditText) soildialog.findViewById(R.id.Soilcohesion);
                mSoilCohesionFactor.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sSoilCohesionFactor = mSoilCohesionFactor.getText().toString();
                        if (!sSoilCohesionFactor.matches(""))
                            mCalculation.getSoil().setC(Double.parseDouble(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                mSoilFrictionAngle = (EditText) soildialog.findViewById(R.id.Soilfricta);
                mSoilFrictionAngle.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        String sSoilFrictionAngle = mSoilFrictionAngle.getText().toString();
                        if (!sSoilFrictionAngle.matches(""))
                            mCalculation.getSoil().setfrictA(Integer.parseInt(charSequence.toString()));
                        mCallbacks.onCalculationUpdated(mCalculation);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                Button mCancelButton = (Button) soildialog.findViewById(R.id.soil_cancel);
                mCancelButton.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        mCalculation.setSoil(null);
                        soildialog.dismiss();

                    }

                });

                Button mSaveButton = (Button) soildialog.findViewById(R.id.soil_save);
                mSaveButton.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        mCalculation.getSoil().convertToMetric();
                        mSoils.add(mCalculation.getSoil());
                        SoilOutputList.add(mCalculation.getSoil().getName());
                        SoilLab.get(getActivity()).saveSoils();

                        soildialog.dismiss();

                    }
                });
                // Toasts corresponding to Questions in this popup
                ToastMaker(R.string.fricta_popup, PhiQuestion, v);
                ToastMaker(R.string.cohesion_popup, CohesionQuestion, v);
                ToastMaker(R.string.ws_popup, UnitWeightQuestion, v);

                soildialog.show();


            }

        });

        performCalculation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                DecimalFormat df = new DecimalFormat("#");
                populateCalculation();
                    mCalculation.imperialconversion();

                    double AnchCap = mCalculation.anchor_capacity(isimperial);
                    double tipover = mCalculation.tip_over_moment(isimperial);
                    dragValue.setText(df.format(AnchCap) + "     " + answerUnits);
                    momentValue.setText(df.format(tipover) + "     " + answerUnits);

                    if (AnchCap <= tipover) {
                        dragValue.setTextColor(Color.RED);
                        dragValue.setHighlightColor(Color.YELLOW);
                    } else {
                        momentValue.setTextColor(Color.RED);
                        momentValue.setHighlightColor(Color.YELLOW);
                    }
                    CalculationLab.get(getActivity()).saveCalculations();


            }

        });

 /*       mReportButton = (Button)v.findViewById(R.id.report_button);
        mReportButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                Pdf.maker(mCalculation);
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                     new String[]{});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MACS Report");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "This email sent by the MACS App");
                // Here my file name is shortcuts.pdf which i have stored in /res/raw folder
                Uri emailUri = Uri.parse("file://" + Environment.getExternalStorageDirectory().getPath()+ "/Report.pdf");
                emailIntent.putExtra(Intent.EXTRA_STREAM, emailUri);
                emailIntent.setType("application/pdf");
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));

            }
        });
*/
        setRetainInstance(true);
        return v;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_DATE){
            Date date = (Date) data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCalculation.setDate(date);
            mCallbacks.onCalculationUpdated(mCalculation);
            updateDate();
        }



    }

    // A Simple toast function, displays text by resource Id depending on what imagebutton was clicked
    void ToastMaker(final int display, ImageButton button, View view){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast thistoast = Toast.makeText(getActivity(),getResources().getString(display),Toast.LENGTH_LONG);
                //calls the class which allows us to display toasts for longer than 3.5 seconds
                ToastExpander.showFor(thistoast,5000);
            }
        });

    }

    @Override
    public void onPause(){
        super.onPause();
        CalculationLab.get(getActivity()).saveCalculations();
        VehicleLab.get(getActivity()).saveVehicles();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void staticVehicles(ArrayList<Vehicle> v){

        Vehicle ex1 = new Vehicle();
        ex1.setVehicleClass("Bulldozer");
        ex1.setType("Caterpillar D8");
        ex1.setWv(36199.93);
        ex1.setBladeW(3.255);
        ex1.setCg(3.51);
        ex1.setTrackL(3.255);
        ex1.setTrackW(0.62);

        Vehicle ex2 = new Vehicle();
        ex2.setVehicleClass("Excavator");
        ex2.setType("Caterpillar 320C");
        ex2.setWv(21821.93);
        ex2.setBladeW(1.55);
        ex2.setCg(7.75);
        ex2.setTrackL(3.72);
        ex2.setTrackW(0.62);

        if (!v.contains(ex1))
            v.add(ex1);
        if (!v.contains(ex2))
            v.add(ex2);


    }

    private void staticSoils(ArrayList<Soil> s){

        Soil a = new Soil();
        a.setC(0);
        a.setfrictA(25);
        a.setunitW(1522);
        a.setName("Uncompacted Loose Silt/Sand/Gravel");

        Soil b = new Soil();
        b.setC(0);
        b.setfrictA(30);
        b.setunitW(1762);
        b.setName("Uncompacted Lightly Compacted Silt/Sand/Gravel");

        Soil c = new Soil();
        c.setC(0);
        c.setfrictA(35);
        c.setunitW(2082);
        c.setName("Dense Compacted Silt/Sand/Gravel");

        Soil d = new Soil();
        d.setC(23.9);
        d.setfrictA(0);
        d.setunitW(1522);
        d.setName("Soft Clay");

        Soil e = new Soil();
        e.setC(47.88);
        e.setfrictA(0);
        e.setunitW(1522);
        e.setName("Firm Clay");

        Soil f = new Soil();
        f.setC(95.76);
        f.setfrictA(0);
        f.setunitW(1522);
        f.setName("Stiff Clay");


        Soil g = new Soil();
        g.setC(143.64);
        g.setfrictA(0);
        g.setunitW(1522);
        g.setName("Very Stiff Clay");

        Soil h = new Soil();
        h.setC(191.52);
        h.setfrictA(0);
        h.setunitW(1522);
        h.setName("Hard Clay");

        if (! s.contains(a))
            s.add(a);
        if (! s.contains(b))
            s.add(b);
        if (! s.contains(c))
             s.add(c);
        if (! s.contains(d))
            s.add(d);
        if (! s.contains(e))
            s.add(e);
        if (! s.contains(f))
            s.add(f);
        if (! s.contains(g))
            s.add(g);
        if (! s.contains(h))
            s.add(h);

    }

}
