package com.example.ar_epsi;

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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    ArFragment arFragment;
    boolean shouldAddModel = true;

    private void register(Runnable r) {
        findViewById(R.id.button).setOnClickListener(v -> r.run());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        assert arFragment != null;
        register(()-> {
            try {
                // image naming and path  to include sd card  appending name you choose for file
                String mPath = Environment.getExternalStorageDirectory().toString() + "/" + "toto" + ".jpg";

                // create bitmap screen capture
                View v1 = getWindow().getDecorView().getRootView();
                v1.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
                v1.setDrawingCacheEnabled(false);

                File imageFile = new File(mPath);

                FileOutputStream outputStream = new FileOutputStream(imageFile);
                int quality = 20;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();

            } catch (Throwable e) {
                // Several error may come out with file handling or DOM
                System.out.println("----------- anchor.getPose(); ----------------");
                System.out.println(e);
                e.printStackTrace();
            }
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("image/jpg");
            String shareSub = "Partager votre photo";
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, "body text");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(Environment.getExternalStorageDirectory().toString() + "/" + "toto" + ".jpg"));
            startActivity(Intent.createChooser(sharingIntent, "Share using"));
        });
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void placeObject(ArFragment arFragment, Anchor anchor, Uri uri) {
        ModelRenderable.builder()
                .setSource(arFragment.getContext(), uri)
                .build()
                .thenAccept(modelRenderable -> {
                    System.out.println("----------- anchor.getPose(); ----------------");
                    System.out.println(anchor.getPose());
                    System.out.println("----------- COLOR ----------------");
                    ModelRenderable blueSphereRenderable = modelRenderable.makeCopy();

                    blueSphereRenderable.getMaterial(0).setFloat3("zone1Color", 1.00f, 1.00f, 1.00f);
                    blueSphereRenderable.getMaterial(1).setFloat3("zone1Color", 0.00f, 0.00f, 0.00f);
                    blueSphereRenderable.getMaterial(2).setFloat3("zone1Color", 1.00f, 1.00f, 1.00f);
                    blueSphereRenderable.getMaterial(4).setFloat3("zone1Color", 0.00f, 0.00f, 0.00f);
                    blueSphereRenderable.getMaterial(5).setFloat3("zone1Color", 1.00f, 1.00f, 1.00f);
                    addNodeToScene(arFragment, anchor, blueSphereRenderable);
                })
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
                        System.out.println("X");
                        System.out.println(augmentedImage.getExtentX());
                        System.out.println("Z");
                        System.out.println(augmentedImage.getExtentZ());
                        System.out.println("Center ^pose");
                        System.out.println(Arrays.toString(augmentedImage.getCenterPose().getXAxis()));
                        System.out.println(Arrays.toString(augmentedImage.getCenterPose().getYAxis()));
                        byte[] nv21 = convertYUV420ToN21(imageAcquire);
                        byte[] jpeg = convertN21ToJpeg(nv21, 640, 480);
                        // width X = 640
                        // heigh Y = 480
                        imageAcquire.close();
                        // ECRIRE DANS UN FICHIER
                        Bitmap bitmapImage = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, null);
                        System.out.println("GetDominant Color");
                        getColorImg(bitmapImage, 320, 240);
                        getColorImg(bitmapImage, 200, 150);

                        createDirectoryAndSaveFile(bitmapImage, "toto.jpeg");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    placeObject(arFragment, augmentedImage.createAnchor(augmentedImage.getCenterPose()), Uri.parse("Rhinoceros.sfb"));
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
        try (InputStream is = getAssets().open("test.png")) {
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
    private byte[] convertN21ToJpeg(byte[] bytesN21, int w, int h) {
        byte[] rez = new byte[0];

        YuvImage yuv_image = new YuvImage(bytesN21, ImageFormat.NV21, w, h, null);
        Rect rect = new Rect(0, 0, w, h);
        ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
        yuv_image.compressToJpeg(rect, 100, output_stream);
        rez = output_stream.toByteArray();

        return rez;
    }

    private byte[] convertYUV420ToN21(Image imgYUV420) {
        byte[] rez = new byte[0];

        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        rez = new byte[buffer0_size + buffer2_size];

        buffer0.get(rez, 0, buffer0_size);
        buffer2.get(rez, buffer0_size, buffer2_size);

        return rez;
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

    public static void getColorImg(Bitmap bitmap, int x, int y) {
        int pixel = bitmap.getPixel(x,y);
        int redValue = Color.red(pixel);
        int greenValue = Color.green(pixel);
        int blueValue = Color.blue(pixel);
        System.out.println("------------COLOR---------------");
        System.out.println(redValue);
        System.out.println(greenValue);
        System.out.println(blueValue);
    }
}
