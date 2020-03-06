package com.example.admin.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.LocaleList;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.TracingConfig;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import com.google.firebase.database.ValueEventListener;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class ListOnline extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    DatabaseReference onlineRef, currentUserRef, counterRef, locations;
    FirebaseRecyclerAdapter<User, ListOnlineViewHolder> adapter;


    RecyclerView list_online;
    RecyclerView.LayoutManager layoutManager;

    private static final int MY_PERMISSION_REQUEST_CODE = 7171;
    private static final int PLAY_SERVICES_RES_REQUEST = 7172;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;


    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISTANCE = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_online);

        list_online = (RecyclerView) findViewById(R.id.listOnline);
        list_online.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        list_online.setLayoutManager(layoutManager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar.setTitle("TRACKER");
        setSupportActionBar(toolbar);
        locations = FirebaseDatabase.getInstance().getReference("Locations");
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");
        counterRef = FirebaseDatabase.getInstance().getReference("lastOnline");//new child
        currentUserRef = FirebaseDatabase.getInstance().getReference("lastOnline")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{

                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION


            }, MY_PERMISSION_REQUEST_CODE);
        } else

        {

            if (checkPlayService()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();

            }

        }


        setupSystem();

        update();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayService()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();


                    }

                }

            }
            break;
        }

    }

    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                locations.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(new Tracking(FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                String.valueOf(mLastLocation.getLatitude()),
                                String.valueOf(mLastLocation.getLongitude())));

            }
            
            else 
            {
                //Toast.makeText(this,"could not get location ",Toast.LENGTH_SHORT).show();
                Log.d("TEST","COULDNT LOAD LOCATION");
                
            }
        }

    
    
    
    
    private void createLocationRequest() {
        mLocationRequest= new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(DISTANCE);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    private void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();

    }

    private boolean checkPlayService() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RES_REQUEST).show();
            } else {
                Toast.makeText(this, "This device not supported", Toast.LENGTH_SHORT).show();
                finish();


            }
            return false;

        }
        return true;

    }
    private void update() {
        FirebaseRecyclerOptions<User> options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(counterRef,User.class)
                        .build();
    adapter = new FirebaseRecyclerAdapter<User, ListOnlineViewHolder>(options) {


        @Override
        protected void onBindViewHolder(@NonNull ListOnlineViewHolder holder, int position, @NonNull final User model) {
            if (model.getEmail().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail()))

            {   holder.txtEmail.setText(model.getEmail()+"(me)");}
            else

            { holder.txtEmail.setText(model.getEmail());}

            holder.itemClickListener=new ItemClickListener() {
                @Override
                public void onclick(View view, int position) {

                    if(!model.getEmail().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail()))
                    {
                        Intent map= new Intent(ListOnline.this,MapTracking.class);
                        map.putExtra("email",model.getEmail());
                        map.putExtra("lat",mLastLocation.getLatitude());
                        map.putExtra("lng",mLastLocation.getLongitude());
                        startActivity(map);
                    }

                }
            };
        }

        @NonNull
        @Override
        public ListOnlineViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            // layout called R.layout.message for each item
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.user_layout, viewGroup, false);

            return new ListOnlineViewHolder(view);
        }
    };
        adapter.notifyDataSetChanged();
        list_online.setAdapter(adapter);
    }
   /* private void updateList() {
        adapter=new FirebaseRecyclerAdapter<User, ListOnlineViewHolder>(

                User.class,
                R.layout.user_layout,
                ListOnlineViewHolder.class,
                counterRef

        )
        {
            @Override
            protected void populateViewHolder(ListOnlineViewHolder viewHolder, final User model, int position)
            {
                if (model.getEmail().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail()))

                    viewHolder.txtEmail.setText(model.getEmail()+"(me)");
                else

                viewHolder.txtEmail.setText(model.getEmail());

                viewHolder.itemClickListener=new ItemClickListener() {
                    @Override
                    public void onclick(View view, int position) {

                        if(!model.getEmail().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail()))
                        {
                            Intent map= new Intent(ListOnline.this,MapTracking.class);
                            map.putExtra("email",model.getEmail());
                            map.putExtra("lat",mLastLocation.getLatitude());
                            map.putExtra("lng",mLastLocation.getLongitude());
                            startActivity(map);
                        }

                    }
                };
            }
        };

        adapter.notifyDataSetChanged();
        list_online.setAdapter(adapter);


    }*/

    private void setupSystem() {

        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue(Boolean.class))
                {
                    currentUserRef.onDisconnect().removeValue();
                    counterRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .setValue(new User(FirebaseAuth.getInstance().getCurrentUser().getEmail(),"Online"));

   adapter.notifyDataSetChanged();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        counterRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot:dataSnapshot.getChildren())
                {
                    User user = postSnapshot.getValue(User.class);
                    Log.d("LOG",""+user.getEmail()+"is"+user.getStatus());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.action_join:
                counterRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(new User(FirebaseAuth.getInstance().getCurrentUser().getEmail(),"Online"));
break;
            case R.id.action_logout:
                currentUserRef.removeValue();

                break;

        }
               return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
mLastLocation= location;
displayLocation();
    }




    @Override
    public void onConnected(@Nullable Bundle bundle) {
     displayLocation();
     startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient!= null)
        {
            mGoogleApiClient.connect();

        }
    }

    @Override
    protected void onStop() {
        if(mGoogleApiClient !=null)
            mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayService();
    }
}


