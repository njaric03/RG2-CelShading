package xyz.marsavic.gfxlab.graphics3d.raytracers;

import xyz.marsavic.functions.F0;
import xyz.marsavic.functions.F1;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.graphics3d.*;


public class RayTracerDepth extends RayTracer {

	private static final double EPSILON = 1e-9;
	private final double farPlane;


	public RayTracerDepth(F0<F1<Scene, Double>> ffSceneT, double farPlane) {
		super(ffSceneT);
		this.farPlane = farPlane;
	}

	@Override
	protected Color sample(Scene scene, Ray ray) {
		Hit hit = scene.solid().firstHit(ray, EPSILON);
		double t = hit.t();
		double d = (t == Double.POSITIVE_INFINITY) ? 1.0 : Math.min(t / farPlane, 1.0);
		return Color.gray(d);
	}

}
