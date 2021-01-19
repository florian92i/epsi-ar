package com.example.ar_epsi;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    ArFragment arFragment;
    ModelRenderable lampPostRenderable;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        /*
        Ensuite , nous utilisons la classe ModelRenderable pour créer notre modèle.
        Avec l'aide de la méthode setSource, nous créons charger notre modèle à partir du. sfb qui a été généré lorsque nous avons importé les actifs,
        la méthode thenAccept reçoit le modèle une fois qu'il est construit et nous définissons le modèle chargé sur notre lampPostRenderable. Piano.gltf
         */
        System.out.println("Le message ici ---------------------------");
        System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
        ModelRenderable.builder()
                .setSource(this, Uri.parse("Mesh_Rhinoceros.sfb"))
                .build()
                .thenAccept(renderable -> lampPostRenderable = renderable)
                .exceptionally(throwable -> {
                    Toast toast =
                            Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return null;
                });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitresult, Plane plane, MotionEvent motionevent) -> {
                    if (lampPostRenderable == null){
                        return;
                    }

                    Anchor anchor = hitresult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    TransformableNode lamp = new TransformableNode(arFragment.getTransformationSystem());
                    lamp.setParent(anchorNode);
                    lamp.setRenderable(lampPostRenderable);
                    lamp.select();

/*
Scène : C'est l'endroit où tous vos objets 3D seront rendus. Cette scène est hébergée par le fragment AR que nous avons inclus dans la mise en page. Un nœud d'ancrage est attaché à cet écran qui agit comme la racine de l'arbre et tous les autres objets sont rendus comme ses objets.
HitResult : C'est une ligne imaginaire (ou un rayon) venant de l'infini qui donne le point d'intersection de lui-même avec un objet du monde réel.
Ancre : Une ancre est un emplacement et une orientation fixes dans le monde réel. Il peut être compris comme les coordonnées x, y, z dans l'espace 3D. Vous pouvez y obtenir des informations sur la publication d'une ancre. La pose est la position et l'orientation de l'objet dans la scène. Ceci est utilisé pour transformer l'espace de coordonnées local de l'objet en espace de coordonnées réel.
AnchorNode: C'est le nœud qui se positionne automatiquement dans le monde. C'est le premier nœud qui est défini lorsque l'avion est détecté.
TransformableNode : C'est un nœud avec lequel interagir. Il peut être déplacé, mis à l'échelle, tourné et bien plus encore. Dans cet exemple, nous pouvons mettre à l'échelle la lampe et la faire pivoter. D'où le nom Transformable.
 */
                }
        );

    }


    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }


}

