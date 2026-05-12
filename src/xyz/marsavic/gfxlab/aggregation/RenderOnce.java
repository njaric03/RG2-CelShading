package xyz.marsavic.gfxlab.aggregation;

import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.*;
import xyz.marsavic.reactions.Event;
import xyz.marsavic.reactions.elements.ElementSingleOutput;
import xyz.marsavic.reactions.elements.HasOutput;
import xyz.marsavic.resources.Rr;

public class RenderOnce extends ElementSingleOutput<F1<Rr<Matrix<Color>>, Integer>> {

	public final Input<ColorFunction3> inColorFunction;
	public final Input<TransformationFromSize> inTransformationFromSize;
	public final Input<Vec3> inSize;


	public RenderOnce(
			HasOutput<ColorFunction3> outColorFunction,
			HasOutput<TransformationFromSize> outTransformationFromSize,
			HasOutput<Vec3> outSize
	) {
		super("RenderOnce");
		inColorFunction          = new Input<>("colorFunction",          ColorFunction3.class,          outColorFunction);
		inTransformationFromSize = new Input<>("transformationFromSize", TransformationFromSize.class,  outTransformationFromSize);
		inSize                   = new Input<>("size",                   Vec3.class,                    outSize);
	}


	@Override
	protected <T> void onInputChanged(Input<T> input, Event event) {
		outputs().forEach(Output::fireInvalidated);
	}


	@Override
	public F1<Rr<Matrix<Color>>, Integer> result() {
		return iFrame -> render(iFrame);
	}


	private Rr<Matrix<Color>> render(int iFrame) {
		Vec3 size = inSize.get();
		Vector sizeFrame = size.p12();
		int w = sizeFrame.xInt();

		TransformedColorFunction3 tcf3 = new TransformedColorFunction3(
				inColorFunction.get(),
				inTransformationFromSize.get().at(size)
		);

		double t = iFrame;

		Rr<Matrix<Color>> rOut = UtilsGL.matricesColor.obtain(sizeFrame, true);
		rOut.a(m -> UtilsGL.parallel.parallelY(sizeFrame, y -> {
			for (int x = 0; x < w; x++) {
				m.set(x, y, tcf3.at(t, Vector.xy(x + 0.5, y + 0.5)));
			}
		}));
		return rOut;
	}
}
