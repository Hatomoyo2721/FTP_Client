package com.example.ftp_client.ui.connection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ftp_client.R;

public class AddExistingConnectionFragment extends Fragment {

    private ProgressBar loadingProgressBar;
    private TextView loadingTextView;
    private EditText editTextIPAddress;
    private EditText editTextPort;
    private EditText editTextUsername;
    private Button buttonSave;
    private Button buttonBack;
    private RelativeLayout loadingScreenLayout;

    public AddExistingConnectionFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_existing_connection, container, false);

        editTextIPAddress = rootView.findViewById(R.id.editTextIPAddress);
        editTextPort = rootView.findViewById(R.id.editTextPort);
        editTextUsername = rootView.findViewById(R.id.editTextUsername);
        buttonSave = rootView.findViewById(R.id.buttonSave);
        loadingScreenLayout = rootView.findViewById(R.id.loadingScreenLayoutExisting);
        loadingProgressBar = rootView.findViewById(R.id.loadingProgressBarExisting);
        loadingTextView = rootView.findViewById(R.id.loadingTextViewExisting);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExistConnection();
            }
        });

        return rootView;
    }

    private void saveExistConnection() {
    }
}
