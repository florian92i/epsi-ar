package com.example.ar_epsi;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    ArFragment arFragment;
    boolean shouldAddModel = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        assert arFragment != null;
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void placeObject(ArFragment arFragment, Anchor anchor, Uri uri) {
        ModelRenderable.builder()
                .setSource(arFragment.getContext(), uri)
                .build()
                .thenAccept(modelRenderable -> addNodeToScene(arFragment, anchor, modelRenderable))
                .exceptionally(throwable -> {
                            Toast.makeText(arFragment.getContext(), "Error:" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            return null;
                        }
                );
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        // on va boucler sur plusieurs frame pour pouvoir analyser et détecter notre image
        try {
            System.out.println(frame.getImageMetadata());
        } catch (Exception e) {
            e.printStackTrace();
        }


        Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : augmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {

                if (augmentedImage.getName().equals("rhino") && shouldAddModel) {
                    System.out.println("---------------------------");
                    try {

                        //augmentedImage
                        System.out.println("PASSE LA");
                        Image imageAcquire = frame.acquireCameraImage(); // ici faut passer la bonne taille d'image qui correpond à notre photo
                        //image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer()
                        //image.getPlanes()[2].getBuffer(), image.getWidth(), image.getHeight()

                        byte[] nv21 = YUV_420_888toNV(imageAcquire.getPlanes()[0].getBuffer(), imageAcquire.getPlanes()[0].getBuffer(), imageAcquire.getPlanes()[0].getBuffer(), false);
                        byte[] jpeg = NV21toJPEG(nv21, 300, 300, 100);
                        imageAcquire.close();
                        // ECRIRE DANS UN FICHIER
                        Bitmap bitmapImage = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, null);
                        createDirectoryAndSaveFile(bitmapImage, "toto.jpeg");
                        System.out.println("----------tail image------ ");
                        System.out.println(bitmapImage.getWidth());
                        System.out.println(bitmapImage.getHeight());
                        int pixel = bitmapImage.getPixel(12,35);
                        int redValue = Color.red(pixel);
                        int greenValue = Color.green(pixel);
                        int blueValue = Color.blue(pixel);
                        System.out.println("------------COLOR---------------");
                        System.out.println(redValue);
                        System.out.println(greenValue);
                        System.out.println(blueValue);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(augmentedImage.getExtentX());
                    System.out.println(augmentedImage.getExtentZ());
                    System.out.println(augmentedImage.getCenterPose());
                    System.out.println("---------------------------");
                    placeObject(arFragment, augmentedImage.createAnchor(augmentedImage.getCenterPose()), Uri.parse("Mesh_Rhinoceros.sfb"));
                    shouldAddModel = false;
                    // à voir pour ça pour rajouter des models
                }
            }
        }
    }

    public boolean setupAugmentedImagesDb(Config config, Session session) {
        AugmentedImageDatabase augmentedImageDatabase;
        Bitmap bitmap = loadAugmentedImage();
        if (bitmap == null) {
            return false;
        }

        augmentedImageDatabase = new AugmentedImageDatabase(session);
        augmentedImageDatabase.addImage("rhino", bitmap);
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    // permet de charger l'image de référence
    // error si on charge pas
    private Bitmap loadAugmentedImage() {
        try (InputStream is = getAssets().open("imageTestBlanc.png")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e("ImageLoad", "IO Exception", e);
        }

        return null;
    }

    private void addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    // convertisseur d'image
    public static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    // nv12: true = NV12, false = NV21
    public static byte[] YUV_420_888toNV(ByteBuffer yBuffer, ByteBuffer uBuffer, ByteBuffer vBuffer, boolean nv12) {
        byte[] nv;

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv = new byte[ySize + uSize + vSize];

        yBuffer.get(nv, 0, ySize);
        if (nv12) {//U and V are swapped
            vBuffer.get(nv, ySize, vSize);
            uBuffer.get(nv, ySize + vSize, uSize);
        } else {
            uBuffer.get(nv, ySize , uSize);
            vBuffer.get(nv, ySize + uSize, vSize);
        }
        return nv;
    }

    private void createDirectoryAndSaveFile(Bitmap imageToSave, String fileName) {


        System.out.println("------------------- lka ---");
        System.out.println(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
        ));

        File direct = new File(    Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
        ),
                "Camera");

        System.out.println(direct.mkdirs());

        if (!direct.exists()) {
            File wallpaperDirectory = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
            )));
            wallpaperDirectory.mkdirs();
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
        ), fileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
