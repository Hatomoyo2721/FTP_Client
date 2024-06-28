package com.example.ftp_client;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ftp_client.ui.activity.HistoryActivity;
import com.example.ftp_client.ui.connection.AddConnectionFragment;
import com.example.ftp_client.ui.connection.ConnectionListFragment;
import com.example.ftp_client.ui.connection.ConnectionModel;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AddConnectionFragment.OnConnectionAddedListener {

    private Toolbar toolbar;
    private static final String CONNECTION_LIST_FRAGMENT_TAG = "CONNECTION_LIST_FRAGMENT";
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof AddConnectionFragment) {
                setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // Load the ConnectionListFragment initially
        switchToFragment(new ConnectionListFragment());

        // Load the ConnectionListFragment initially
//        ConnectionListFragment connectionListFragment = new ConnectionListFragment();
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.fragment_container, connectionListFragment, CONNECTION_LIST_FRAGMENT_TAG)
//                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Hide the history menu item if the ConnectionListFragment is not visible
        ConnectionListFragment listFragment = (ConnectionListFragment)
                getSupportFragmentManager().findFragmentByTag(CONNECTION_LIST_FRAGMENT_TAG);
        // Uncomment if you want to control visibility of menu items based on the fragment
//        if (listFragment != null && listFragment.isVisible()) {
//            menu.findItem(R.id.action_history).setVisible(false);
//        } else {
//            menu.findItem(R.id.action_history).setVisible(true);
//        }
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_history:
                Intent intent = new Intent(this, HistoryActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionAdded(ConnectionModel connection) {
        // Get the current fragment
        ConnectionListFragment listFragment = (ConnectionListFragment)
                getSupportFragmentManager().findFragmentByTag(CONNECTION_LIST_FRAGMENT_TAG);
        if (listFragment != null) {
            listFragment.addConnection(connection);
        }
    }

    private void switchToFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void setCustomAnimations(int enter, int exit, int popEnter, int popExit) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(enter, exit, popEnter, popExit)
                .commit();
    }
}
