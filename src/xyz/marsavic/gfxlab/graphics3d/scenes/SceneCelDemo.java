package xyz.marsavic.gfxlab.graphics3d.scenes;

import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.*;
import xyz.marsavic.gfxlab.aggregation.AggregatorOneAhead;
import xyz.marsavic.gfxlab.aggregation.EAggregator;
import xyz.marsavic.gfxlab.aggregation.RenderOnce;
import xyz.marsavic.gfxlab.graphics3d.*;
import xyz.marsavic.gfxlab.graphics3d.cameras.Perspective;
import xyz.marsavic.gfxlab.graphics3d.cameras.TransformedCamera;
import xyz.marsavic.gfxlab.graphics3d.raytracers.RayTracerDepth;
import xyz.marsavic.gfxlab.graphics3d.raytracers.RayTracerNormal;
import xyz.marsavic.gfxlab.graphics3d.raytracers.RayTracerSimple;
import xyz.marsavic.gfxlab.graphics3d.solids.Ball;
import xyz.marsavic.gfxlab.graphics3d.solids.Box;
import xyz.marsavic.gfxlab.graphics3d.solids.Group;
import xyz.marsavic.gfxlab.graphics3d.solids.HalfSpace;
import xyz.marsavic.gfxlab.tonemapping.CelShader3;
import xyz.marsavic.gfxlab.tonemapping.matrixcolor_to_colortransforms.AutoSoft;
import xyz.marsavic.reactions.elements.ElementF;
import xyz.marsavic.utils.Hash;

import java.util.List;

import static xyz.marsavic.gfxlab.Vec3.*;
import static xyz.marsavic.reactions.elements.Elements.*;


public record SceneCelDemo() implements FFSceneT {

	@Override
	public Solid solid() {
		Ball sphere = Ball.cr(xyz(-1.5, -0.5, 6.2), 1.4, Material.matte(Color.hsb(0.0, 0.7, 0.9)));

		Solid cube = Box.$.pq(xyz(-1, -1, -1), xyz(1, 1, 1))
				.material(Material.matte(Color.hsb(0.5, 0.6, 0.9)))
				.transformed(Affine3.chain(
						Affine3.rotationAboutY(0.1),
						Affine3.rotationAboutX(0.05),
						Affine3.translation(xyz(0.7, -0.5, 5.4))
				));

		HalfSpace floor = HalfSpace.pn(xyz(0, -2, 0), xyz(0, 1, 0),
				uv -> Material.matte(uv.add(Vector.xy(0.05, 0.05)).mod().min() < 0.1 ? 0.5 : 0.9));

		HalfSpace backWall = HalfSpace.pn(xyz(0, 0, 11), xyz(0, 0, -1),
				Material.matte(Color.hsb(0.6, 0.2, 0.85)));

		return Group.of(sphere, cube, floor, backWall);
	}


	@Override
	public List<Light> lights() {
		return List.of(
				Light.pc(xyz(-4, 5, 1), Color.hsb(0.1, 0.2, 10.0)),
				Light.pc(xyz(7, 6, -2), Color.hsb(0.6, 0.3, 7.0)),
				Light.pc(ZERO, Color.gray(0.2))
		);
	}


	@Override
	public Camera camera() {
		return new TransformedCamera(
				new Perspective(1.0 / 3),
				Affine3.IDENTITY.then(Affine3.translation(xyz(0, 0.4, -10)))
		);
	}


	// ================================================================================================================

	public static ElementF<Animation> setup() {
		var sceneNode = e(SceneCelDemo.class);

		var supersample = e(2);
		var sizeOut = e(xyz(1, 640, 640));
		var sizeHi = e((Vec3 so, Integer s) -> xyz(so.x(), so.y() * s, so.z() * s), sizeOut, supersample);

		return e(CelShader3.class,
				new EAggregator(
						e(AggregatorOneAhead::new),
						e(RayTracerSimple.class, sceneNode, e(1)),
						e(TransformationFromSize.ToGeometricT0_.class),
						sizeHi,
						e(false),
						e(false),
						e(Hash.class, e(0x8EE6B0C4E02CA7B2L))
				),
				new RenderOnce(
						e(RayTracerNormal.class, sceneNode),
						e(TransformationFromSize.ToGeometricT0_.class),
						sizeHi
				),
				new RenderOnce(
						e(RayTracerDepth.class, sceneNode, e(15.0)),
						e(TransformationFromSize.ToGeometricT0_.class),
						sizeHi
				),
				e(AutoSoft.class, e(0x1p-4), e(1.0)),
				supersample,    // supersample
				e(4),    // lightnessBands
				e(3),    // chromaBands
				e(12),   // hueBands
				e(0.3),  // edgeThreshold
				e(3.0)   // depthEdgeScale
		);
	}

}
