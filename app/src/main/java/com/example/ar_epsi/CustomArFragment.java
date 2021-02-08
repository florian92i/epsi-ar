package com.example.ar_epsi;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Objects;

public class CustomArFragment extends ArFragment {
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    @Override
    protected Config getSessionConfiguration(Session session) {
        getPlaneDiscoveryController().setInstructionView(null);
        Config config = new Config(session);
        //config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        config.setFocusMode(Config.FocusMode.AUTO);
        session.configure(config);
        getArSceneView().setupSession(session);

        ActivityCompat.requestPermissions(
                Objects.requireNonNull(getActivity()),
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
        );
        if ((((MainActivity) Objects.requireNonNull(getActivity())).setupAugmentedImagesDb(config, session))) {
            Log.d("SetupAugImgDb", "Success");
        } else {
            Log.e("SetupAugImgDb","Faliure setting up db");
        }

        return config;
    }


}