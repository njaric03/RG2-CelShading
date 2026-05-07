package xyz.marsavic.gfxlab.graphics3d.raytracers;

import xyz.marsavic.functions.F0;
import xyz.marsavic.functions.F1;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.gfxlab.graphics3d.*;


public class RayTracerNormal extends RayTracer {

	private static final double EPSILON = 1e-9;


	public RayTracerNormal(F0<F1<Scene, Double>> ffSceneT) {
		super(ffSceneT);
	}

	@Override
	protected Color sample(Scene scene, Ray ray) {
		Hit hit = scene.solid().firstHit(ray, EPSILON);
		Vec3 n = (hit.t() == Double.POSITIVE_INFINITY) ? Vec3.ZERO : hit.n_();
		return Color.rgb((n.x() + 1) * 0.5, (n.y() + 1) * 0.5, (n.z() + 1) * 0.5);
	}

}
