package com.reachndo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconCompat;
import com.faizmalkani.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.service.Event;
import com.service.Location;
import com.service.LocationService;
import com.service.MessageEvent;
import com.service.NotificationEvent;
import com.service.SaveAndLoad;
import com.service.Singleton;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainMenu extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private CharSequence mTitle;

    private static ListView listView;
    private static EventListAdapter listAdapter;
    private static MaterialMenuIconCompat materialMenu;

    private static FloatingActionButton mFab;

    public static TextView warningLocMainText;
    public static TextView warningLocSubText;

    public static TextView warningEvnMainText;
    public static TextView warningEvnSubText;

    private static AdapterView.OnItemClickListener clickListener;

    private static MainMenu instance;

    private static final int REQUEST_PLACE_PICKER = 1;

    public static MainMenu getInstance(){
        return instance;
    }

    public EventListAdapter getEventAdapter(){
        return listAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        startService(new Intent(this, LocationService.class));

        //Setting style according to API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTheme(R.style.MaterialDesign);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.DarkMaterialPurple));
        }

        try {
            SaveAndLoad.loadInfo(this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        materialMenu = new MaterialMenuIconCompat(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);


        setContentView(R.layout.activity_main_menu);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        listAdapter = new EventListAdapter(getBaseContext(), new ArrayList<Event>());

        clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        };

        makeActionOverflowMenuShown();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();

    }

    public void onSectionAttached(int index) {
        if(index > 0) {
            mTitle = Singleton.getLocations().get(index - 1).getName();
            notifyListView(index - 1);
        }else {
            mTitle = getResources().getString(R.string.no_locations);
            notifyListView(-1);
        }
    }

    public void updateActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            updateActionBar();
            materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
            return true;
        }else{
            materialMenu.animateState(MaterialMenuDrawable.IconState.ARROW);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainMenu.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id  == android.R.id.home) {
            if (mNavigationDrawerFragment.isDrawerOpen()) {
                materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
            }else{
                materialMenu.animateState(MaterialMenuDrawable.IconState.ARROW);
            }
        }


        return super.onOptionsItemSelected(item);
    }

    public void removeLocation(int position) {
        Singleton.getLocations().remove(position);

        NavigationDrawerFragment nd = NavigationDrawerFragment.getInstance();
        if(position == nd.getCurrentSelection()){
            if(Singleton.getLocations().size() != 0){
                nd.selectItem(0);
            }else{
                nd.selectItem(-1);
            }
        }else if(position < nd.getCurrentSelection()){
            nd.selectItem(nd.getCurrentSelection() - 1);
        }
        try {
            SaveAndLoad.saveInfo(MainMenu.this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static PlaceholderFragment fragment;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_menu, container, false);
            showFloatingActionButton(rootView);
            listView = (ListView) rootView.findViewById(R.id.list);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(clickListener);
            warningLocMainText = (TextView) rootView.findViewById(R.id.txtLocView);
            warningLocSubText = (TextView) rootView.findViewById(R.id.subTxtLocView);
            warningEvnMainText = (TextView) rootView.findViewById(R.id.txtEvnView);
            warningEvnSubText = (TextView) rootView.findViewById(R.id.subTxtEvnView);
            NavigationDrawerFragment nd = NavigationDrawerFragment.getInstance();
            if(Singleton.getLocations().size() == 0){
                warningLocMainText.setVisibility(View.VISIBLE);
                warningLocSubText.setVisibility(View.VISIBLE);
            }else if(Singleton.getLocations().get(nd.getCurrentSelection()).getEventsIn().size() == 0 &&
                    Singleton.getLocations().get(nd.getCurrentSelection()).getEventsOut().size() == 0 ){
                listAdapter.clear();
                listAdapter.notifyDataSetChanged();
                warningEvnMainText.setVisibility(View.VISIBLE);
                warningEvnSubText.setVisibility(View.VISIBLE);
            }
            return rootView;
        }


        public void showFloatingActionButton(View v) {
            mFab = (FloatingActionButton) v.findViewById(R.id.fab);
            if(mFab != null) {
                mFab.setDrawable(getResources().getDrawable(R.drawable.ic_plusicon));
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {

                            PlacePicker.IntentBuilder intentBuilder;
                            intentBuilder = new PlacePicker.IntentBuilder();
                            Intent intent = intentBuilder.build(getContext());
                            startActivityForResult(intent, REQUEST_PLACE_PICKER);

                        } catch (GooglePlayServicesRepairableException e) {
                            Log.d("PlacesAPI Demo", "GooglePlayServicesRepairableException thrown");
                        } catch (GooglePlayServicesNotAvailableException e) {
                            Log.d("PlacesAPI Demo", "GooglePlayServicesNotAvailableException thrown");
                        }
                    }
                });
                mFab.listenTo(listView);
            }
            else
                Log.d("Debug", "Error in mFab (NULL)");

        }

        @Override
        public void onActivityResult(int requestCode,
                                        int resultCode, Intent data) {

            if (requestCode == REQUEST_PLACE_PICKER
                    && resultCode == Activity.RESULT_OK) {


                // The user has selected a place. Extract the name and address.
                final Place place = PlacePicker.getPlace(data, getContext());
                showLocationNamePicker(place);

            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        private void showLocationNamePicker(final Place selectedLocation) {
            final MaterialDialog locationPicker =
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.location_picker_title)
                            .customView(R.layout.location_picker_layout, true)
                            .positiveText(android.R.string.ok)
                            .autoDismiss(false)
                            .negativeText(android.R.string.cancel)
                            .cancelable(false)
                            .show();

            View positive = locationPicker.getActionButton(DialogAction.POSITIVE);
            positive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText radius = ((EditText)locationPicker.getView().findViewById(R.id.radiusText));
                    EditText name = ((EditText)locationPicker.getView().findViewById(R.id.nameTxt));
                    if(name.getText().length() == 0){
                        new MaterialDialog.Builder(getContext())
                                .title(R.string.location_picker_warning_title)
                                .content(R.string.location_picker_warning_name_content)
                                .neutralText(android.R.string.ok)
                                .show();
                        return;
                    }else if(radius.getText().length() == 0 || Integer.parseInt(radius.getText().toString()) == 0){
                        new MaterialDialog.Builder(getContext())
                                .title(R.string.location_picker_warning_title)
                                .content(R.string.location_picker_warning_radius_content)
                                .neutralText(android.R.string.ok)
                                .show();
                        return;
                    }

                    Log.d("Debug Location - add", Singleton.getLocations().size() + "");

                    ArrayList<Location> temp = Singleton.getLocations();
                    Location newLocation = new Location(selectedLocation.getLatLng().latitude,
                            selectedLocation.getLatLng().longitude,
                            name.getText().toString(),
                            Double.parseDouble(radius.getText().toString()), false);
                    temp.add(newLocation);
                    Singleton.setLocations(temp);
                    try {
                        SaveAndLoad.saveInfo(getContext());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.d("Debug Location added", Singleton.getLocations().size() + "");
                    NavigationDrawerFragment nd = NavigationDrawerFragment.getInstance();
                    nd.setLocationAdapter((ArrayList<Location>)Singleton.getLocations());
                    nd.selectItem(Singleton.getLocations().size() - 1);
                    MainMenu menu = MainMenu.getInstance();
                    menu.updateTitle(name.getText().toString());
                    locationPicker.dismiss();
                }
            });

            View negative = locationPicker.getActionButton(DialogAction.NEGATIVE);
            negative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    locationPicker.dismiss();
                }
            });

        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            materialMenu.onSaveInstanceState(outState);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainMenu) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public void updateTitle(String s) {
        mTitle = s;
        updateActionBar();
    }

    private void makeActionOverflowMenuShown() {
        //devices with hardware menu button (e.g. Samsung Note) don't show action overflow menu
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Log.d("Error getting overflow", e.getLocalizedMessage());
        }
    }

    public void notifyListView(final
                               int index){

        if(index == -1){
            listAdapter.clear();
            listAdapter.notifyDataSetChanged();
            return;
        }

        if(warningEvnMainText != null && warningEvnSubText != null &&
                (Singleton.getLocations().get(index).getEventsIn().size() != 0 ||
                        Singleton.getLocations().get(index).getEventsOut().size() != 0)){
            warningEvnMainText.setVisibility(View.INVISIBLE);
            warningEvnSubText.setVisibility(View.INVISIBLE);
        }

        listAdapter.clear();
        if(Singleton.getLocations().get(index).getEventsIn().size() != 0) {
            listAdapter.add(new Event(getResources().getString(R.string.in)));
            listAdapter.addAll(Singleton.getLocations().get(index).getEventsIn());
        }

        if(Singleton.getLocations().get(index).getEventsOut().size() != 0) {
            listAdapter.add(new Event(getResources().getString(R.string.out)));
            listAdapter.addAll(Singleton.getLocations().get(index).getEventsOut());
        }

        clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showDialog(listAdapter.getItem(i), index);
            }
        };

        listAdapter.notifyDataSetChanged();
    }

    public void showDialog(final Event event, final int index) {
        String title = "";
        String content = "";
        switch (event.getType()) {
            case MESSAGE:
                title = getResources().getString(R.string.sms_dialog_title);
                content = event.getDescription() + "\n\n" + getResources().getString(R.string.sms_dialog_text_info)
                        + " " + ((MessageEvent)event).getTextMessage();
                break;
            case NOTIFICATION:
                title = event.getName();
                content = getResources().getString(R.string.sms_dialog_text_info) + " " + ((NotificationEvent)event).getDescription();
                break;
            default:
                title = event.getName();
                content = event.getDescription();
                break;
        }
        final MaterialDialog infoDialog = new MaterialDialog.Builder(MainMenu.this)
                .title(title)
                .content(content)
                .neutralText(android.R.string.ok)
                .negativeText(R.string.action_delete)
                .show();

        View negative = infoDialog.getActionButton(DialogAction.NEGATIVE);
        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Singleton.getLocations().get(index).removeEvent(event);

                if(warningEvnMainText != null && warningEvnSubText != null &&
                        (Singleton.getLocations().get(index).getEventsIn().size() == 0 &&
                                Singleton.getLocations().get(index).getEventsOut().size() == 0)){
                    warningEvnMainText.setVisibility(View.VISIBLE);
                    warningEvnSubText.setVisibility(View.VISIBLE);
                }

                try {
                    SaveAndLoad.saveInfo(MainMenu.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                infoDialog.dismiss();
                notifyListView(index);
            }
        });
    }

}
