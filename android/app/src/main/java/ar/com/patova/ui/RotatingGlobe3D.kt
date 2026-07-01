package ar.com.patova.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.model.rememberModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader

/**
 * A real-time 3D globe (Filament/SceneView) that rotates continuously in a loop.
 * Replaces the static Icons.Rounded.Language glyph in the "Red" (Network) screen header.
 *
 * The model (assets/models/earth.glb) has a baked "Spin" rotation animation
 * (0deg -> 360deg around the Y axis, 8s per turn) that ModelNode autoplays and loops.
 *
 * - surfaceType = TextureView so it composites correctly inside a scrolling Column
 *   (a raw SurfaceView would punch through / misbehave while scrolling).
 * - isOpaque = false so the sphere renders over the existing Compose background
 *   (the 64dp PremiumBlueBg rounded box) instead of drawing its own backdrop.
 * - cameraManipulator/onGestureListener = null so the tiny icon never intercepts
 *   drag/tap gestures meant for the screen around it; the only motion is the
 *   model's own looping animation.
 */
@Composable
fun RotatingGlobe3D(modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    SceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        surfaceType = SurfaceType.TextureView,
        isOpaque = false,
        cameraManipulator = null,
        onGestureListener = null
    ) {
        rememberModelInstance(modelLoader, "models/earth.glb")?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f,
                autoAnimate = true,
                animationLoop = true
            )
        }
    }
}
