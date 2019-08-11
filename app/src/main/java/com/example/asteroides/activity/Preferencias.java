package com.example.asteroides.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.example.asteroides.R;

public class Preferencias extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencias);

    }
}
